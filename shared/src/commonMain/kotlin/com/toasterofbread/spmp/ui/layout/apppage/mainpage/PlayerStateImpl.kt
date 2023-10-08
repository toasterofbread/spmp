package com.toasterofbread.spmp.ui.layout.apppage.mainpage

import SpMp
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
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
import com.toasterofbread.spmp.model.*
import com.toasterofbread.spmp.model.mediaitem.*
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.layout.BrowseParamsData
import com.toasterofbread.spmp.model.mediaitem.playlist.Playlist
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.platform.*
import com.toasterofbread.spmp.platform.composable.BackHandler
import com.toasterofbread.spmp.platform.playerservice.PlatformPlayerService
import com.toasterofbread.spmp.ui.component.*
import com.toasterofbread.spmp.ui.component.longpressmenu.LongPressMenu
import com.toasterofbread.spmp.ui.component.longpressmenu.LongPressMenuData
import com.toasterofbread.spmp.ui.component.mediaitempreview.*
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.layout.*
import com.toasterofbread.spmp.ui.layout.apppage.AppPage
import com.toasterofbread.spmp.ui.layout.apppage.AppPageState
import com.toasterofbread.spmp.ui.layout.apppage.MediaItemAppPage
import com.toasterofbread.spmp.ui.layout.nowplaying.NOW_PLAYING_VERTICAL_PAGE_COUNT
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingExpansionState
import com.toasterofbread.spmp.ui.layout.nowplaying.ThemeMode
import com.toasterofbread.spmp.ui.layout.nowplaying.getAdjustedKeyboardHeight
import com.toasterofbread.spmp.ui.layout.nowplaying.getNPBackground
import com.toasterofbread.spmp.ui.layout.nowplaying.overlay.PlayerOverlayMenu
import com.toasterofbread.spmp.ui.theme.Theme
import com.toasterofbread.utils.common.init
import com.toasterofbread.utils.composable.OnChangedEffect
import kotlinx.coroutines.*

enum class FeedLoadState { PREINIT, NONE, LOADING, CONTINUING }

@Composable
fun PlayerState.getMainPageItemSize(): DpSize {
    val width = if (isScreenLarge()) MEDIAITEM_PREVIEW_SQUARE_SIZE_LARGE.dp else MEDIAITEM_PREVIEW_SQUARE_SIZE_SMALL.dp
    return DpSize(
        width,
        width + 30.dp
    )
}

@OptIn(ExperimentalMaterialApi::class)
class PlayerStateImpl(override val context: PlatformContext): PlayerState(null, null, null) {
    private var _player: PlatformPlayerService? by mutableStateOf(null)
    override val session_started: Boolean get() = _player?.service_player?.session_started == true

    override var screen_size: DpSize by mutableStateOf(DpSize.Zero)

    private var now_playing_switch_page: Int by mutableStateOf(-1)
    private val app_page_undo_stack: MutableList<AppPage?> = mutableStateListOf()

    private val bottom_padding_anim = Animatable(context.getNavigationBarHeight().toFloat())

    private val low_memory_listener: () -> Unit
    private val prefs_listener: PlatformPreferences.Listener

    private fun switchNowPlayingPage(page: Int) {
        now_playing_switch_page = page
    }

    private var long_press_menu_data: LongPressMenuData? by mutableStateOf(null)
    private var long_press_menu_showing: Boolean by mutableStateOf(false)
    private var long_press_menu_direct: Boolean by mutableStateOf(false)

    private val np_swipe_state: MutableState<SwipeableState<Int>> = mutableStateOf(SwipeableState(0))
    private var np_swipe_anchors: Map<Float, Int>? by mutableStateOf(null)

    val expansion_state = NowPlayingExpansionState(np_swipe_state)

    override val app_page_state = AppPageState(context)
    override val bottom_padding: Float get() = bottom_padding_anim.value
    override val main_multiselect_context: MediaItemMultiSelectContext = getPlayerStateMultiSelectContext()
    override var np_theme_mode: ThemeMode by mutableStateOf(Settings.getEnum(Settings.KEY_NOWPLAYING_THEME_MODE, context.getPrefs()))
    override val np_overlay_menu: MutableState<PlayerOverlayMenu?> = mutableStateOf(null)
    override val top_bar: MusicTopBar = MusicTopBar(this)

    init {
        low_memory_listener = {
            if (app_page != app_page_state.SongFeed) {
                app_page_state.SongFeed.resetSongFeed()
            }
        }

        prefs_listener = object : PlatformPreferences.Listener {
            override fun onChanged(prefs: PlatformPreferences, key: String) {
                when (key) {
                    Settings.KEY_NOWPLAYING_THEME_MODE.name -> {
                        np_theme_mode = Settings.getEnum(Settings.KEY_NOWPLAYING_THEME_MODE, prefs)
                    }
                }
            }
        }
    }

    fun onStart() {
        SpMp.addLowMemoryListener(low_memory_listener)
        context.getPrefs().addListener(prefs_listener)

        if (service_connecting) {
            return
        }

        service_connecting = true
        service_connection = PlatformPlayerService.connect(
            context,
            PlatformPlayerService::class.java,
            _player,
            {
                synchronized(service_connected_listeners) {
                    _player = PlatformPlayerService.instance
                    status = PlayerStatus(this, _player!!)
                    service_connecting = false

                    service_connected_listeners.forEach { it.invoke() }
                    service_connected_listeners.clear()
                }
            }
        ) {
            service_connecting = false
        }

        top_bar.reconnect()
    }

    fun onStop() {
        PlatformPlayerService.disconnect(context, service_connection)
        SpMp.removeLowMemoryListener(low_memory_listener)
        context.getPrefs().removeListener(prefs_listener)
    }

    fun release() {
        onStop()
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

    private fun Density.getNpBottomPadding(insets: WindowInsets): Int {
        return context.getNavigationBarHeight() + (if (np_overlay_menu.value == null) insets.getAdjustedKeyboardHeight(this, context) else 0)
    }

    @Composable
    override fun nowPlayingTopOffset(base: Modifier): Modifier {
        val density = LocalDensity.current
        val keyboard_insets = WindowInsets.ime
        val screen_height: Dp = screen_size.height

        return base.offset {
            with (density) {
                val bottom_padding = getNpBottomPadding(keyboard_insets)
                val swipe_offset: Dp =
                    if (session_started) -np_swipe_state.value.offset.value.dp - (screen_height * 0.5f)
                    else 0.dp

                IntOffset(
                    0,
                    swipe_offset.toPx().toInt() - bottom_padding
                )
            }
        }
    }

    @Composable
    override fun nowPlayingBottomPadding(include_np: Boolean): Dp {
        val bottom_padding = with(LocalDensity.current) {
            LocalDensity.current.getNpBottomPadding(WindowInsets.ime).toDp()
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
        expansion_state.scrollTo(1)
    }

    override fun playMediaItem(item: MediaItem, shuffle: Boolean, at_index: Int) {
        withPlayer {
            if (item is Song) {
                service_player.playSong(item, start_radio = true, shuffle = shuffle, at_index = at_index)
            }
            else {
                service_player.startRadioAtIndex(at_index, item, shuffle = shuffle)
            }

            if (np_swipe_state.value.targetValue == 0 && Settings.get(Settings.KEY_OPEN_NP_ON_SONG_PLAYED)) {
                switchNowPlayingPage(1)
            }
        }
    }

    override fun playPlaylist(playlist: Playlist, from_index: Int) {
        withPlayer {
            service_player.startRadioAtIndex(
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
        val density = LocalDensity.current
        val bottom_padding = density.getNpBottomPadding(WindowInsets.ime)

        OnChangedEffect(bottom_padding) {
            bottom_padding_anim.animateTo(bottom_padding.toFloat())
        }

        LaunchedEffect(screen_size.height, bottom_padding) {
            val half_screen_height: Float = screen_size.height.value * 0.5f

            with(density) {
                np_swipe_anchors = (0..NOW_PLAYING_VERTICAL_PAGE_COUNT)
                    .associateBy { anchor ->
                        if (anchor == 0) MINIMISED_NOW_PLAYING_HEIGHT_DP.toFloat() - half_screen_height
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
            com.toasterofbread.spmp.ui.layout.nowplaying.NowPlaying(np_swipe_state.value, np_swipe_anchors!!)
        }

        OnChangedEffect(now_playing_switch_page) {
            if (now_playing_switch_page >= 0) {
                np_swipe_state.value.animateTo(now_playing_switch_page)
                now_playing_switch_page = -1
            }
        }
    }

    @Composable
    fun LongPressMenu() {
        long_press_menu_data?.also { data ->
            LongPressMenu(
                long_press_menu_showing,
                { hideLongPressMenu() },
                data
            )
        }
    }

    @Composable
    fun HomePage() {
        BackHandler(app_page_undo_stack.isNotEmpty()) {
            navigateBack()
        }

        CompositionLocalProvider(LocalContentColor provides Theme.on_background) {
            MainPageDisplay()
        }
    }

    // PlayerServiceHost

    override val controller: PlatformPlayerService? get() = PlatformPlayerService.instance
    override fun withPlayer(action: PlatformPlayerService.() -> Unit) {
        _player?.also { action(it) }
    }

    val service_connected: Boolean get() = _player != null

    private var service_connecting = false
    private var service_connected_listeners = mutableListOf<() -> Unit>()
    private lateinit var service_connection: Any

    override lateinit var status: PlayerStatus
        private set

    override fun isRunningAndFocused(): Boolean {
        return controller?.has_focus == true
    }
}
