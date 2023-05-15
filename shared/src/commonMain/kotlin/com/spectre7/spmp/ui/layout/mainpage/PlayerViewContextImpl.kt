package com.spectre7.spmp.ui.layout.mainpage

import SpMp
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeableState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
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
import com.spectre7.spmp.ui.component.*
import com.spectre7.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.spectre7.spmp.ui.layout.*
import com.spectre7.spmp.ui.layout.nowplaying.NOW_PLAYING_VERTICAL_PAGE_COUNT
import com.spectre7.spmp.ui.layout.nowplaying.ThemeMode
import com.spectre7.utils.addUnique
import com.spectre7.utils.composable.OnChangedEffect
import com.spectre7.utils.init
import com.spectre7.utils.toFloat
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread

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
class PlayerViewContextImpl: PlayerViewContext(null, null, null) {
    private var now_playing_switch_page: Int by mutableStateOf(-1)
    private val overlay_page_undo_stack: MutableList<Triple<OverlayPage, MediaItem?, MediaItem?>?> = mutableListOf()
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

    private var now_playing_swipe_state: SwipeableState<Int> by mutableStateOf(SwipeableState(0))
    private var now_playing_swipe_anchors: Map<Float, Int>? by mutableStateOf(null)

    private val pinned_items: MutableList<MediaItem> = mutableStateListOf()

    override var np_theme_mode: ThemeMode by mutableStateOf(Settings.getEnum(Settings.KEY_NOWPLAYING_THEME_MODE))
    override var overlay_page: Triple<OverlayPage, MediaItem?, MediaItem?>? by mutableStateOf(null)
        private set
    override val bottom_padding: Dp get() = bottom_padding_anim.value.dp
    override val pill_menu = PillMenu(
        top = false,
        left = false
    )
    override val main_multiselect_context: MediaItemMultiSelectContext = getMainMultiselectContext { this }
    
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
            pinned_items.add(Playlist.fromId(playlist))
        }
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    fun init() {
        val screen_height = SpMp.context.getScreenHeight().value
        LaunchedEffect(Unit) {
            now_playing_swipe_state.init(mapOf(screen_height * -0.5f to 0))
        }
    }

    fun release() {
        SpMp.removeLowMemoryListener(low_memory_listener)
        Settings.prefs.removeListener(prefs_listener)
    }

    override fun getNowPlayingTopOffset(screen_height: Dp, density: Density): Int {
        return with (density) { (-now_playing_swipe_state.offset.value.dp - (screen_height * 0.5f)).toPx().toInt() }
    }

    override fun setOverlayPage(page: OverlayPage?, media_item: MediaItem?, from_current: Boolean) {
        val current = if (from_current) overlay_page!!.second!! else null

        val new_page = page?.let { Triple(page, media_item, current) }
        if (new_page != overlay_page) {
            overlay_page_undo_stack.add(overlay_page)
            overlay_page = new_page
        }
    }

    override fun navigateBack() {
        overlay_page = overlay_page_undo_stack.removeLastOrNull()
    }

    override fun onMediaItemClicked(item: MediaItem) {
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

        if (now_playing_swipe_state.targetValue != 0) {
            switchNowPlayingPage(0)
        }
        hideLongPressMenu()
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

        if (now_playing_swipe_state.targetValue == 0 && Settings.get(Settings.KEY_OPEN_NP_ON_SONG_PLAYED)) {
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
        val status_bar_height = SpMp.context.getStatusBarHeight()

        LaunchedEffect(screen_height, status_bar_height) {
            val half_screen_height = screen_height.value * 0.5f

            now_playing_swipe_anchors = (0..NOW_PLAYING_VERTICAL_PAGE_COUNT)
                .associateBy { anchor ->
                    if (anchor == 0) MINIMISED_NOW_PLAYING_HEIGHT.toFloat() - half_screen_height
                    else ((screen_height + status_bar_height).value * anchor) - half_screen_height
                }

            val current_swipe_value = now_playing_swipe_state.targetValue
            now_playing_swipe_state = SwipeableState(0).apply {
                init(mapOf(-half_screen_height to 0))
                snapTo(current_swipe_value)
            }
        }

        if (now_playing_swipe_anchors != null) {
            com.spectre7.spmp.ui.layout.nowplaying.NowPlaying(remember { { this } }, now_playing_swipe_state, now_playing_swipe_anchors!!)
        }

        OnChangedEffect(now_playing_switch_page) {
            if (now_playing_switch_page >= 0) {
                now_playing_swipe_state.animateTo(now_playing_switch_page)
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

                LongPressMenu(
                    long_press_menu_showing && current,
                    (long_press_menu_direct && current) || data.thumb_shape == null,
                    {
                        if (current) {
                            hideLongPressMenu()
                        }
                    },
                    { this },
                    data,
                    Modifier
                        .onSizeChanged {
                            height = maxOf(height, it.height)
                            height_found = true
                        }
                        .height(if (height_found && height > 0) with(LocalDensity.current) { height.toDp() } else Dp.Unspecified)
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
    private val playerProvider = { this }
    private var main_page_layouts: List<MediaItemLayout> by mutableStateOf(emptyList())
    private var main_page_filter_chips: List<Pair<Int, String>>? by mutableStateOf(null)
    private var main_page_selected_filter_chip: Int? by mutableStateOf(null)

    private val feed_load_state = mutableStateOf(FeedLoadState.NONE)
    private val feed_load_lock = ReentrantLock()
    private var feed_continuation: String? by mutableStateOf(null)

    private fun loadFeed(min_rows: Int, allow_cached: Boolean, continue_feed: Boolean, filter_chip: Int? = null, onFinished: ((success: Boolean) -> Unit)? = null) {
        thread {
            if (!feed_load_lock.tryLock()) {
                return@thread
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

            onFinished?.invoke(result.isSuccess)
        }
    }

    @Composable
    fun HomeFeed() {
        Crossfade(targetState = overlay_page) { page ->
            Column(Modifier.fillMaxSize()) {
                if (page != null && page.first != OverlayPage.MEDIAITEM && page.first != OverlayPage.SEARCH) {
                    Spacer(Modifier.requiredHeight(SpMp.context.getStatusBarHeight()))
                }

                val close = remember { { navigateBack() } }
                when (page?.first) {
                    null -> {
                        DisposableEffect(Unit) {
                            check(!main_page_showing)
                            main_page_showing = true

                            if (main_page_layouts.isEmpty()) {
                                loadFeed(Settings.get(Settings.KEY_FEED_INITIAL_ROWS), allow_cached = true, continue_feed = false) {
                                    if (it) {
                                        PlayerServiceHost.player.loadPersistentQueue()
                                    }
                                }
                            }

                            onDispose {
                                check(main_page_showing)
                                main_page_showing = false
                            }
                        }

                        MainPage(
                            pinned_items,
                            { main_page_layouts },
                            playerProvider,
                            main_page_scroll_state,
                            feed_load_state,
                            remember { derivedStateOf { feed_continuation != null } }.value,
                            { main_page_filter_chips },
                            { main_page_selected_filter_chip },
                            pill_menu,
                            { filter_chip: Int?, continuation: Boolean ->
                                loadFeed(-1, false, continuation, filter_chip)
                            }
                        )
                    }
                    OverlayPage.SEARCH -> SearchPage(
                        pill_menu,
                        if (PlayerServiceHost.session_started) MINIMISED_NOW_PLAYING_HEIGHT.dp else 0.dp,
                        playerProvider,
                        close
                    )
                    OverlayPage.SETTINGS -> PrefsPage(pill_menu, playerProvider, close)
                    OverlayPage.MEDIAITEM -> Crossfade(page) { p ->
                        when (val item = p.second) {
                            null -> {}
                            is Artist -> ArtistPage(pill_menu, item, playerProvider, p.third, close)
                            is Playlist -> PlaylistPage(pill_menu, item, playerProvider, p.third, close)
                            else -> throw NotImplementedError()
                        }
                    }
                    OverlayPage.LIBRARY -> LibraryPage(pill_menu, playerProvider, close = close)
                    OverlayPage.RADIO_BUILDER -> RadioBuilderPage(
                        pill_menu,
                        if (PlayerServiceHost.session_started) MINIMISED_NOW_PLAYING_HEIGHT.dp else 0.dp,
                        playerProvider,
                        close
                    )
                    OverlayPage.YTM_LOGIN -> YoutubeMusicLogin(Modifier.fillMaxSize()) { result ->
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

private fun getMainMultiselectContext(playerProvider: () -> PlayerViewContext): MediaItemMultiSelectContext =
    MediaItemMultiSelectContext(
        playerProvider,
        selectedItemActions = { multiselect ->
            // Play after button
            Row(
                Modifier.clickable {
                    PlayerServiceHost.player.addMultipleToQueue(
                        multiselect.getUniqueSelectedItems().filterIsInstance(),
                        PlayerServiceHost.player.active_queue_index + 1,
                        is_active_queue = true
                    )
                    multiselect.onActionPerformed()
                },
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.SubdirectoryArrowRight, null)
                Text(getString(if (distance == 1) "lpm_action_play_after_1_song" else "lpm_action_play_after_x_songs").replace("\$x", distance.toString()), fontSize = 15.sp)
            }
        },
        nextRowSelectedItemActions = { multiselect ->
            // Play after controls and song indicator
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
                    Icon(Icons.Filled.Remove, null, tint = background_colour())
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
                    Icon(Icons.Filled.Add, null, tint = background_colour())
                }

                val active_queue_item = 
                    if (PlayerServiceHost.player.active_queue_index < PlayerServiceHost.status.m_queue_size) 
                        PlayerServiceHost.player.getSong(PlayerServiceHost.player.active_queue_index)
                    else null

                Crossfade(active_queue_item, animationSpec = tween(100)) {
                    it?.PreviewLong(MediaItem.PreviewParams(
                        { playerProvider().copy(onClickedOverride = { item -> playerProvider().openMediaItem(item) }) }
                    ))
                }
            }
        }
    )