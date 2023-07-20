package com.toasterofbread.spmp.ui.layout.mainpage

import SpMp
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
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
import com.toasterofbread.spmp.PlayerService
import com.toasterofbread.spmp.api.HomeFeedLoadResult
import com.toasterofbread.spmp.api.cast
import com.toasterofbread.spmp.api.getHomeFeed
import com.toasterofbread.spmp.api.unit
import com.toasterofbread.spmp.model.*
import com.toasterofbread.spmp.model.mediaitem.*
import com.toasterofbread.spmp.platform.*
import com.toasterofbread.spmp.platform.composable.BackHandler
import com.toasterofbread.spmp.ui.component.*
import com.toasterofbread.spmp.ui.component.longpressmenu.LongPressMenu
import com.toasterofbread.spmp.ui.component.longpressmenu.LongPressMenuData
import com.toasterofbread.spmp.ui.component.mediaitemlayout.MediaItemLayout
import com.toasterofbread.spmp.ui.component.mediaitempreview.*
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.layout.*
import com.toasterofbread.spmp.ui.layout.artistpage.ArtistPage
import com.toasterofbread.spmp.ui.layout.library.LibraryPage
import com.toasterofbread.spmp.ui.layout.nowplaying.NOW_PLAYING_VERTICAL_PAGE_COUNT
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingExpansionState
import com.toasterofbread.spmp.ui.layout.nowplaying.ThemeMode
import com.toasterofbread.spmp.ui.layout.nowplaying.overlay.OverlayMenu
import com.toasterofbread.spmp.ui.layout.playlistpage.PlaylistPage
import com.toasterofbread.spmp.ui.layout.prefspage.PrefsPage
import com.toasterofbread.spmp.ui.layout.prefspage.PrefsPageCategory
import com.toasterofbread.spmp.ui.layout.radiobuilder.RadioBuilderPage
import com.toasterofbread.utils.addUnique
import com.toasterofbread.utils.composable.OnChangedEffect
import com.toasterofbread.utils.init
import com.toasterofbread.utils.launchSingle
import com.toasterofbread.utils.toFloat
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

    fun getItem(): MediaItem?

    data class MediaItemPage(private val holder: MediaItemHolder): PlayerOverlayPage {
        override fun getItem(): MediaItem? = holder.item

        @Composable
        override fun getPage(pill_menu: PillMenu, previous_item: MediaItemHolder?, bottom_padding: Dp, close: () -> Unit) {
            when (val item = holder.item) {
                null -> close()
                is Playlist -> PlaylistPage(
                    pill_menu,
                    item,
                    previous_item?.item,
                    PaddingValues(top = SpMp.context.getStatusBarHeight(), bottom = bottom_padding),
                    close
                )
                is Artist -> ArtistPage(pill_menu, item, previous_item?.item, bottom_padding, close)
                is Song -> SongRelatedPage(
                    pill_menu,
                    item,
                    Modifier.fillMaxSize(),
                    previous_item?.item,
                    PaddingValues(
                        top = SpMp.context.getStatusBarHeight(),
                        bottom = bottom_padding,
                        start = SpMp.context.getDefaultHorizontalPadding(),
                        end = SpMp.context.getDefaultHorizontalPadding()
                    ),
                    close = close
                )
                else -> throw NotImplementedError(item.type.toString())
            }
        }
    }

    data class YtmLoginPage(private val manual: Boolean = false): PlayerOverlayPage {
        override fun getItem(): MediaItem? = null

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
        override fun getItem(): MediaItem? = null

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
            override fun getItem(): MediaItem? = null

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
            override fun getItem(): MediaItem? = null

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
            override fun getItem(): MediaItem? = null

            val current_category: MutableState<PrefsPageCategory?> = mutableStateOf(null)

            @Composable
            override fun getPage(pill_menu: PillMenu, previous_item: MediaItemHolder?, bottom_padding: Dp, close: () -> Unit) {
                PrefsPage(pill_menu, bottom_padding, current_category, Modifier.fillMaxSize(), close)
            }
        }
        val LibraryPage = object : PlayerOverlayPage {
            override fun getItem(): MediaItem? = null

            @Composable
            override fun getPage(pill_menu: PillMenu, previous_item: MediaItemHolder?, bottom_padding: Dp, close: () -> Unit) {
                LibraryPage(pill_menu, bottom_padding, Modifier.fillMaxSize(), close = close)
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
class PlayerStateImpl(private val context: PlatformContext): PlayerState(null, null, null) {
    private var _player: PlayerService? by mutableStateOf(null)
    override val session_started: Boolean get() = _player?.session_started == true

    private var now_playing_switch_page: Int by mutableStateOf(-1)
    private val overlay_page_undo_stack: MutableList<Pair<PlayerOverlayPage, MediaItem?>?> = mutableListOf()
    private var main_page_showing: Boolean by mutableStateOf(false)

    @Composable
    private fun getCurrentBottomPadding(): Float =
        with(LocalDensity.current) {
            (session_started.toFloat() * MINIMISED_NOW_PLAYING_HEIGHT_DP.dp.toPx()) + SpMp.context.getNavigationBarHeight()
        }
    private val bottom_padding_anim = Animatable(SpMp.context.getNavigationBarHeight().toFloat())

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

    private val pinned_items: MutableList<MediaItemHolder> = mutableStateListOf()

    val expansion_state = NowPlayingExpansionState(np_swipe_state, context)
    override var download_manager = PlayerDownloadManager(context)

    override var overlay_page: Pair<PlayerOverlayPage, MediaItem?>? by mutableStateOf(null)
        private set
    override val bottom_padding: Float get() = bottom_padding_anim.value
    override val pill_menu = PillMenu(
        top = false,
        left = false
    )
    override val main_multiselect_context: MediaItemMultiSelectContext = getPlayerStateMultiSelectContext()
    override var np_theme_mode: ThemeMode by mutableStateOf(Settings.getEnum(Settings.KEY_NOWPLAYING_THEME_MODE, context.getPrefs()))
    override val np_overlay_menu: MutableState<OverlayMenu?> = mutableStateOf(null)

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
        val prefs = context.getPrefs()
        context.getPrefs().addListener(prefs_listener)

        runBlocking {
            for (uid in Settings.INTERNAL_PINNED_ITEMS.get<Set<String>>(prefs)) {
                val item = MediaItem.fromUid(uid, context)
                pinned_items.add(item.getHolder())
            }
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
        if (service_connecting) {
            return
        }

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
        val screen_height = context.getScreenHeight()
        val bottom_padding = context.getNavigationBarHeightDp()
        val keyboard_insets = SpMp.context.getImeInsets()

        return base.offset {
            val keyboard_bottom_padding = if (keyboard_insets == null || np_swipe_state.value.targetValue != 0) 0 else keyboard_insets.getBottom(density)
            IntOffset(
                0,
                with (density) {
                    (-np_swipe_state.value.offset.value.dp - (screen_height * 0.5f) - bottom_padding).toPx().toInt() - keyboard_bottom_padding
                }
            )
        }
    }

    @Composable
    override fun nowPlayingBottomPadding(include_np: Boolean): Dp {
        if (include_np) {
            val np by animateDpAsState(if (session_started) MINIMISED_NOW_PLAYING_HEIGHT_DP.dp else 0.dp)
            return context.getNavigationBarHeightDp() + np
        }
        return context.getNavigationBarHeightDp()
    }

    override fun setOverlayPage(page: PlayerOverlayPage?, from_current: Boolean) {
        val current = if (from_current) overlay_page?.first?.getItem() else null
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
        if (item is Song) {
            playMediaItem(item)
        }
        else {
            openMediaItem(item)
        }
    }
    override fun onMediaItemLongClicked(item: MediaItem, long_press_data: LongPressMenuData?) {
        showLongPressMenu(when (item) {
            is Song -> long_press_data ?: getSongLongPressMenuData(item)
            is Artist -> long_press_data ?: getArtistLongPressMenuData(item)
            else -> long_press_data ?: LongPressMenuData(item)
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

    override fun openNowPlayingOverlayMenu(menu: OverlayMenu?) {
        np_overlay_menu.value = menu
        expansion_state.scrollTo(1)
    }

    override fun playMediaItem(item: MediaItem, shuffle: Boolean) {
        withPlayer {
            if (item is Song) {
                playSong(item, start_radio = true, shuffle = shuffle)
            }
            else {
                startRadioAtIndex(0, item, shuffle = shuffle)
            }

            if (np_swipe_state.value.targetValue == 0 && Settings.get(Settings.KEY_OPEN_NP_ON_SONG_PLAYED)) {
                switchNowPlayingPage(1)
            }
        }
    }

    override fun playPlaylist(playlist: Playlist, from_index: Int) {
        withPlayer {
            startRadioAtIndex(
                0,
                playlist,
                onLoad =
                    if (from_index <= 0) null
                    else { success ->
                        if (success) {
                            withContext(Dispatchers.Main) {
                                seekToSong(from_index)
                            }
                        }
                    }
            )
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
        val bottom_padding = getCurrentBottomPadding()
        OnChangedEffect(bottom_padding) {
            bottom_padding_anim.animateTo(bottom_padding)
        }

        val screen_height = context.getScreenHeight()

        LaunchedEffect(screen_height) {
            val half_screen_height = screen_height.value * 0.5f

            np_swipe_anchors = (0..NOW_PLAYING_VERTICAL_PAGE_COUNT)
                .associateBy { anchor ->
                    if (anchor == 0) MINIMISED_NOW_PLAYING_HEIGHT_DP.toFloat() - half_screen_height
                    else ((screen_height).value * anchor) - half_screen_height
                }

            val current_swipe_value = np_swipe_state.value.targetValue
            np_swipe_state.value = SwipeableState(0).apply {
                init(mapOf(-half_screen_height to 0))
                snapTo(current_swipe_value)
            }
        }

        if (np_swipe_anchors != null) {
            com.toasterofbread.spmp.ui.layout.nowplaying.NowPlaying(np_swipe_state.value, np_swipe_anchors!!)
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
    private var main_page_load_error: Throwable? by mutableStateOf(null)
    private var main_page_filter_chips: List<FilterChip>? by mutableStateOf(null)
    private var main_page_selected_filter_chip: Int? by mutableStateOf(null)

    private val feed_load_state = mutableStateOf(FeedLoadState.PREINIT)
    private val feed_load_lock = ReentrantLock()
    private var feed_continuation: String? by mutableStateOf(null)

    private suspend fun loadFeed(
        min_rows: Int,
        allow_cached: Boolean,
        continue_feed: Boolean,
        filter_chip: Int? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        main_page_selected_filter_chip = filter_chip

        feed_load_lock.lock()

        check(feed_load_state.value == FeedLoadState.PREINIT || feed_load_state.value == FeedLoadState.NONE)

        val filter_params = filter_chip?.let { main_page_filter_chips!![it].params }
        feed_load_state.value = if (continue_feed) FeedLoadState.CONTINUING else FeedLoadState.LOADING

        coroutineContext.job.invokeOnCompletion {
            feed_load_state.value = FeedLoadState.NONE
            feed_load_lock.unlock()
        }

        val result = loadFeedLayouts(min_rows, allow_cached, filter_params, if (continue_feed) feed_continuation else null)
        
        result.fold(
            { data ->
                val square_item_max_text_rows: Int = Settings.KEY_FEED_SQUARE_PREVIEW_TEXT_LINES.get()
                val itemSizeProvider: @Composable () -> DpSize = { getMainPageItemSize() }
                for (layout in data.layouts) {
                    layout.itemSizeProvider = itemSizeProvider
                    layout.square_item_max_text_rows = square_item_max_text_rows
                }

                if (continue_feed) {
                    main_page_layouts = (main_page_layouts ?: emptyList()) + data.layouts
                } else {
                    main_page_layouts = data.layouts
                    main_page_filter_chips = data.filter_chips
                }

                feed_continuation = data.ctoken
            },
            { error ->
                main_page_load_error = error
                main_page_layouts = emptyList()
                main_page_filter_chips = null
            }
        )

        feed_load_state.value = FeedLoadState.NONE

        return@withContext result.unit()
    }

    @Composable
    fun HomePage() {
        Crossfade(targetState = overlay_page) { page ->
            val feed_coroutine_scope = rememberCoroutineScope()

            Column(Modifier.fillMaxSize()) {
                if (page != null && page.first !is PlayerOverlayPage.MediaItemPage && page.first != PlayerOverlayPage.SearchPage) {
                    Spacer(Modifier.requiredHeight(context.getStatusBarHeight()))
                }

                val close = remember { { navigateBack() } }

                if (page == null) {
                    LaunchedEffect(Unit) {
                        check(!main_page_showing)
                        main_page_showing = true

                        if (main_page_layouts == null) {
                            feed_coroutine_scope.launchSingle {
                                player?.also { player ->
                                    val result = loadFeed(Settings.get(Settings.KEY_FEED_INITIAL_ROWS), allow_cached = true, continue_feed = false)
                                    player.loadPersistentQueue(result.isSuccess)
                                }
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
                        main_page_load_error,
                        remember { derivedStateOf { feed_continuation != null } }.value,
                        { main_page_filter_chips },
                        { main_page_selected_filter_chip },
                        pill_menu,
                        { filter_chip: Int?, continuation: Boolean ->
                            feed_coroutine_scope.launchSingle {
                                loadFeed(-1, false, continuation, filter_chip)
                            }
                        }
                    )
                }
                else {
                    page.first.getPage(
                        pill_menu,
                        page.second,
                        (if (session_started) MINIMISED_NOW_PLAYING_HEIGHT_DP.dp else 0.dp) + SpMp.context.getNavigationBarHeightDp(),
                        close
                    )
                }
            }
        }
    }

    // PlayerServiceHost

    override val player: PlayerService? get() = _player
    override fun withPlayer(action: PlayerService.() -> Unit) {
        _player?.also { action(it) }
    }

    val service_connected: Boolean get() = _player != null

    private var service_connecting = false
    private var service_connected_listeners = mutableListOf<(PlayerService) -> Unit>()
    private lateinit var service_connection: Any

    override lateinit var status: PlayerStatus
        private set

    override fun isRunningAndFocused(): Boolean {
        return player?.has_focus == true
    }
}

private suspend fun loadFeedLayouts(
    min_rows: Int,
    allow_cached: Boolean,
    params: String?,
    continuation: String? = null,
): Result<HomeFeedLoadResult> {
    val result = getHomeFeed(
        allow_cached = allow_cached,
        min_rows = min_rows,
        params = params,
        continuation = continuation
    )

    val data = result.getOrNull() ?: return result.cast()
    return Result.success(
        data.copy(
            layouts = data.layouts.filter { it.items.isNotEmpty() }
        )
    )
}
