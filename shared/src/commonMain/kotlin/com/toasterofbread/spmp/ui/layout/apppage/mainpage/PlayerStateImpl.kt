package com.toasterofbread.spmp.ui.layout.apppage.mainpage

import LocalPlayerState
import SpMp
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeableState
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import com.toasterofbread.composekit.platform.PlatformPreferences
import com.toasterofbread.composekit.platform.PlatformPreferencesListener
import com.toasterofbread.composekit.platform.composable.BackHandler
import com.toasterofbread.composekit.utils.common.init
import com.toasterofbread.composekit.utils.composable.getEnd
import com.toasterofbread.composekit.utils.composable.getStart
import com.toasterofbread.spmp.model.*
import com.toasterofbread.spmp.model.mediaitem.*
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.layout.BrowseParamsData
import com.toasterofbread.spmp.model.mediaitem.playlist.Playlist
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.settings.Settings
import com.toasterofbread.spmp.model.settings.category.BehaviourSettings
import com.toasterofbread.spmp.model.settings.category.ThemeSettings
import com.toasterofbread.spmp.platform.*
import com.toasterofbread.spmp.platform.download.DownloadMethodSelectionDialog
import com.toasterofbread.spmp.platform.playerservice.PlatformPlayerService
import com.toasterofbread.spmp.platform.playerservice.PlayerServicePlayer
import com.toasterofbread.spmp.service.playercontroller.PersistentQueueHandler
import com.toasterofbread.spmp.ui.component.*
import com.toasterofbread.spmp.ui.component.longpressmenu.LongPressMenu
import com.toasterofbread.spmp.ui.component.longpressmenu.LongPressMenuData
import com.toasterofbread.spmp.ui.component.mediaitempreview.*
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.layout.*
import com.toasterofbread.spmp.ui.layout.apppage.AppPage
import com.toasterofbread.spmp.ui.layout.apppage.AppPageState
import com.toasterofbread.spmp.ui.layout.apppage.MediaItemAppPage
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingExpansionState
import com.toasterofbread.spmp.ui.layout.nowplaying.ThemeMode
import com.toasterofbread.spmp.ui.layout.nowplaying.getNPBackground
import com.toasterofbread.spmp.ui.layout.nowplaying.getNowPlayingVerticalPageCount
import com.toasterofbread.spmp.ui.layout.nowplaying.maintab.NowPlayingMainTabPage
import com.toasterofbread.spmp.ui.layout.nowplaying.overlay.PlayerOverlayMenu
import kotlinx.coroutines.*

enum class FeedLoadState { PREINIT, NONE, LOADING, CONTINUING }

@Composable
fun PlayerState.getMainPageItemSize(): DpSize {
    val width = if (form_factor.is_large) MEDIAITEM_PREVIEW_SQUARE_SIZE_LARGE.dp else MEDIAITEM_PREVIEW_SQUARE_SIZE_SMALL.dp
    return DpSize(
        width,
        width + 30.dp
    )
}

@OptIn(ExperimentalMaterialApi::class)
class PlayerStateImpl(override val context: AppContext, private val coroutine_scope: CoroutineScope): PlayerState(null, null, null) {
    private var _player: PlatformPlayerService? by mutableStateOf(null)

    private val app_page_undo_stack: MutableList<AppPage?> = mutableStateListOf()

    private val low_memory_listener: () -> Unit
    private val prefs_listener: PlatformPreferencesListener

    override fun switchNowPlayingPage(page: Int) {
        coroutine_scope.launch {
            np_swipe_state.value.animateTo(
                page,
                when (form_factor) {
//                    NowPlayingMainTabPage.Mode.LARGE -> spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMediumLow)
                    else -> spring()
                }
            )
        }
    }

    private var long_press_menu_data: LongPressMenuData? by mutableStateOf(null)
    private var long_press_menu_showing: Boolean by mutableStateOf(false)
    private var long_press_menu_direct: Boolean by mutableStateOf(false)

    private val np_swipe_state: MutableState<SwipeableState<Int>> = mutableStateOf(
        SwipeableState(0)
    )
    private var np_swipe_anchors: Map<Float, Int>? by mutableStateOf(null)

    private var download_request_songs: List<Song>? by mutableStateOf(null)
    private var download_request_callback: DownloadRequestCallback? by mutableStateOf(null)

    override val expansion = NowPlayingExpansionState(this, np_swipe_state, coroutine_scope)
    override val session_started: Boolean get() = _player?.service_player?.session_started == true
    override var screen_size: DpSize by mutableStateOf(DpSize.Zero)

    override val app_page_state = AppPageState(this)
    override val main_multiselect_context: MediaItemMultiSelectContext = MediaItemMultiSelectContext()
    override var np_theme_mode: ThemeMode by mutableStateOf(
        Settings.getEnum(ThemeSettings.Key.NOWPLAYING_THEME_MODE, context.getPrefs()))
    override val np_overlay_menu: MutableState<PlayerOverlayMenu?> = mutableStateOf(null)
    override val top_bar: MusicTopBar = MusicTopBar(this)

    init {
        low_memory_listener = {
            if (app_page != app_page_state.SongFeed) {
                app_page_state.SongFeed.resetSongFeed()
            }
        }

        prefs_listener = object : PlatformPreferencesListener {
            override fun onChanged(prefs: PlatformPreferences, key: String) {
                when (key) {
                    ThemeSettings.Key.NOWPLAYING_THEME_MODE.getName() -> {
                        np_theme_mode = Settings.getEnum(ThemeSettings.Key.NOWPLAYING_THEME_MODE, prefs)
                    }
                }
            }
        }
    }

    fun onStart() {
        SpMp.addLowMemoryListener(low_memory_listener)
        context.getPrefs().addListener(prefs_listener)
        top_bar.reconnect()

        if (PlatformPlayerService.isServiceRunning(context)) {
            connectService(null)
        }
        else {
            coroutine_scope.launch {
                if (PersistentQueueHandler.isPopulatedQueueSaved(context)) {
                    connectService(null)
                }
            }
        }
    }

    fun onStop() {
        SpMp.removeLowMemoryListener(low_memory_listener)
        context.getPrefs().removeListener(prefs_listener)
    }

    fun release() {
        service_connection?.also {
            PlatformPlayerService.disconnect(context, it)
        }
        service_connection = null
        _player = null
        top_bar.release()
    }

    override fun interactService(action: (player: PlatformPlayerService) -> Unit) {
        synchronized(service_connected_listeners) {
            _player?.also {
                action(it)
                return
            }

            service_connected_listeners.add {
                action(_player!!)
            }
        }
    }

    private fun Density.getNpBottomPadding(system_insets: WindowInsets, navigation_insets: WindowInsets, keyboard_insets: WindowInsets?): Int {
        val ime_padding: Int =
            if (keyboard_insets == null || np_overlay_menu.value != null) 0
            else keyboard_insets.getBottom(this).let { ime ->
                    if (ime > 0) {
                        val nav = navigation_insets.getBottom(this@getNpBottomPadding)
                        return@let ime.coerceAtMost(
                            (ime - nav).coerceAtLeast(0)
                        )
                    }
                    return@let ime
                }

        return system_insets.getBottom(this) + ime_padding
    }

    @Composable
    override fun nowPlayingTopOffset(base: Modifier): Modifier {
        val system_insets = WindowInsets.systemBars
        val navigation_insets = WindowInsets.navigationBars
        val keyboard_insets = WindowInsets.ime
        val screen_height: Dp = screen_size.height

        return base
            .offset {
                val bottom_padding: Int = getNpBottomPadding(system_insets, navigation_insets, keyboard_insets)
                val swipe_offset: Dp =
                    if (session_started) -np_swipe_state.value.offset.value.dp - (screen_height * 0.5f)
                    else 0.dp

                IntOffset(
                    0,
                    swipe_offset.toPx().toInt() - bottom_padding
                )
            }
            .padding(start = system_insets.getStart(), end = system_insets.getEnd())
    }

    @Composable
    override fun nowPlayingBottomPadding(include_np: Boolean): Dp {
        val bottom_padding = with(LocalDensity.current) {
            LocalDensity.current.getNpBottomPadding(WindowInsets.systemBars, WindowInsets.navigationBars, WindowInsets.ime).toDp()
        }

        if (include_np) {
            val np by animateDpAsState(if (session_started) MINIMISED_NOW_PLAYING_HEIGHT_DP.dp else 0.dp)
            return np + bottom_padding
        }
        return bottom_padding
    }

    override fun onNavigationBarTargetColourChanged(colour: Color?, from_lpm: Boolean) {
        if (!from_lpm && long_press_menu_showing) {
            return
        }

        context.setNavigationBarColour(
            colour ?: if (from_lpm) getNPBackground() else null
        )
    }

    override fun openAppPage(page: AppPage?, from_current: Boolean, replace_current: Boolean) {
        if (page != app_page) {
            if (!replace_current) {
                app_page_undo_stack.add(app_page)
            }
            app_page_state.setPage(page, from_current = from_current, going_back = false)

            if (np_swipe_state.value.targetValue != 0) {
                switchNowPlayingPage(0)
            }
            hideLongPressMenu()
        }
    }

    override fun navigateBack() {
        if (app_page.onBackNavigation()) {
            return
        }
        app_page_state.setPage(app_page_undo_stack.removeLastOrNull(), from_current = false, going_back = true)
    }

    override fun onMediaItemClicked(item: MediaItem, multiselect_key: Int?) {
        if (item is Song) {
            playMediaItem(item)
            onPlayActionOccurred()
        }
        else {
            openMediaItem(item)
        }
    }
    override fun onMediaItemLongClicked(item: MediaItem, long_press_data: LongPressMenuData?) {
        showLongPressMenu(long_press_data ?: item.getLongPressMenuData())
    }

    override fun openMediaItem(item: MediaItem, from_current: Boolean, replace_current: Boolean, browse_params: BrowseParamsData?) {
        if (item is Artist && item.isForItem()) {
            return
        }
        openAppPage(MediaItemAppPage(app_page_state, item.getHolder(), browse_params), from_current, replace_current)
    }

    override fun openViewMorePage(browse_id: String, title: String?) {
        openAppPage(app_page_state.getViewMorePage(browse_id, title))
    }

    override fun openNowPlayingPlayerOverlayMenu(menu: PlayerOverlayMenu?) {
        np_overlay_menu.value = menu
        expansion.scrollTo(1)
    }

    override fun onPlayActionOccurred() {
        if (np_swipe_state.value.targetValue == 0 && Settings.get(BehaviourSettings.Key.OPEN_NP_ON_SONG_PLAYED)) {
            switchNowPlayingPage(1)
        }
    }

    override fun playMediaItem(item: MediaItem, shuffle: Boolean, at_index: Int) {
        withPlayer {
            if (item is Song) {
                playSong(
                    item,
                    start_radio = BehaviourSettings.Key.START_RADIO_ON_SONG_PRESS.get(context),
                    shuffle = shuffle,
                    at_index = at_index
                )
            }
            else {
                startRadioAtIndex(at_index, item, shuffle = shuffle)
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

    override fun showLongPressMenu(data: LongPressMenuData) {
        // Check lateinit
        data.layout_size

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
        long_press_menu_data = null
    }

    @Composable
    fun NowPlaying() {
        val player: PlayerState = LocalPlayerState.current
        val density: Density = LocalDensity.current
        val bottom_padding: Int = density.getNpBottomPadding(WindowInsets.systemBars, WindowInsets.navigationBars, WindowInsets.ime)

        val vertical_page_count: Int = getNowPlayingVerticalPageCount(player)
        val minimised_now_playing_height: Dp = MINIMISED_NOW_PLAYING_HEIGHT_DP.dp

        LaunchedEffect(screen_size.height, bottom_padding, vertical_page_count) {
            val half_screen_height: Float = screen_size.height.value * 0.5f

            with(density) {
                np_swipe_anchors = (0..vertical_page_count)
                    .associateBy { anchor ->
                        if (anchor == 0) minimised_now_playing_height.value - half_screen_height
                        else ((screen_size.height - bottom_padding.toDp()).value * anchor) - half_screen_height
                    }
            }

            val current_swipe_value = np_swipe_state.value.targetValue
            np_swipe_state.value = SwipeableState(0).apply {
                init(mapOf(-half_screen_height to 0))
                snapTo(current_swipe_value)
            }
        }

        if (np_swipe_anchors != null) {
            com.toasterofbread.spmp.ui.layout.nowplaying.NowPlaying(
                np_swipe_state.value,
                np_swipe_anchors!!,
                content_padding = PaddingValues(start = WindowInsets.getStart(), end = WindowInsets.getEnd())
            )
        }
    }

    @Composable
    fun PersistentContent() {
        long_press_menu_data?.also { data ->
            LongPressMenu(
                long_press_menu_showing,
                { hideLongPressMenu() },
                data
            )
        }

        download_request_songs?.also { songs ->
            DownloadMethodSelectionDialog(
                onCancelled = {
                    download_request_songs = null
                    download_request_callback?.invoke(null)
                },
                onSelected = { method ->
                    method.execute(context, songs, download_request_callback)
                    download_request_songs = null
                },
                songs = songs
            )
        }
    }

    @Composable
    fun HomePage() {
        BackHandler(app_page_undo_stack.isNotEmpty()) {
            navigateBack()
        }

        CompositionLocalProvider(LocalContentColor provides context.theme.on_background) {
            MainPageDisplay()
        }
    }

    // PlayerServiceHost

    override val controller: PlatformPlayerService? get() = _player
    override fun withPlayer(action: PlayerServicePlayer.() -> Unit) {
        _player?.also {
            action(it.service_player)
            return
        }

        connectService {
            action(it.service_player)
        }
    }

    @Composable
    override fun withPlayerComposable(action: @Composable PlayerServicePlayer.() -> Unit) {
        connectService(null)
        _player?.service_player?.also {
            action(it)
        }
    }

    val service_connected: Boolean get() = _player?.load_state?.loading == false
    val service_loading_message: String? get() = _player?.load_state?.takeIf { it.loading }?.loading_message

    private var service_connecting = false
    private var service_connected_listeners = mutableListOf<(PlatformPlayerService) -> Unit>()
    private var service_connection: Any? = null

    private fun connectService(onConnected: ((PlatformPlayerService) -> Unit)?) {
        synchronized(service_connected_listeners) {
            if (service_connecting) {
                if (onConnected != null) {
                    service_connected_listeners.add(onConnected)
                }
                return
            }

            _player?.also { service ->
                onConnected?.invoke(service)
                return
            }

            service_connecting = true
            service_connection = PlatformPlayerService.connect(
                context,
                _player,
                { service ->
                    synchronized(service_connected_listeners) {
                        _player = service
                        status.setPlayer(service)
                        service_connecting = false

                        onConnected?.invoke(service)
                        for (listener in service_connected_listeners) {
                            listener.invoke(service)
                        }
                        service_connected_listeners.clear()
                    }
                },
                {
                    service_connecting = false
                }
            )
        }
    }

    override val status: PlayerStatus = PlayerStatus()

    override fun isRunningAndFocused(): Boolean {
        return controller?.has_focus == true
    }

    override fun onSongDownloadRequested(songs: List<Song>, callback: DownloadRequestCallback?) {
        download_request_songs = songs
        download_request_callback = callback
    }
}
