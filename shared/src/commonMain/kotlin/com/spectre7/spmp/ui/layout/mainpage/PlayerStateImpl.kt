package com.spectre7.spmp.ui.layout.mainpage

import SpMp
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeableState
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import com.spectre7.spmp.PlayerService
import com.spectre7.spmp.api.cast
import com.spectre7.spmp.api.getHomeFeed
import com.spectre7.spmp.api.getOrThrowHere
import com.spectre7.spmp.model.*
import com.spectre7.spmp.model.mediaitem.*
import com.spectre7.spmp.model.mediaitem.enums.PlaylistType
import com.spectre7.spmp.platform.*
import com.spectre7.spmp.platform.composable.BackHandler
import com.spectre7.spmp.ui.component.*
import com.spectre7.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.spectre7.spmp.ui.layout.*
import com.spectre7.spmp.ui.layout.library.LibraryPage
import com.spectre7.spmp.ui.layout.nowplaying.NOW_PLAYING_VERTICAL_PAGE_COUNT
import com.spectre7.spmp.ui.layout.nowplaying.NowPlayingExpansionState
import com.spectre7.spmp.ui.layout.nowplaying.ThemeMode
import com.spectre7.spmp.ui.layout.prefspage.PrefsPage
import com.spectre7.utils.addUnique
import com.spectre7.utils.composable.OnChangedEffect
import com.spectre7.utils.init
import com.spectre7.utils.launchSingle
import com.spectre7.utils.toFloat
import kotlinx.coroutines.*
import java.util.concurrent.locks.ReentrantLock

enum class FeedLoadState { PREINIT, NONE, LOADING, CONTINUING }

@Composable
fun getMainPageItemSize(): DpSize {
    val width = if (SpMp.context.isScreenLarge()) MEDIAITEM_PREVIEW_SQUARE_SIZE_LARGE.dp else MEDIAITEM_PREVIEW_SQUARE_SIZE_SMALL.dp
    return DpSize(
        width,
        width + 30.dp
    )
}

interface PlayerOverlayPage {
    @Composable
    fun getPage(pill_menu: PillMenu, previous_item: MediaItemHolder?, bottom_padding: Dp, close: () -> Unit)

    data class MediaItemPage(private val holder: MediaItemHolder): PlayerOverlayPage {
        @Composable
        override fun getPage(pill_menu: PillMenu, previous_item: MediaItemHolder?, bottom_padding: Dp, close: () -> Unit) {
            when (val item = holder.item) {
                null -> close()
                is Playlist -> PlaylistPage(pill_menu, item, previous_item?.item, close)
                is Artist -> ArtistPage(pill_menu, item, previous_item?.item, close)
                else -> throw NotImplementedError(item.type.toString())
            }
        }
    }

    data class YtmLoginPage(private val manual: Boolean = false): PlayerOverlayPage {
        @Composable
        override fun getPage(pill_menu: PillMenu, previous_item: MediaItemHolder?, bottom_padding: Dp, close: () -> Unit) {
            YoutubeMusicLogin(
                Modifier.fillMaxSize(),
                manual = manual
            ) { result ->
                result?.fold(
                    { Settings.KEY_YTM_AUTH.set(it) },
                    { TODO(it.toString()) }
                )
                close()
            }
        }
    }

    private data class GenericFeedViewMorePage(private val browse_id: String): PlayerOverlayPage {
        @Composable
        override fun getPage(pill_menu: PillMenu, previous_item: MediaItemHolder?, bottom_padding: Dp, close: () -> Unit) {
            GenericFeedViewMorePage(browse_id, Modifier.fillMaxSize(), bottom_padding = bottom_padding)
        }
    }

    companion object {
        fun getViewMorePage(browse_id: String): PlayerOverlayPage = when (browse_id) {
            "FEmusic_listen_again", "FEmusic_mixed_for_you", "FEmusic_new_releases_albums" -> GenericFeedViewMorePage(browse_id)
            "FEmusic_moods_and_genres" -> TODO(browse_id)
            "FEmusic_charts" -> TODO(browse_id)
            else -> throw NotImplementedError(browse_id)
        }

        val RadioBuilderPage = object : PlayerOverlayPage {
            @Composable
            override fun getPage(pill_menu: PillMenu, previous_item: MediaItemHolder?, bottom_padding: Dp, close: () -> Unit) {
                RadioBuilderPage(
                    pill_menu,
                    bottom_padding,
                    Modifier.fillMaxSize(),
                    close
                )
            }
        }
        val SearchPage = object : PlayerOverlayPage {
            @Composable
            override fun getPage(pill_menu: PillMenu, previous_item: MediaItemHolder?, bottom_padding: Dp, close: () -> Unit) {
                SearchPage(
                    pill_menu,
                    bottom_padding,
                    close
                )
            }
        }
        val SettingsPage = object : PlayerOverlayPage {
            @Composable
            override fun getPage(pill_menu: PillMenu, previous_item: MediaItemHolder?, bottom_padding: Dp, close: () -> Unit) {
                PrefsPage(pill_menu, bottom_padding, Modifier.fillMaxSize(), close)
            }
        }
        val LibraryPage = object : PlayerOverlayPage {
            @Composable
            override fun getPage(pill_menu: PillMenu, previous_item: MediaItemHolder?, bottom_padding: Dp, close: () -> Unit) {
                LibraryPage(pill_menu, bottom_padding, Modifier.fillMaxSize(), close = close)
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
class PlayerStateImpl: PlayerState(null, null, null) {
    private var _player: PlayerService? by mutableStateOf(null)
    override val session_started: Boolean get() = _player?.session_started == true

    private var now_playing_switch_page: Int by mutableStateOf(-1)
    private val overlay_page_undo_stack: MutableList<Pair<PlayerOverlayPage, MediaItem?>?> = mutableListOf()
    private val bottom_padding_anim = Animatable(session_started.toFloat() * MINIMISED_NOW_PLAYING_HEIGHT)
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

    private val pinned_items: MutableList<MediaItemHolder> = mutableStateListOf()

    override var np_theme_mode: ThemeMode by mutableStateOf(Settings.getEnum(Settings.KEY_NOWPLAYING_THEME_MODE))
    override var overlay_page: Pair<PlayerOverlayPage, MediaItem?>? by mutableStateOf(null)
        private set
    override val bottom_padding: Dp get() = bottom_padding_anim.value.dp
    override val pill_menu = PillMenu(
        top = false,
        left = false
    )
    override val main_multiselect_context: MediaItemMultiSelectContext = getPlayerStateMultiSelectContext()

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

        runBlocking {
            for (uid in Settings.INTERNAL_PINNED_ITEMS.get<Set<String>>()) {
                val item = MediaItem.fromUid(uid)
                pinned_items.add(item.getHolder())
            }
        }
    }

    private var initialised: Boolean = false
    lateinit var context: PlatformContext

    override lateinit var download_manager: PlayerDownloadManager

    fun init(context: PlatformContext) {
        if (!initialised) {
            this.context = context
            download_manager = PlayerDownloadManager(context)
            initialised = true
        }
    }

    @Composable
    fun initComposable(context: PlatformContext) {
        val screen_height = context.getScreenHeight().value
        LaunchedEffect(Unit) {
            np_swipe_state.value.init(mapOf(screen_height * -0.5f to 0))
        }
    }

    fun onStart() {
        check(!service_connecting)

        service_connecting = true
        service_connection = MediaPlayerService.connect(
            context,
            PlayerService::class.java,
            _player
        ) { service ->
            synchronized(service_connected_listeners) {
                _player = service
                status = PlayerStatus(_player!!)
                service_connecting = false

                service_connected_listeners.forEach { it(service) }
                service_connected_listeners.clear()
            }
        }
    }

    fun onStop() {
        MediaPlayerService.disconnect(context, service_connection)
        download_manager.release()
        SpMp.removeLowMemoryListener(low_memory_listener)
        Settings.prefs.removeListener(prefs_listener)
        _player = null
    }

    override fun interactService(action: (player: PlayerService) -> Unit) {
        synchronized(service_connected_listeners) {
            _player?.also {
                action(it)
                return
            }

            service_connected_listeners.add(action)
        }
    }

    @Composable
    override fun nowPlayingTopOffset(base: Modifier): Modifier {
        val density = LocalDensity.current
        val screen_height = SpMp.context.getScreenHeight()
        val bottom_padding = SpMp.context.getNavigationBarHeight()

        return base.offset {
            IntOffset(
                0,
                with (density) {
                    (-np_swipe_state.value.offset.value.dp - (screen_height * 0.5f) - bottom_padding).toPx().toInt()
                }
            )
        }
    }

    @Composable
    override fun nowPlayingBottomPadding(): Dp = SpMp.context.getNavigationBarHeight()

    override fun setOverlayPage(page: PlayerOverlayPage?, from_current: Boolean) {
        val current = if (from_current) overlay_page?.second else null

        val new_page = page?.let { Pair(page, current) }
        if (new_page != overlay_page) {
            overlay_page_undo_stack.add(overlay_page)
            overlay_page = new_page
        }
    }

    override fun navigateBack() {
        overlay_page = overlay_page_undo_stack.removeLastOrNull()
    }

    override fun onMediaItemClicked(item: MediaItem, multiselect_key: Int?) {
        if (item is Song || (item is Playlist && item.playlist_type == PlaylistType.RADIO)) {
            playMediaItem(item)
        }
        else {
            openMediaItem(item)
        }
    }
    override fun onMediaItemLongClicked(item: MediaItem, queue_index: Int?) {
        showLongPressMenu(when (item) {
            is Song -> getSongLongPressMenuData(item, multiselect_key = queue_index)
            is Artist -> getArtistLongPressMenuData(item)
            else -> LongPressMenuData(item)
        })
    }

    override fun openPage(page: PlayerOverlayPage, from_current: Boolean) {
        setOverlayPage(page, from_current)
        if (np_swipe_state.value.targetValue != 0) {
            switchNowPlayingPage(0)
        }
        hideLongPressMenu()
    }

    override fun openMediaItem(item: MediaItem, from_current: Boolean) {
        if (item is Artist && item.is_for_item) {
            return
        }
        openPage(PlayerOverlayPage.MediaItemPage(item.getHolder()), from_current)
    }

    override fun openViewMorePage(browse_id: String) {
        openPage(PlayerOverlayPage.getViewMorePage(browse_id))
    }

    override fun playMediaItem(item: MediaItem, shuffle: Boolean) {
        if (item is Song) {
            player.playSong(item, start_radio = true, shuffle = shuffle)
        }
        else {
            player.startRadioAtIndex(0, item, shuffle = shuffle)
        }

        if (np_swipe_state.value.targetValue == 0 && Settings.get(Settings.KEY_OPEN_NP_ON_SONG_PLAYED)) {
            switchNowPlayingPage(1)
        }
    }

    override fun onMediaItemPinnedChanged(item: MediaItem, pinned: Boolean) {
        if (pinned) {
            pinned_items.addUnique(item.getHolder())
        }
        else {
            pinned_items.removeAll { it.item == item }
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
        OnChangedEffect(session_started) {
            bottom_padding_anim.animateTo(session_started.toFloat() * MINIMISED_NOW_PLAYING_HEIGHT)
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
                    data
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
    private var main_page_layouts: List<MediaItemLayout>? by mutableStateOf(null)
    private var main_page_filter_chips: List<Pair<Int, String>>? by mutableStateOf(null)
    private var main_page_selected_filter_chip: Int? by mutableStateOf(null)

    private val feed_load_state = mutableStateOf(FeedLoadState.PREINIT)
    private val feed_load_lock = ReentrantLock()
    private var feed_continuation: String? by mutableStateOf(null)

    private suspend fun loadFeed(
        min_rows: Int,
        allow_cached: Boolean,
        continue_feed: Boolean,
        filter_chip: Int? = null,
        report_error: Boolean = false
    ): Result<Unit> = withContext(Dispatchers.IO) {
        main_page_selected_filter_chip = filter_chip

        feed_load_lock.lock()

        check(feed_load_state.value == FeedLoadState.PREINIT || feed_load_state.value == FeedLoadState.NONE)

        val filter_params = filter_chip?.let { main_page_filter_chips!![it].second }
        feed_load_state.value = if (continue_feed) FeedLoadState.CONTINUING else FeedLoadState.LOADING

        coroutineContext.job.invokeOnCompletion {
            feed_load_state.value = FeedLoadState.NONE
            feed_load_lock.unlock()
        }

        val result = loadFeedLayouts(min_rows, allow_cached, filter_params, if (continue_feed) feed_continuation else null)
        if (result.isFailure) {
            if (report_error) {
                SpMp.reportActionError(result.exceptionOrNull())
            }
            main_page_layouts = emptyList()
            main_page_filter_chips = null
        } else {
            val (layouts, cont, chips) = result.getOrThrow()
            for (layout in layouts) {
                layout.itemSizeProvider = { getMainPageItemSize() }
            }

            if (continue_feed) {
                main_page_layouts = (main_page_layouts ?: emptyList()) + layouts
            } else {
                main_page_layouts = layouts
                main_page_filter_chips = chips
            }

            feed_continuation = cont
        }

        feed_load_state.value = FeedLoadState.NONE

        return@withContext result.fold(
            { Result.success(Unit) },
            { Result.failure(it) }
        )
    }

    @Composable
    fun HomePage() {
        Crossfade(targetState = overlay_page) { page ->
            val feed_coroutine_scope = rememberCoroutineScope()

            Column(Modifier.fillMaxSize()) {
                if (page != null && page.first !is PlayerOverlayPage.MediaItemPage && page.first != PlayerOverlayPage.SearchPage) {
                    Spacer(Modifier.requiredHeight(SpMp.context.getStatusBarHeight()))
                }

                val close = remember { { navigateBack() } }

                if (page == null) {
                    LaunchedEffect(Unit) {
                        check(!main_page_showing)
                        main_page_showing = true

                        if (main_page_layouts == null) {
                            feed_coroutine_scope.launchSingle {
                                val result = loadFeed(Settings.get(Settings.KEY_FEED_INITIAL_ROWS), allow_cached = true, continue_feed = false)
                                player.loadPersistentQueue(result.isSuccess)
                            }
                        }
                    }

                    DisposableEffect(Unit) {
                        onDispose {
                            check(main_page_showing)
                            main_page_showing = false
                        }
                    }

                    pinned_items.removeAll { it.item == null }

                    MainPage(
                        pinned_items,
                        { main_page_layouts ?: emptyList() },
                        main_page_scroll_state,
                        feed_load_state,
                        remember { derivedStateOf { feed_continuation != null } }.value,
                        { main_page_filter_chips },
                        { main_page_selected_filter_chip },
                        pill_menu,
                        { filter_chip: Int?, continuation: Boolean ->
                            feed_coroutine_scope.launchSingle {
                                loadFeed(-1, false, continuation, filter_chip, report_error = true)
                            }
                        }
                    )
                }
                else {
                    page.first.getPage(
                        pill_menu,
                        page.second,
                        if (session_started) MINIMISED_NOW_PLAYING_HEIGHT.dp else 0.dp,
                        close
                    )
                }
            }
        }
    }

    // PlayerServiceHost

    override val player: PlayerService? get() = _player

    val service_connected: Boolean get() = _player != null

    private var service_connecting = false
    private var service_connected_listeners = mutableListOf<(PlayerService) -> Unit>()
    private lateinit var service_connection: Any

    override lateinit var status: PlayerStatus
        private set

    override fun isRunningAndFocused(): Boolean {
        if (!player.has_focus) {
            return false
        }

        return true
    }
}

private suspend fun loadFeedLayouts(min_rows: Int, allow_cached: Boolean, params: String?, continuation: String? = null): Result<Triple<List<MediaItemLayout>, String?, List<Pair<Int, String>>?>> {
    val result = getHomeFeed(allow_cached = allow_cached, min_rows = min_rows, params = params, continuation = continuation)

    if (!result.isSuccess) {
        return result.cast()
    }

    val (row_data, new_continuation, chips) = result.getOrThrowHere()
    return Result.success(Triple(row_data.filter { it.items.isNotEmpty() }, new_continuation, chips))
}
