@file:OptIn(ExperimentalMaterialApi::class)

package com.spectre7.spmp.ui.layout

import SpMp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeableState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.*
import com.spectre7.spmp.PlayerServiceHost
import com.spectre7.spmp.api.*
import com.spectre7.spmp.model.*
import com.spectre7.spmp.platform.*
import com.spectre7.spmp.ui.component.*
import com.spectre7.spmp.ui.layout.nowplaying.NOW_PLAYING_VERTICAL_PAGE_COUNT
import com.spectre7.spmp.ui.layout.nowplaying.NowPlaying
import com.spectre7.spmp.ui.layout.nowplaying.ThemeMode
import com.spectre7.spmp.ui.theme.Theme
import com.spectre7.utils.*
import kotlinx.coroutines.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread

const val MINIMISED_NOW_PLAYING_HEIGHT: Int = 64
const val MEDIAITEM_PREVIEW_SQUARE_SIZE_SMALL: Float = 100f
const val MEDIAITEM_PREVIEW_SQUARE_SIZE_LARGE: Float = 200f

enum class OverlayPage { SEARCH, SETTINGS, MEDIAITEM, LIBRARY, RADIO_BUILDER, YTM_LOGIN }

private enum class FeedLoadState { NONE, LOADING, CONTINUING }

open class PlayerViewContext(
    private val onClickedOverride: ((item: MediaItem) -> Unit)? = null,
    private val onLongClickedOverride: ((item: MediaItem) -> Unit)? = null,
    private val upstream: PlayerViewContext? = null
) {
    open val np_theme_mode: ThemeMode get() = upstream!!.np_theme_mode
    open val overlay_page: Triple<OverlayPage, MediaItem?, MediaItemLayout?>? get() = upstream!!.overlay_page
    open val bottom_padding: Dp get() = upstream!!.bottom_padding
    open val pill_menu: PillMenu get() = upstream!!.pill_menu

    fun copy(onClickedOverride: ((item: MediaItem) -> Unit)? = null, onLongClickedOverride: ((item: MediaItem) -> Unit)? = null): PlayerViewContext {
        return PlayerViewContext(onClickedOverride, onLongClickedOverride, this)
    }

    open fun getNowPlayingTopOffset(screen_height: Dp, density: Density): Int = upstream!!.getNowPlayingTopOffset(screen_height, density)

    open fun setOverlayPage(page: OverlayPage?, media_item: MediaItem? = null, opened_layout: MediaItemLayout? = null) { upstream!!.setOverlayPage(page, media_item, opened_layout) }

    open fun navigateBack() { upstream!!.navigateBack() }

    open fun onMediaItemClicked(item: MediaItem) {
        if (onClickedOverride != null) {
            onClickedOverride.invoke(item)
        }
        else {
            upstream!!.onMediaItemClicked(item)
        }
    }
    open fun onMediaItemLongClicked(item: MediaItem, queue_index: Int? = null) {
        if (onLongClickedOverride != null) {
            onLongClickedOverride.invoke(item)
        }
        else {
            upstream!!.onMediaItemLongClicked(item, queue_index)
        }
    }

    open fun openMediaItem(item: MediaItem, opened_layout: MediaItemLayout? = null) { upstream!!.openMediaItem(item, opened_layout) }
    open fun playMediaItem(item: MediaItem, shuffle: Boolean = false) { upstream!!.playMediaItem(item, shuffle) }

    open fun onMediaItemPinnedChanged(item: MediaItem, pinned: Boolean) { upstream!!.onMediaItemPinnedChanged(item, pinned) }

    open fun showLongPressMenu(data: LongPressMenuData) { upstream!!.showLongPressMenu(data) }

    open fun hideLongPressMenu() { upstream!!.hideLongPressMenu() }
}

private class PlayerViewContextImpl: PlayerViewContext(null, null, null) {
    private var now_playing_switch_page: Int by mutableStateOf(-1)
    private val overlay_page_undo_stack: MutableList<Triple<OverlayPage, MediaItem?, MediaItemLayout?>?> = mutableListOf()
    private val bottom_padding_anim = androidx.compose.animation.core.Animatable(PlayerServiceHost.session_started.toFloat() * MINIMISED_NOW_PLAYING_HEIGHT)

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
    override var overlay_page: Triple<OverlayPage, MediaItem?, MediaItemLayout?>? by mutableStateOf(null)
        private set
    override val bottom_padding: Dp get() = bottom_padding_anim.value.dp
    override val pill_menu = PillMenu(
        top = false,
        left = false
    )

    init {
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
        Settings.prefs.removeListener(prefs_listener)
    }

    override fun getNowPlayingTopOffset(screen_height: Dp, density: Density): Int {
        return with (density) { (-now_playing_swipe_state.offset.value.dp - (screen_height * 0.5f)).toPx().toInt() }
    }

    override fun setOverlayPage(page: OverlayPage?, media_item: MediaItem?, opened_layout: MediaItemLayout?) {
        val new_page = page?.let { Triple(page, media_item, opened_layout) }
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

    override fun openMediaItem(item: MediaItem, opened_layout: MediaItemLayout?) {
        if (item is Artist && item.for_song) {
            return
        }

        setOverlayPage(OverlayPage.MEDIAITEM, item, opened_layout)

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
            PlayerServiceHost.player.clearQueue()
            PlayerServiceHost.player.startRadioAtIndex(0, item)

            item.editRegistry {
                it.play_count++
            }
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
            NowPlaying(remember { { this } }, now_playing_swipe_state, now_playing_swipe_anchors!!)
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
//                        .height(if (height_found && height > 0) with(LocalDensity.current) { height.toDp() } else Dp.Unspecified)
                )
            }
        }
    }

    private val main_page_scroll_state = LazyListState()
    private val playerProvider = { this }
    private var main_page_layouts: List<MediaItemLayout> by mutableStateOf(emptyList())
    private var main_page_filter_chips: List<String>? by mutableStateOf(null)
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
            val params = filter_chip?.let { main_page_filter_chips!![it] }

            feed_load_state.value = if (continue_feed) FeedLoadState.CONTINUING else FeedLoadState.LOADING

            val result = loadFeedLayouts(min_rows, allow_cached, params, if (continue_feed) feed_continuation else null)
            if (result.isFailure) {
                SpMp.error_manager.onError("loadFeed", result.exceptionOrNull()!!)
            }
            else {
                val (layouts, cont, chips) = result.getOrThrow()
                for (layout in layouts) {
                    layout.itemSizeProvider = { getMainPageItemSize() }
                }

                if (continue_feed) {
                    main_page_layouts = main_page_layouts + layouts
                }
                else {
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
        LaunchedEffect(Unit) {
            loadFeed(Settings.get(Settings.KEY_FEED_INITIAL_ROWS), allow_cached = true, continue_feed = false) {
                if (it) {
                    PlayerServiceHost.player.loadPersistentQueue()
                }
            }
        }

        Crossfade(targetState = overlay_page) { page ->
            Column(Modifier.fillMaxSize()) {
                if (page != null && page.first != OverlayPage.MEDIAITEM && page.first != OverlayPage.SEARCH) {
                    Spacer(Modifier.requiredHeight(SpMp.context.getStatusBarHeight()))
                }

                val close = remember { { navigateBack() } }
                when (page?.first) {
                    null -> MainPage(
                        pinned_items,
                        main_page_layouts,
                        playerProvider,
                        main_page_scroll_state,
                        feed_load_state,
                        remember { derivedStateOf { feed_continuation != null } }.value,
                        { main_page_filter_chips },
                        { main_page_selected_filter_chip },
                        { filter_chip: Int?, continuation: Boolean ->
                            loadFeed(-1, false, continuation, filter_chip)
                        }
                    )
                    OverlayPage.SEARCH -> SearchPage(pill_menu, if (PlayerServiceHost.session_started) MINIMISED_NOW_PLAYING_HEIGHT.dp else 0.dp, playerProvider, close)
                    OverlayPage.SETTINGS -> PrefsPage(pill_menu, playerProvider, close)
                    OverlayPage.MEDIAITEM -> Crossfade(page) { p ->
                        when (val item = p.second) {
                            null -> {}
                            is Artist, is Playlist -> ArtistPlaylistPage(pill_menu, item, playerProvider, p.third, close)
                            else -> throw NotImplementedError()
                        }
                    }
                    OverlayPage.LIBRARY -> LibraryPage(pill_menu, playerProvider, close)
                    OverlayPage.RADIO_BUILDER -> RadioBuilderPage(pill_menu, if (PlayerServiceHost.session_started) MINIMISED_NOW_PLAYING_HEIGHT.dp else 0.dp, playerProvider, close)
                    OverlayPage.YTM_LOGIN -> YoutubeMusicLogin(Modifier.fillMaxSize()) { result ->
                        result.fold(
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

@Composable
fun PlayerView() {
    val player = remember { PlayerViewContextImpl() }
    player.init()
    player.LongPressMenu()

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

    val screen_height = SpMp.context.getScreenHeight()

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

            player.HomeFeed()
        }

        player.NowPlaying()
    }
}

@Composable
private fun getMainPageItemSize(): DpSize {
    val width = if (SpMp.context.isScreenLarge()) MEDIAITEM_PREVIEW_SQUARE_SIZE_LARGE.dp else MEDIAITEM_PREVIEW_SQUARE_SIZE_SMALL.dp
    return DpSize(
        width,
        width + 30.dp
    )
}

@Composable
private fun MainPage(
    pinned_items: MutableList<MediaItem>,
    layouts: List<MediaItemLayout>,
    playerProvider: () -> PlayerViewContext,
    scroll_state: LazyListState,
    feed_load_state: MutableState<FeedLoadState>,
    can_continue_feed: Boolean,
    getFilterChips: () -> List<String>?,
    getSelectedFilterChip: () -> Int?,
    loadFeed: (filter_chip: Int?, continuation: Boolean) -> Unit
) {
    val padding by animateDpAsState(if (SpMp.context.isScreenLarge()) 30.dp else 10.dp)

    val artists_layout: MediaItemLayout = remember {
        MediaItemLayout(
            LocalisedYoutubeString.raw(MediaItem.Type.ARTIST.getReadable(true)),
            null,
            items = mutableStateListOf(),
            type = MediaItemLayout.Type.ROW,
            itemSizeProvider = { DpSize(120.dp, 150.dp) }
        )
    }
    var auth_info: YoutubeMusicAuthInfo by remember { mutableStateOf(YoutubeMusicAuthInfo(Settings.KEY_YTM_AUTH.get())) }

    LaunchedEffect(layouts) {
        val artists_map: MutableMap<Artist, Int> = mutableMapOf()
        for (layout in layouts) {
            for (item in layout.items) {
                if (item is Artist) {
                    continue
                }

                item.artist?.also { artist ->
                    if (artist == auth_info.own_channel) {
                        return@also
                    }

                    val current = artists_map[artist]
                    if (current == null) {
                        artists_map[artist] = 1
                    }
                    else {
                        artists_map[artist] = current + 1
                    }
                }
            }
        }

        val artists = artists_map.mapNotNull { artist ->
            if (artist.value < 1) null
            else Pair(artist.key, artist.value)    
        }.sortedByDescending { it.second }

        artists_layout.items.clear()
        for (artist in artists) {
            artists_layout.items.add(artist.first)
        }
    }

    DisposableEffect(Unit) {
        val prefs_listener = object : ProjectPreferences.Listener {
            override fun onChanged(prefs: ProjectPreferences, key: String) {
                if (key == Settings.KEY_YTM_AUTH.name) {
                    auth_info = YoutubeMusicAuthInfo(Settings.KEY_YTM_AUTH.get(prefs))
                }
            }
        }

        Settings.prefs.addListener(prefs_listener)

        onDispose {
            Settings.prefs.removeListener(prefs_listener)
        }
    }

    Column(Modifier.padding(horizontal = padding)) {
        MainPageTopBar(auth_info, playerProvider, Modifier.padding(top = SpMp.context.getStatusBarHeight()))

        // Main scrolling view
        SwipeRefresh(
            state = feed_load_state.value == FeedLoadState.LOADING,
            onRefresh = { loadFeed(getSelectedFilterChip(), false) },
            swipe_enabled = feed_load_state.value == FeedLoadState.NONE,
            indicator = false
        ) {
//            val state by remember { derivedStateOf { if (feed_load_state.value == FeedLoadState.LOADING) null else layouts.isNotEmpty() } }
            val state = if (feed_load_state.value == FeedLoadState.LOADING) null else layouts.isNotEmpty()
            var current_state by remember { mutableStateOf(state) }
            val state_alpha = remember { Animatable(1f) }

            LaunchedEffect(state) {
                state_alpha.animateTo(0f, tween(300))
                current_state = state
                state_alpha.animateTo(1f, tween(300))
            }

            @Composable
            fun TopContent() {
                MainPageScrollableTopContent(playerProvider, pinned_items, getFilterChips, getSelectedFilterChip, Modifier.padding(bottom = 15.dp)) {
                    loadFeed(it, false)
                }
            }

            when (current_state) {
                // Loaded
                true -> {
                    LazyMediaItemLayoutColumn(
                        layouts,
                        playerProvider,
                        layout_modifier = Modifier.graphicsLayer { alpha = state_alpha.value },
                        padding = PaddingValues(
                            bottom = playerProvider().bottom_padding
                        ),
                        onContinuationRequested = if (can_continue_feed) {
                            { loadFeed(getSelectedFilterChip(), true) }
                        } else null,
                        continuation_alignment = Alignment.Start,
                        loading_continuation = feed_load_state.value != FeedLoadState.NONE,
                        scroll_state = scroll_state,
                        scroll_enabled = !state_alpha.isRunning,
                        spacing = 30.dp,
                        topContent = {
                            item {
                                TopContent()
                            }
                        },
                        layoutItem = { layout, i, showLayout ->
                            showLayout(this, layout)
                            
                            if (i == 0) {
                                artists_layout.also { showLayout(this, it) }
                            }
                        }
                    ) { it.type ?: MediaItemLayout.Type.GRID }
                }
                // Offline
                false -> {
                    // TODO
                    Text("Offline", Modifier.padding(top = SpMp.context.getStatusBarHeight() + padding))
                }
                // Loading
                null -> {
                    Column(Modifier.fillMaxSize()) {
                        TopContent()
                        MainPageLoadingView(Modifier.graphicsLayer { alpha = state_alpha.value }.fillMaxSize())
                    }
                }
            }
        }
    }
}

@Composable
private fun MainPageTopBar(auth_info: YoutubeMusicAuthInfo, playerProvider: () -> PlayerViewContext, modifier: Modifier = Modifier) {
    Row(modifier) {

        var lyrics: Song.Lyrics? by remember { mutableStateOf(null) }
        var lyrics_loaded: Boolean by remember { mutableStateOf(false) }

        LaunchedEffect(PlayerServiceHost.status.m_song) {
            val song = PlayerServiceHost.status.m_song

            if (song?.lyrics_loaded == true) {
                lyrics = song!!.lyrics
                lyrics_loaded = true
            }
            else {
                lyrics = null
                lyrics_loaded = false

                if (song != null) {
                    launch {
                        lyrics = song.loadLyrics()
                        lyrics_loaded = true
                    }
                }
            }
        }

        Crossfade(Pair(lyrics_loaded, lyrics)) { state ->
            val (loading, lyr) = state
    
            if (!state.first) {
                SubtleLoadingIndicator()
            }
            else if (state.second != null && state.second!!.sync_type != Song.Lyrics.SyncType.NONE) {
                LyricsLineDisplay(state.second!!, { PlayerServiceHost.status.position_ms }, Theme.current.on_background_provider, Modifier.fillMaxWidth())
            }
            else {
                PlayerServiceHost.player.Waveform(Color.White)
            }
        }

        // TODO | Lyrics
        Spacer(Modifier.fillMaxWidth().weight(1f))

        IconButton({
            if (auth_info.initialised) {
                playerProvider().onMediaItemClicked(auth_info.own_channel)
            }
            else {
                playerProvider().setOverlayPage(OverlayPage.YTM_LOGIN)
            }
        }) {
            Crossfade(auth_info) { info ->
                if (auth_info.initialised) {
                    info.own_channel.Thumbnail(MediaItem.ThumbnailQuality.LOW, Modifier.clip(CircleShape).size(27.dp))
                }
                else {
                    Icon(Icons.Filled.Person, null)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainPageScrollableTopContent(
    playerProvider: () -> PlayerViewContext,
    pinned_items: MutableList<MediaItem>,
    getFilterChips: () -> List<String>?,
    getSelectedFilterChip: () -> Int?,
    modifier: Modifier = Modifier,
    onFilterChipSelected: (Int?) -> Unit
) {
    val pinned_layout = remember(pinned_items) {
        MediaItemLayout(
            null, null,
            MediaItemLayout.Type.ROW,
            pinned_items,
            itemSizeProvider = { getMainPageItemSize() * 0.8f }
        )
    }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        AnimatedVisibility(
            pinned_layout.items.isNotEmpty(),
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            MediaItemGrid(
                pinned_layout,
                playerProvider,
                rows = 1,
                startContent = {
                    item {
                        Column(
                            Modifier.fillMaxHeight(),
                            verticalArrangement = Arrangement.SpaceEvenly,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(Modifier.size(30.dp), contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.PushPin, null, Modifier.alpha(0.5f))
                            }
                            IconButton({
                                for (i in 0 until pinned_items.size) {
                                    pinned_items.first().setPinnedToHome(false, playerProvider)
                                }
                            }, Modifier.size(30.dp)) {
                                Icon(Icons.Filled.CleaningServices, null)
                            }
                        }
                    }
                }
            )
        }

        LazyRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val filter_chips = getFilterChips()
            val selected_filter_chip = getSelectedFilterChip()

            items(filter_chips?.size ?: 0) { i ->
                val chip = filter_chips!![i]
                ElevatedFilterChip(
                    i == selected_filter_chip,
                    {
                        onFilterChipSelected(if (i == selected_filter_chip) null else i)
                    },
                    { Text(getFilterChipName(chip)) }
                )
            }
        }
    }

}

private val FILTER_CHIP_INDICES = mapOf(
    11 to "Relax",
    19 to "Workout",
    27 to "Energise",
    35 to "Commute",
    43 to "Focus"
)

private fun getFilterChipName(params: String): String {
    for (index in FILTER_CHIP_INDICES) {
        if (params[index.key] == 'D') {
            return getStringTemp(index.value)
        }
    }
    throw NotImplementedError(params)
}

@Composable
private fun MainPageLoadingView(modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(getString("loading_feed"), Modifier.alpha(0.4f), fontSize = 12.sp, color = Theme.current.on_background)
        Spacer(Modifier.height(5.dp))
        LinearProgressIndicator(
            Modifier
                .alpha(0.4f)
                .fillMaxWidth(0.35f), color = Theme.current.on_background)
    }
}

//@Composable
//private fun RadioBuilderCard(playerProvider: () -> PlayerViewContext, modifier: Modifier = Modifier) {
//    Card(
//        modifier,
//        colors = CardDefaults.cardColors(
//            containerColor = Theme.current.vibrant_accent,
//            contentColor = Theme.current.vibrant_accent.getContrasted()
//        )
//    ) {
//        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
//            WidthShrinkText(
//                getString("radio_builder_title"),
//                modifier = Modifier.padding(horizontal = 15.dp),
//                fontSize = 25.sp,
//                colour = Theme.current.vibrant_accent.getContrasted()
//            )
//
//            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
//                val button_colours = ButtonDefaults.buttonColors(
//                    containerColor = Theme.current.vibrant_accent.getContrasted(),
//                    contentColor = Theme.current.vibrant_accent
//                )
//
//                ShapedIconButton(
//                    { playerProvider().setOverlayPage(OverlayPage.RADIO_BUILDER) },
//                    shape = CircleShape,
//                    colors = button_colours.toIconButtonColours()
//                ) {
//                    Icon(Icons.Filled.Add, null)
//                }
//
//                val button_padding = PaddingValues(15.dp, 5.dp)
//                Button({ TODO() }, contentPadding = button_padding, colors = button_colours) {
//                    WidthShrinkText(getString("radio_builder_play_last_button"))
//                }
//                Button({ TODO() }, contentPadding = button_padding, colors = button_colours) {
//                    WidthShrinkText(getString("radio_builder_recent_button"))
//                }
//            }
//        }
//    }
//}

private fun loadFeedLayouts(min_rows: Int, allow_cached: Boolean, params: String?, continuation: String? = null): Result<Triple<List<MediaItemLayout>, String?, List<String>?>> {
    val result = getHomeFeed(allow_cached = allow_cached, min_rows = min_rows, params = params, continuation = continuation)

    if (!result.isSuccess) {
        return result.cast()
    }

    val (row_data, new_continuation, chips) = result.getOrThrowHere()
    return Result.success(Triple(row_data.filter { it.items.isNotEmpty() }, new_continuation, chips))
}
