@file:OptIn(ExperimentalMaterialApi::class)

package com.spectre7.spmp.ui.layout

import com.spectre7.spmp.platform.ProjectPreferences.OnSharedPreferenceChangeListener
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.PlayerServiceHost
import com.spectre7.spmp.R
import com.spectre7.spmp.api.cast
import com.spectre7.spmp.api.getHomeFeed
import com.spectre7.spmp.api.getOrThrowHere
import com.spectre7.spmp.model.*
import com.spectre7.spmp.ui.component.*
import com.spectre7.spmp.ui.layout.nowplaying.NOW_PLAYING_VERTICAL_PAGE_COUNT
import com.spectre7.spmp.ui.layout.nowplaying.NowPlaying
import com.spectre7.spmp.ui.layout.nowplaying.ThemeMode
import com.spectre7.spmp.ui.theme.Theme
import com.spectre7.utils.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread

const val MINIMISED_NOW_PLAYING_HEIGHT: Int = 64
enum class OverlayPage { SEARCH, SETTINGS, MEDIAITEM, LIBRARY, RADIO_BUILDER }

private enum class FeedLoadState { NONE, LOADING, CONTINUING }

@OptIn(ExperimentalMaterialApi::class)
data class PlayerViewContext(
    private val onClickedOverride: ((item: MediaItem) -> Unit)? = null,
    private val onLongClickedOverride: ((item: MediaItem) -> Unit)? = null,
    private val base: PlayerViewContext? = null
) {
    fun copy(onClickedOverride: ((item: MediaItem) -> Unit)? = null, onLongClickedOverride: ((item: MediaItem) -> Unit)? = null): PlayerViewContext {
        return PlayerViewContext(onClickedOverride, onLongClickedOverride, this)
    }

    private val is_base: Boolean get() = base == null
    private fun baseOrThis(): PlayerViewContext = base ?: this

    private val np_theme_mode_state: MutableState<ThemeMode>? = if (is_base) mutableStateOf(Settings.getEnum(Settings.KEY_NOWPLAYING_THEME_MODE)) else null

    var np_theme_mode: ThemeMode
        get() = baseOrThis().np_theme_mode_state!!.value
        private set(value) { baseOrThis().np_theme_mode_state!!.value = value }

    private val now_playing_swipe_state: SwipeableState<Int>? = if (is_base) SwipeableState(0) else null
    private var now_playing_swipe_anchors: Map<Float, Int>? = null
    fun getNowPlayingSwipeState(): SwipeableState<Int> = baseOrThis().now_playing_swipe_state!!

    fun getNowPlayingTopOffset(screen_height: Dp, density: Density): Int {
        return with (density) { (-getNowPlayingSwipeState().offset.value.dp - screen_height * 0.5f).toPx().toInt() }
    }

    private val now_playing_switch_page: MutableState<Int>? = if (is_base) mutableStateOf(-1) else null
    private fun switchNowPlayingPage(page: Int) {
        baseOrThis().now_playing_switch_page!!.value = page
    }

    val pill_menu = PillMenu(
        top = false,
        left = false
    )

    private val overlay_page_undo_stack: MutableList<Triple<OverlayPage, MediaItem?, MediaItemLayout?>?> = mutableListOf()
    var overlay_page: Triple<OverlayPage, MediaItem?, MediaItemLayout?>? by mutableStateOf(null)
        private set

    fun setOverlayPage(page: OverlayPage?, media_item: MediaItem? = null, opened_layout: MediaItemLayout? = null) {
        val new_page = page?.let { Triple(page, media_item, opened_layout) }
        if (new_page != overlay_page) {
            overlay_page_undo_stack.add(overlay_page)
            overlay_page = new_page
        }
    }

    fun navigateBack() {
        overlay_page = overlay_page_undo_stack.removeLastOrNull()
    }

    private val bottom_padding_anim = androidx.compose.animation.core.Animatable(PlayerServiceHost.session_started.toFloat() * MINIMISED_NOW_PLAYING_HEIGHT)
    val bottom_padding: Dp get() = bottom_padding_anim.value.dp

    private lateinit var prefs_listener: OnSharedPreferenceChangeListener

    init {
        if (is_base) {
            prefs_listener = OnSharedPreferenceChangeListener { prefs, key ->
                when (key) {
                    Settings.KEY_NOWPLAYING_THEME_MODE.name -> {
                        np_theme_mode = Settings.getEnum(Settings.KEY_NOWPLAYING_THEME_MODE, prefs)
                    }
                }
            }
            Settings.prefs.registerOnSharedPreferenceChangeListener(prefs_listener)
        }
    }

    fun release() {
        if (is_base) {
            Settings.prefs.unregisterOnSharedPreferenceChangeListener(prefs_listener)
        }
    }

    fun onMediaItemClicked(item: MediaItem) {
        if (onClickedOverride != null) {
            onClickedOverride.invoke(item)
            return
        }

        if (base != null) {
            base.onMediaItemClicked(item)
            return
        }

        if (item is Song || (item is Playlist && item.playlist_type == Playlist.PlaylistType.RADIO)) {
            playMediaItem(item)
        }
        else {
            openMediaItem(item)
        }
    }
    fun onMediaItemLongClicked(item: MediaItem, queue_index: Int? = null) {
        if (onLongClickedOverride != null) {
            onLongClickedOverride.invoke(item)
            return
        }

        if (base != null) {
            base.onMediaItemLongClicked(item)
            return
        }

        showLongPressMenu(when (item) {
            is Song -> getSongLongPressMenuData(item, queue_index = queue_index)
            is Artist -> getArtistLongPressMenuData(item)
            else -> LongPressMenuData(item)
        })
    }

    fun openMediaItem(item: MediaItem, opened_layout: MediaItemLayout? = null) {
        if (item is Artist && item.for_song) {
            return
        }

        if (base != null) {
            base.openMediaItem(item, opened_layout)
            return
        }

        setOverlayPage(OverlayPage.MEDIAITEM, item, opened_layout)

        if (getNowPlayingSwipeState().targetValue != 0) {
            switchNowPlayingPage(0)
        }
        hideLongPressMenu()
    }

    fun playMediaItem(item: MediaItem, shuffle: Boolean = false) {
        if (shuffle) {
            TODO()
        }

        if (item is Song) {
            PlayerServiceHost.service.playSong(item)
        }
        else {
            PlayerServiceHost.service.clearQueue()
            PlayerServiceHost.service.startRadioAtIndex(0, item)

            item.editRegistry {
                it.play_count++
            }
        }

        if (getNowPlayingSwipeState().targetValue == 0 && Settings.get(Settings.KEY_OPEN_NP_ON_SONG_PLAYED)) {
            switchNowPlayingPage(1)
        }

    }

    private var long_press_menu_data: LongPressMenuData? by mutableStateOf(null)
    private var long_press_menu_showing: Boolean by mutableStateOf(false)
    private var long_press_menu_direct: Boolean by mutableStateOf(false)

    fun showLongPressMenu(data: LongPressMenuData) {
        if (base != null) {
            base.showLongPressMenu(data)
            return
        }

        long_press_menu_data = data

        if (long_press_menu_showing) {
            long_press_menu_direct = true
        }
        else {
            long_press_menu_showing = true
            long_press_menu_direct = false
        }
    }

    fun hideLongPressMenu() {
        if (base != null) {
            base.hideLongPressMenu()
            return
        }
        long_press_menu_showing = false
        long_press_menu_direct = false
    }

    @Composable
    fun NowPlaying() {
        check(is_base)

        OnChangedEffect(PlayerServiceHost.session_started) {
            bottom_padding_anim.animateTo(PlayerServiceHost.session_started.toFloat() * MINIMISED_NOW_PLAYING_HEIGHT)
        }

        if (now_playing_swipe_anchors == null) {
            val screen_height = getScreenHeight()
            val half_screen_height = screen_height.value * 0.5f
            now_playing_swipe_anchors = (0..NOW_PLAYING_VERTICAL_PAGE_COUNT).associateBy { if (it == 0) MINIMISED_NOW_PLAYING_HEIGHT.toFloat() - half_screen_height else (screen_height.value * it) - half_screen_height }

            now_playing_swipe_state!!.init(mapOf(-half_screen_height to 0))
        }

        NowPlaying(remember { { this } }, now_playing_swipe_state!!, now_playing_swipe_anchors!!)
        
        OnChangedEffect(now_playing_switch_page!!.value) {
            if (now_playing_switch_page.value >= 0) {
                now_playing_swipe_state.animateTo(now_playing_switch_page.value)
                now_playing_switch_page.value = -1
            }
        }

        BackHandler(overlay_page_undo_stack.isNotEmpty()) {
            navigateBack()
        }
    }

    @Composable
    internal fun LongPressMenu() {
        var height by remember { mutableStateOf(0) }

        Crossfade(long_press_menu_data) { data ->
            if (data != null) {
                val current = data == long_press_menu_data
                var height_found by remember { mutableStateOf(false) }

                LongPressIconMenu(
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
}

@Composable
fun PlayerView() {
    val player = remember { PlayerViewContext() }
    val playerProvider = remember { { player } }
    player.LongPressMenu()

    val feed_load_state = remember { mutableStateOf(FeedLoadState.NONE) }
    val feed_load_lock = remember { ReentrantLock() }
    var feed_continuation: String? by remember { mutableStateOf(null) }

    val main_page_layouts = remember { mutableStateListOf<MediaItemLayout>() }
    val screen_height = getScreenHeight()

    fun loadFeed(min_rows: Int, allow_cached: Boolean, continue_feed: Boolean, onFinished: ((success: Boolean) -> Unit)? = null) {
        thread {
            if (!feed_load_lock.tryLock()) {
                return@thread
            }
            check(feed_load_state.value == FeedLoadState.NONE)

            feed_load_state.value = if (continue_feed) FeedLoadState.CONTINUING else FeedLoadState.LOADING

            val result = loadFeedLayouts(min_rows, allow_cached, if (continue_feed) feed_continuation else null)
            if (result.isFailure) {
                MainActivity.error_manager.onError("loadFeed", result.exceptionOrNull()!!)
            }
            else {
                if (!continue_feed) {
                    main_page_layouts.clear()
                }

                val (layouts, cont) = result.getOrThrow()
                main_page_layouts.addAll(layouts)
                feed_continuation = cont
            }

            feed_load_state.value = FeedLoadState.NONE
            feed_load_lock.unlock()

            onFinished?.invoke(result.isSuccess)
        }
    }

    LaunchedEffect(Unit) {
        loadFeed(Settings.get(Settings.KEY_FEED_INITIAL_ROWS), allow_cached = true, continue_feed = false) {
            if (it) {
                PlayerServiceHost.service.loadPersistentQueue()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            player.release()
        }
    }

    LaunchedEffect(player.overlay_page) {
        if (player.overlay_page == null) {
            player.pill_menu.showing = true
            player.pill_menu.top = false
            player.pill_menu.left = false
            player.pill_menu.clearExtraActions()
            player.pill_menu.clearAlongsideActions()
            player.pill_menu.clearActionOverriders()
            player.pill_menu.setBackgroundColourOverride(null)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Theme.current.background)
    ) {
        Box {
            val expand_state = remember { mutableStateOf(false) }
            val overlay_open by remember { derivedStateOf { player.overlay_page != null } }

            player.pill_menu.PillMenu(
                if (overlay_open) 1 else 3,
                { index, action_count ->
                    ActionButton(
                        if (action_count == 1) Icons.Filled.Close else
                            when (index) {
                                0 -> Icons.Filled.Search
                                1 -> Icons.Filled.LibraryMusic
                                else -> Icons.Filled.Settings
                            }
                    ) {
                        player.setOverlayPage(if (action_count == 1) null else
                            when (index) {
                                0 -> OverlayPage.SEARCH
                                1 -> OverlayPage.LIBRARY
                                else -> OverlayPage.SETTINGS
                            }
                        )
                        expand_state.value = false
                    }
                },
                if (!overlay_open) expand_state else null,
                Theme.current.accent_provider,
                container_modifier = Modifier.offset {
                    IntOffset(0, player.getNowPlayingTopOffset(screen_height, this))
                }
            )

            val main_page_scroll_state = rememberLazyListState()

            Crossfade(targetState = player.overlay_page) { page ->
                Column(Modifier.fillMaxSize()) {
                    if (page != null && page.first != OverlayPage.MEDIAITEM && page.first != OverlayPage.SEARCH) {
                        Spacer(Modifier.requiredHeight(getStatusBarHeight(MainActivity.context)))
                    }

                    val close = remember { { player.navigateBack() } }
                    when (page?.first) {
                        null -> MainPage(
                            main_page_layouts,
                            playerProvider,
                            main_page_scroll_state,
                            feed_load_state,
                            remember { derivedStateOf { feed_continuation != null } }.value,
                            { loadFeed(-1, false, it) }
                        )
                        OverlayPage.SEARCH -> SearchPage(player.pill_menu, if (PlayerServiceHost.session_started) MINIMISED_NOW_PLAYING_HEIGHT.dp else 0.dp, playerProvider, close)
                        OverlayPage.SETTINGS -> PrefsPage(player.pill_menu, playerProvider, close)
                        OverlayPage.MEDIAITEM -> Crossfade(page) { p ->
                            when (val item = p.second) {
                                null -> {}
                                is Artist, is Playlist -> ArtistPlaylistPage(player.pill_menu, item, playerProvider, p.third, close)
                                else -> throw NotImplementedError()
                            }
                        }
                        OverlayPage.LIBRARY -> LibraryPage(player.pill_menu, playerProvider, close)
                        OverlayPage.RADIO_BUILDER -> RadioBuilderPage(player.pill_menu, if (PlayerServiceHost.session_started) MINIMISED_NOW_PLAYING_HEIGHT.dp else 0.dp, playerProvider, close)
                    }
                }
            }
        }

        player.NowPlaying()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainPage(
    layouts: MutableList<MediaItemLayout>,
    playerProvider: () -> PlayerViewContext,
    scroll_state: LazyListState,
    feed_load_state: MutableState<FeedLoadState>,
    can_continue_feed: Boolean,
    loadFeed: (continuation: Boolean) -> Unit
) {
    SwipeRefresh(
        state = rememberSwipeRefreshState(feed_load_state.value == FeedLoadState.LOADING),
        onRefresh = { loadFeed(false) },
        swipeEnabled = feed_load_state.value == FeedLoadState.NONE,
        modifier = Modifier.padding(horizontal = 10.dp)
    ) {
        Crossfade(remember { derivedStateOf { layouts.isNotEmpty() } }.value) { loaded ->
            if (loaded) {
                LazyMediaItemLayoutColumn(
                    layouts,
                    playerProvider,
                    padding = PaddingValues(
                        top = getStatusBarHeight(MainActivity.context),
                        bottom = playerProvider().bottom_padding
                    ),
                    onContinuationRequested = if (can_continue_feed) {
                        { loadFeed(true) }
                    } else null,
                    continuation_alignment = Alignment.Start,
                    loading_continuation = feed_load_state.value != FeedLoadState.NONE,
                    scroll_state = scroll_state,
                    vertical_arrangement = Arrangement.spacedBy(15.dp),
                    topContent = {
                        item {
                            Card(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(5.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Theme.current.vibrant_accent,
                                    contentColor = Theme.current.vibrant_accent.getContrasted()
                                )
                            ) {
                                Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
                                    WidthShrinkText(getString("radio_builder_title), fontSize = 25.sp, colour = Theme.current.vibrant_accent.getContrasted())

                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                        val button_colours = ButtonDefaults.buttonColors(
                                            containerColor = Theme.current.vibrant_accent.getContrasted(),
                                            contentColor = Theme.current.vibrant_accent
                                        )

                                        ShapedIconButton(
                                            { playerProvider().setOverlayPage(OverlayPage.RADIO_BUILDER) },
                                            shape = CircleShape,
                                            colors = button_colours.toIconButtonColours()
                                        ) {
                                            Icon(Icons.Filled.Add, null)
                                        }

                                        val button_padding = PaddingValues(15.dp, 5.dp)
                                        Button({ TODO() }, contentPadding = button_padding, colors = button_colours) {
                                            WidthShrinkText(getString("radio_builder_play_last_button))
                                        }
                                        Button({ TODO() }, contentPadding = button_padding, colors = button_colours) {
                                            WidthShrinkText(getString("radio_builder_recent_button))
                                        }
                                    }
                                }
                            }
                        }
                    }
                ) { MediaItemLayout.Type.GRID }
            }
            else {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(getString("loading_feed), Modifier.alpha(0.4f), fontSize = 12.sp, color = Theme.current.on_background)
                    Spacer(Modifier.height(5.dp))
                    LinearProgressIndicator(
                        Modifier
                            .alpha(0.4f)
                            .fillMaxWidth(0.35f), color = Theme.current.on_background)
                }
            }
        }
    }
}

private fun loadFeedLayouts(min_rows: Int, allow_cached: Boolean, continuation: String? = null): Result<Pair<List<MediaItemLayout>, String?>> {
    val result = getHomeFeed(allow_cached = allow_cached, min_rows = min_rows, continuation = continuation)

    if (!result.isSuccess) {
        return result.cast()
    }

    val (row_data, new_continuation) = result.getOrThrowHere()

    val rows = row_data.toMutableList()
    val request_limit = Semaphore(10) // TODO?

//    runBlocking { withContext(Dispatchers.IO) { coroutineScope {
//        for (row in row_data) {
//            val entry = MediaItemLayout(row.title, row.subtitle, MediaItemLayout.Type.GRID, thumbnail_provider = row.thumbnail_provider, viewMore = row.viewMore)
//            rows.add(entry)
//
//            for (item in row.items) {
//                if (item.title != null) {
//                    entry.addItem(item)
//                    continue
//                }
//
//                launch {
//                    request_limit.withPermit {
//                        item.loadData().onSuccess { loaded ->
//                            synchronized(request_limit) {
//                                entry.addItem(loaded)
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }}}

    rows.removeIf { it.items.isEmpty() }
    return Result.success(Pair(rows, new_continuation))
}
