package com.spectre7.spmp.ui.layout.mainpage

import LocalPlayerState
import SpMp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeableState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import com.spectre7.spmp.PlayerServiceHost
import com.spectre7.spmp.api.cast
import com.spectre7.spmp.api.getHomeFeed
import com.spectre7.spmp.api.getOrThrowHere
import com.spectre7.spmp.model.*
import com.spectre7.spmp.platform.ProjectPreferences
import com.spectre7.spmp.platform.composable.BackHandler
import com.spectre7.spmp.platform.isScreenLarge
import com.spectre7.spmp.platform.vibrateShort
import com.spectre7.spmp.resources.getString
import com.spectre7.spmp.ui.component.*
import com.spectre7.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.spectre7.spmp.ui.layout.*
import com.spectre7.spmp.ui.layout.nowplaying.NOW_PLAYING_VERTICAL_PAGE_COUNT
import com.spectre7.spmp.ui.layout.nowplaying.NowPlayingExpansionState
import com.spectre7.spmp.ui.layout.nowplaying.ThemeMode
import com.spectre7.spmp.ui.layout.prefspage.PrefsPage
import com.spectre7.utils.addUnique
import com.spectre7.utils.composable.OnChangedEffect
import com.spectre7.utils.init
import com.spectre7.utils.toFloat
import kotlinx.coroutines.*
import java.util.concurrent.locks.ReentrantLock

enum class FeedLoadState { NONE, LOADING, CONTINUING }

@Composable
fun getMainPageItemSize(): DpSize {
    val width = if (SpMp.context.isScreenLarge()) MEDIAITEM_PREVIEW_SQUARE_SIZE_LARGE.dp else MEDIAITEM_PREVIEW_SQUARE_SIZE_SMALL.dp
    return DpSize(
        width,
        width + 30.dp
    )
}

@OptIn(ExperimentalMaterialApi::class)
class PlayerStateImpl: PlayerState(null, null, null) {
    private var now_playing_switch_page: Int by mutableStateOf(-1)
    private val overlay_page_undo_stack: MutableList<Triple<OverlayPage, Any?, MediaItem?>?> = mutableListOf()
    private val bottom_padding_anim = Animatable(PlayerServiceHost.session_started.toFloat() * MINIMISED_NOW_PLAYING_HEIGHT)
    private var main_page_showing: Boolean by mutableStateOf(false)

    private val low_memory_listener: () -> Unit
    private val prefs_listener: ProjectPreferences.Listener

    private fun switchNowPlayingPage(page: Int) {
        now_playing_switch_page = page
    }

    private var long_press_menu_data: LongPressMenuData? by mutableStateOf(null)
    private var long_press_menu_showing: Boolean by mutableStateOf(false)
    private var long_press_menu_direct: Boolean by mutableStateOf(false)

    private val np_swipe_state: MutableState<SwipeableState<Int>> = mutableStateOf(SwipeableState(0))
    private var np_swipe_anchors: Map<Float, Int>? by mutableStateOf(null)

    val expansion_state = NowPlayingExpansionState(np_swipe_state)

    private val pinned_items: MutableList<MediaItem> = mutableStateListOf()

    override var np_theme_mode: ThemeMode by mutableStateOf(Settings.getEnum(Settings.KEY_NOWPLAYING_THEME_MODE))
    override var overlay_page: Triple<OverlayPage, Any?, MediaItem?>? by mutableStateOf(null)
        private set
    override val bottom_padding: Dp get() = bottom_padding_anim.value.dp
    override val pill_menu = PillMenu(
        top = false,
        left = false
    )
    override val main_multiselect_context: MediaItemMultiSelectContext = getMainMultiselectContext()

    init {
        low_memory_listener = {
            if (!main_page_showing) {
                resetHomeFeed()
            }
        }
        SpMp.addLowMemoryListener(low_memory_listener)

        prefs_listener = object : ProjectPreferences.Listener {
            override fun onChanged(prefs: ProjectPreferences, key: String) {
                when (key) {
                    Settings.KEY_NOWPLAYING_THEME_MODE.name -> {
                        np_theme_mode = Settings.getEnum(Settings.KEY_NOWPLAYING_THEME_MODE, prefs)
                    }
                }
            }
        }
        Settings.prefs.addListener(prefs_listener)

        for (song in Settings.INTERNAL_PINNED_SONGS.get<Set<String>>()) {
            pinned_items.add(Song.fromId(song))
        }
        for (artist in Settings.INTERNAL_PINNED_ARTISTS.get<Set<String>>()) {
            pinned_items.add(Artist.fromId(artist))
        }
        for (playlist in Settings.INTERNAL_PINNED_PLAYLISTS.get<Set<String>>()) {
            pinned_items.add(AccountPlaylist.fromId(playlist))
        }
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    fun init() {
        val screen_height = SpMp.context.getScreenHeight().value
        LaunchedEffect(Unit) {
            np_swipe_state.value.init(mapOf(screen_height * -0.5f to 0))
        }
    }

    fun release() {
        SpMp.removeLowMemoryListener(low_memory_listener)
        Settings.prefs.removeListener(prefs_listener)
    }

    @Composable
    override fun nowPlayingTopOffset(base: Modifier): Modifier {
        val density = LocalDensity.current
        val screen_height = SpMp.context.getScreenHeight()
        val keyboard_insets = SpMp.context.getImeInsets()

        return base.offset {
            IntOffset(
                0,
                with (density) { (-np_swipe_state.value.offset.value.dp - (screen_height * 0.5f)).toPx().toInt() } -  (keyboard_insets?.getBottom(density) ?: 0)
            )
        }
    }

    override fun setOverlayPage(page: OverlayPage?, data: Any?, from_current: Boolean) {
        val current = if (from_current) (overlay_page!!.second as MediaItem) else null

        val new_page = page?.let { Triple(page, data, current) }
        if (new_page != overlay_page) {
            overlay_page_undo_stack.add(overlay_page)
            overlay_page = new_page
        }
    }

    override fun navigateBack() {
        overlay_page = overlay_page_undo_stack.removeLastOrNull()
    }

    override fun onMediaItemClicked(item: MediaItem, multiselect_key: Int?) {
        if (item is Song || (item is Playlist && item.playlist_type == Playlist.PlaylistType.RADIO)) {
            playMediaItem(item)
        }
        else {
            openMediaItem(item)
        }
    }
    override fun onMediaItemLongClicked(item: MediaItem, queue_index: Int?) {
        showLongPressMenu(when (item) {
            is Song -> getSongLongPressMenuData(item, queue_index = queue_index)
            is Artist -> getArtistLongPressMenuData(item)
            else -> LongPressMenuData(item)
        })
    }

    override fun openMediaItem(item: MediaItem, from_current: Boolean) {
        if (item is Artist && item.is_for_item) {
            return
        }

        setOverlayPage(OverlayPage.MEDIAITEM, item, from_current)

        if (np_swipe_state.value.targetValue != 0) {
            switchNowPlayingPage(0)
        }
        hideLongPressMenu()
    }

    override fun openViewMoreURL(url: String) { 
        
    }

    override fun playMediaItem(item: MediaItem, shuffle: Boolean) {
        if (shuffle) {
            TODO()
        }

        if (item is Song) {
            PlayerServiceHost.player.playSong(item)
        }
        else {
            PlayerServiceHost.player.startRadioAtIndex(0, item)
        }

        if (np_swipe_state.value.targetValue == 0 && Settings.get(Settings.KEY_OPEN_NP_ON_SONG_PLAYED)) {
            switchNowPlayingPage(1)
        }
    }

    override fun onMediaItemPinnedChanged(item: MediaItem, pinned: Boolean) {
        if (pinned) {
            pinned_items.addUnique(item)
        }
        else {
            pinned_items.remove(item)
        }
    }

    override fun showLongPressMenu(data: LongPressMenuData) {
        long_press_menu_data = data

        if (long_press_menu_showing) {
            long_press_menu_direct = true
        }
        else {
            long_press_menu_showing = true
            long_press_menu_direct = false
        }
    }

    override fun hideLongPressMenu() {
        long_press_menu_showing = false
        long_press_menu_direct = false
    }

    @Composable
    fun NowPlaying() {
        OnChangedEffect(PlayerServiceHost.session_started) {
            bottom_padding_anim.animateTo(PlayerServiceHost.session_started.toFloat() * MINIMISED_NOW_PLAYING_HEIGHT)
        }

        val screen_height = SpMp.context.getScreenHeight()

        LaunchedEffect(screen_height) {
            val half_screen_height = screen_height.value * 0.5f

            np_swipe_anchors = (0..NOW_PLAYING_VERTICAL_PAGE_COUNT)
                .associateBy { anchor ->
                    if (anchor == 0) MINIMISED_NOW_PLAYING_HEIGHT.toFloat() - half_screen_height
                    else ((screen_height).value * anchor) - half_screen_height
                }

            val current_swipe_value = np_swipe_state.value.targetValue
            np_swipe_state.value = SwipeableState(0).apply {
                init(mapOf(-half_screen_height to 0))
                snapTo(current_swipe_value)
            }
        }

        if (np_swipe_anchors != null) {
            com.spectre7.spmp.ui.layout.nowplaying.NowPlaying(np_swipe_state.value, np_swipe_anchors!!)
        }

        OnChangedEffect(now_playing_switch_page) {
            if (now_playing_switch_page >= 0) {
                np_swipe_state.value.animateTo(now_playing_switch_page)
                now_playing_switch_page = -1
            }
        }

        BackHandler(overlay_page_undo_stack.isNotEmpty()) {
            navigateBack()
        }
    }

    @Composable
    fun LongPressMenu() {
        var height by remember { mutableStateOf(0) }

        Crossfade(long_press_menu_data) { data ->
            if (data != null) {
                val current = data == long_press_menu_data
                var height_found by remember { mutableStateOf(false) }

                LaunchedEffect(current) {
                    if (!current) {
                        height_found = false
                    }
                }

                LongPressMenu(
                    long_press_menu_showing && current,
                    {
                        if (current) {
                            hideLongPressMenu()
                        }
                    },
                    data,
//                    Modifier
//                        .onSizeChanged {
//                            height = maxOf(height, it.height)
//                            height_found = true
//                            println("SIZECHANGED $height")
//                        }
//                        .height(if (height_found && height > 0) with(LocalDensity.current) { height.toDp() } else Dp.Unspecified)
                )
            }
        }
    }

    private fun resetHomeFeed() {
        main_page_layouts = emptyList()
        main_page_filter_chips = null
        main_page_selected_filter_chip = null
    }

    private val main_page_scroll_state = LazyListState()
    private var main_page_layouts: List<MediaItemLayout> by mutableStateOf(emptyList())
    private var main_page_filter_chips: List<Pair<Int, String>>? by mutableStateOf(null)
    private var main_page_selected_filter_chip: Int? by mutableStateOf(null)

    private val feed_load_state = mutableStateOf(FeedLoadState.NONE)
    private val feed_load_lock = ReentrantLock()
    private var feed_continuation: String? by mutableStateOf(null)

    private suspend fun loadFeed(
        min_rows: Int,
        allow_cached: Boolean,
        continue_feed: Boolean,
        filter_chip: Int? = null
    ): Result<Unit>? = withContext(Dispatchers.IO) {
        if (!feed_load_lock.tryLock()) {
            return@withContext null
        }
        check(feed_load_state.value == FeedLoadState.NONE)

        main_page_selected_filter_chip = filter_chip
        val params = filter_chip?.let { main_page_filter_chips!![it].second }

        feed_load_state.value = if (continue_feed) FeedLoadState.CONTINUING else FeedLoadState.LOADING

        val result = loadFeedLayouts(min_rows, allow_cached, params, if (continue_feed) feed_continuation else null)
        if (result.isFailure) {
            SpMp.error_manager.onError("loadFeed", result.exceptionOrNull()!!)
            main_page_layouts = emptyList()
            main_page_filter_chips = null
        } else {
            val (layouts, cont, chips) = result.getOrThrow()
            for (layout in layouts) {
                layout.itemSizeProvider = { getMainPageItemSize() }
            }

            if (continue_feed) {
                main_page_layouts = main_page_layouts + layouts
            } else {
                main_page_layouts = layouts
                main_page_filter_chips = chips
            }

            feed_continuation = cont
        }

        feed_load_state.value = FeedLoadState.NONE
        feed_load_lock.unlock()

        return@withContext result.fold(
            { Result.success(Unit) },
            { Result.failure(it) }
        )
    }

    @Composable
    fun HomeFeed() {
        Crossfade(targetState = overlay_page) { page ->
            val feed_coroutine_scope = rememberCoroutineScope()

            Column(Modifier.fillMaxSize()) {
                if (page != null && page.first != OverlayPage.MEDIAITEM && page.first != OverlayPage.SEARCH) {
                    Spacer(Modifier.requiredHeight(SpMp.context.getStatusBarHeight()))
                }

                val close = remember { { navigateBack() } }
                when (page?.first) {
                    null -> {

                        LaunchedEffect(Unit) {
                            check(!main_page_showing)
                            main_page_showing = true

                            if (main_page_layouts.isEmpty()) {
                                feed_coroutine_scope.coroutineContext.cancelChildren()
                                feed_coroutine_scope.launch {
                                    val result = loadFeed(Settings.get(Settings.KEY_FEED_INITIAL_ROWS), allow_cached = true, continue_feed = false)
                                    PlayerServiceHost.player.loadPersistentQueue(result?.isSuccess == true)
                                }
                            }
                        }

                        DisposableEffect(Unit) {
                            onDispose {
                                check(main_page_showing)
                                main_page_showing = false
                            }
                        }

                        MainPage(
                            pinned_items,
                            { main_page_layouts },
                            main_page_scroll_state,
                            feed_load_state,
                            remember { derivedStateOf { feed_continuation != null } }.value,
                            { main_page_filter_chips },
                            { main_page_selected_filter_chip },
                            pill_menu,
                            { filter_chip: Int?, continuation: Boolean ->
                                feed_coroutine_scope.coroutineContext.cancelChildren()
                                feed_coroutine_scope.launch {
                                    loadFeed(-1, false, continuation, filter_chip)
                                }
                            }
                        )
                    }
                    OverlayPage.SEARCH -> SearchPage(
                        pill_menu,
                        if (PlayerServiceHost.session_started) MINIMISED_NOW_PLAYING_HEIGHT.dp else 0.dp,
                        close
                    )
                    OverlayPage.SETTINGS -> PrefsPage(pill_menu, close)
                    OverlayPage.MEDIAITEM -> Crossfade(page) { p ->
                        when (val item = (p.second as MediaItem?)) {
                            null -> {}
                            is Artist -> ArtistPage(pill_menu, item, p.third, close)
                            is Playlist -> PlaylistPage(pill_menu, item, p.third, close)
                            else -> throw NotImplementedError()
                        }
                    }
                    OverlayPage.VIEW_MORE_URL -> TODO(page.second as String)
                    OverlayPage.LIBRARY -> LibraryPage(pill_menu, close = close)
                    OverlayPage.RADIO_BUILDER -> RadioBuilderPage(
                        pill_menu,
                        if (PlayerServiceHost.session_started) MINIMISED_NOW_PLAYING_HEIGHT.dp else 0.dp,
                        close
                    )
                    OverlayPage.YTM_LOGIN, OverlayPage.YTM_MANUAL_LOGIN -> YoutubeMusicLogin(
                        Modifier.fillMaxSize(),
                        manual = page.first == OverlayPage.YTM_MANUAL_LOGIN
                    ) { result ->
                        result?.fold(
                            { Settings.KEY_YTM_AUTH.set(it) },
                            { TODO(it.toString()) }
                        )
                        close()
                    }
                }
            }
        }
    }
}

private fun loadFeedLayouts(min_rows: Int, allow_cached: Boolean, params: String?, continuation: String? = null): Result<Triple<List<MediaItemLayout>, String?, List<Pair<Int, String>>?>> {
    val result = getHomeFeed(allow_cached = allow_cached, min_rows = min_rows, params = params, continuation = continuation)

    if (!result.isSuccess) {
        return result.cast()
    }

    val (row_data, new_continuation, chips) = result.getOrThrowHere()
    return Result.success(Triple(row_data.filter { it.items.isNotEmpty() }, new_continuation, chips))
}

private fun getMainMultiselectContext(): MediaItemMultiSelectContext =
    MediaItemMultiSelectContext(
        selectedItemActions = { multiselect ->
            // Play after button
            Row(
                Modifier.clickable {
                    PlayerServiceHost.player.addMultipleToQueue(
                        multiselect.getUniqueSelectedItems().filterIsInstance<Song>(),
                        (PlayerServiceHost.player.active_queue_index + 1).coerceAtMost(PlayerServiceHost.status.queue_size),
                        is_active_queue = true
                    )
                    multiselect.onActionPerformed()
                },
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.SubdirectoryArrowRight, null)
                val distance = PlayerServiceHost.player.active_queue_index - PlayerServiceHost.status.index + 1
                Text(getString(if (distance == 1) "lpm_action_play_after_1_song" else "lpm_action_play_after_x_songs").replace("\$x", distance.toString()), fontSize = 15.sp)
            }
        },
        nextRowSelectedItemActions = { multiselect ->
            // Play after controls and song indicator
            AnimatedVisibility(PlayerServiceHost.status.m_queue_size > 0) {
                MultiSelectNextRowActions()
            }
        }
    )

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MultiSelectNextRowActions() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        val active_queue_item =
            if (PlayerServiceHost.player.active_queue_index < PlayerServiceHost.status.m_queue_size)
                PlayerServiceHost.player.getSong(PlayerServiceHost.player.active_queue_index)
            else null

        val player = LocalPlayerState.current
        CompositionLocalProvider(LocalPlayerState provides remember {
            player.copy(onClickedOverride = { item, _ -> player.openMediaItem(item) })
        }) {
            Crossfade(active_queue_item, animationSpec = tween(100), modifier = Modifier.weight(1f)) {
                it?.PreviewLong(MediaItem.PreviewParams())
            }
        }

        val button_modifier = Modifier
            .size(30.dp)
            .fillMaxHeight()
            .aspectRatio(1f)
            .align(Alignment.CenterVertically)

        Surface(
            button_modifier.combinedClickable(
                remember { MutableInteractionSource() },
                rememberRipple(),
                onClick = {
                    PlayerServiceHost.player.updateActiveQueueIndex(-1)
                },
                onLongClick = {
                    SpMp.context.vibrateShort()
                    PlayerServiceHost.player.active_queue_index = PlayerServiceHost.player.current_song_index
                }
            ),
            shape = CircleShape
        ) {
            Icon(Icons.Default.Remove, null)
        }

        Surface(
            button_modifier.combinedClickable(
                remember { MutableInteractionSource() },
                rememberRipple(),
                onClick = {
                    PlayerServiceHost.player.updateActiveQueueIndex(1)
                },
                onLongClick = {
                    SpMp.context.vibrateShort()
                    PlayerServiceHost.player.active_queue_index = PlayerServiceHost.player.song_count - 1
                }
            ),
            shape = CircleShape
        ) {
            Icon(Icons.Filled.Add, null)
        }
    }
}