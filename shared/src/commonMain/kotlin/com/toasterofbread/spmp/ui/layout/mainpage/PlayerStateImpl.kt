package com.toasterofbread.spmp.ui.layout.mainpage

import LocalPlayerState
import SpMp
import androidx.compose.animation.Crossfade
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
import com.toasterofbread.composesettings.ui.SettingsInterface
import com.toasterofbread.composesettings.ui.item.SettingsValueState
import com.toasterofbread.spmp.model.*
import com.toasterofbread.spmp.model.mediaitem.*
import com.toasterofbread.spmp.platform.*
import com.toasterofbread.spmp.platform.composable.BackHandler
import com.toasterofbread.spmp.service.playerservice.PlayerService
import com.toasterofbread.spmp.ui.component.*
import com.toasterofbread.spmp.ui.component.longpressmenu.LongPressMenu
import com.toasterofbread.spmp.ui.component.longpressmenu.LongPressMenuData
import com.toasterofbread.spmp.ui.component.mediaitempreview.*
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.layout.*
import com.toasterofbread.spmp.ui.layout.artistpage.ArtistPage
import com.toasterofbread.spmp.ui.layout.library.PlaylistsPage
import com.toasterofbread.spmp.ui.layout.library.SongsPage
import com.toasterofbread.spmp.ui.layout.nowplaying.NOW_PLAYING_VERTICAL_PAGE_COUNT
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingExpansionState
import com.toasterofbread.spmp.ui.layout.nowplaying.ThemeMode
import com.toasterofbread.spmp.ui.layout.nowplaying.getNPBackground
import com.toasterofbread.spmp.ui.layout.nowplaying.overlay.OverlayMenu
import com.toasterofbread.spmp.ui.layout.playlistpage.PlaylistPage
import com.toasterofbread.spmp.ui.layout.prefspage.PrefsPage
import com.toasterofbread.spmp.ui.layout.prefspage.PrefsPageCategory
import com.toasterofbread.spmp.ui.layout.prefspage.getPrefsPageSettingsInterface
import com.toasterofbread.spmp.ui.layout.radiobuilder.RadioBuilderPage
import com.toasterofbread.spmp.ui.theme.Theme
import com.toasterofbread.spmp.youtubeapi.composable.LoginPage
import com.toasterofbread.utils.composable.OnChangedEffect
import com.toasterofbread.utils.init
import com.toasterofbread.utils.toFloat
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

interface PlayerOverlayPage {
    @Composable
    fun Page(previous_item: MediaItemHolder?, bottom_padding: Dp, close: () -> Unit)

    open fun getItem(): MediaItem? = null

    data class MediaItemPage(private val holder: MediaItemHolder, private val browse_params: String? = null): PlayerOverlayPage {
        override fun getItem(): MediaItem? = holder.item

        @Composable
        override fun Page(previous_item: MediaItemHolder?, bottom_padding: Dp, close: () -> Unit) {
            val player = LocalPlayerState.current

            when (val item = holder.item) {
                null -> close()
                is Playlist -> PlaylistPage(
                    item,
                    previous_item?.item,
                    PaddingValues(top = player.context.getStatusBarHeight(), bottom = bottom_padding),
                    close
                )
                is Artist -> ArtistPage(
                    item,
                    previous_item?.item,
                    bottom_padding,
                    browse_params?.let { params ->
                        Pair(params, player.context.ytapi.ArtistsWithParams)
                    },
                    close
                )
                is Song -> SongRelatedPage(
                    item,
                    player.context.ytapi.SongRelatedContent,
                    Modifier.fillMaxSize(),
                    previous_item?.item,
                    PaddingValues(
                        top = player.context.getStatusBarHeight(),
                        bottom = bottom_padding,
                        start = player.getDefaultHorizontalPadding(),
                        end = player.getDefaultHorizontalPadding()
                    ),
                    close = close
                )
                else -> throw NotImplementedError(item::class.toString())
            }
        }
    }

    data class YtmLoginPage(val page: LoginPage, private val confirm_param: Any? = null): PlayerOverlayPage {
        @Composable
        override fun Page(previous_item: MediaItemHolder?, bottom_padding: Dp, close: () -> Unit) {
            page.LoginPage(
                Modifier.fillMaxSize(),
                confirm_param = confirm_param
            ) { result ->
                result?.fold(
                    { Settings.KEY_YTM_AUTH.set(it) },
                    { TODO(it.toString()) }
                )
                close()
            }
        }
    }

    private data class GenericFeedViewMorePage(private val browse_id: String, private val title: String?): PlayerOverlayPage {
        @Composable
        override fun Page(previous_item: MediaItemHolder?, bottom_padding: Dp, close: () -> Unit) {
            GenericFeedViewMorePage(browse_id, Modifier.fillMaxSize(), bottom_padding = bottom_padding, title = title)
        }
    }

    companion object {
        fun getViewMorePage(browse_id: String, title: String?): PlayerOverlayPage = when (browse_id) {
            "FEmusic_listen_again", "FEmusic_mixed_for_you", "FEmusic_new_releases_albums" -> GenericFeedViewMorePage(browse_id, title)
            "FEmusic_moods_and_genres" -> TODO(browse_id)
            "FEmusic_charts" -> TODO(browse_id)
            else -> throw NotImplementedError(browse_id)
        }

        val RadioBuilderPage = object : PlayerOverlayPage {
            @Composable
            override fun Page(previous_item: MediaItemHolder?, bottom_padding: Dp, close: () -> Unit) {
                RadioBuilderPage(
                    bottom_padding,
                    Modifier.fillMaxSize(),
                    close
                )
            }
        }

        val SettingsPage = object : PlayerOverlayPage {
            val current_category: MutableState<PrefsPageCategory?> = mutableStateOf(null)
            val pill_menu: PillMenu = PillMenu(follow_player = true)
            val ytm_auth: SettingsValueState<Set<String>> =
                SettingsValueState<Set<String>>(
                    Settings.KEY_YTM_AUTH.name
                ).init(Settings.prefs, Settings.Companion::provideDefault)
            val settings_interface: SettingsInterface =
                getPrefsPageSettingsInterface(pill_menu, ytm_auth, { current_category.value }, { current_category.value = null })

            @Composable
            override fun Page(previous_item: MediaItemHolder?, bottom_padding: Dp, close: () -> Unit) {
                PrefsPage(bottom_padding, current_category, pill_menu, settings_interface, ytm_auth, Modifier.fillMaxSize(), close)
            }
        }

        val PlaylistsPage = PlaylistsPage()
        val SongsPage = SongsPage()
    }
}

@OptIn(ExperimentalMaterialApi::class)
class PlayerStateImpl(override val context: PlatformContext): PlayerState(null, null, null) {
    private var _player: PlayerService? by mutableStateOf(null)
    override val session_started: Boolean get() = _player?.session_started == true

    override var screen_size: DpSize by mutableStateOf(DpSize.Zero)

    private var now_playing_switch_page: Int by mutableStateOf(-1)
    private val main_page_undo_stack: MutableList<MainPage?> = mutableStateListOf()
    private val overlay_page_undo_stack: MutableList<Pair<PlayerOverlayPage, MediaItem?>?> = mutableListOf()
    private var main_page_showing: Boolean by mutableStateOf(false)

    @Composable
    private fun getCurrentBottomPadding(): Float =
        with(LocalDensity.current) {
            (session_started.toFloat() * MINIMISED_NOW_PLAYING_HEIGHT_DP.dp.toPx()) + context.getNavigationBarHeight()
        }
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

    val expansion_state = NowPlayingExpansionState(np_swipe_state, this)

    override val main_page_state = MainPageState(context)
    override var overlay_page: Pair<PlayerOverlayPage, MediaItem?>? by mutableStateOf(null)
        private set
    override val bottom_padding: Float get() = bottom_padding_anim.value
    override val main_multiselect_context: MediaItemMultiSelectContext = getPlayerStateMultiSelectContext()
    override var np_theme_mode: ThemeMode by mutableStateOf(Settings.getEnum(Settings.KEY_NOWPLAYING_THEME_MODE, context.getPrefs()))
    override val np_overlay_menu: MutableState<OverlayMenu?> = mutableStateOf(null)

    init {
        low_memory_listener = {
            if (!main_page_showing) {
                main_page_state.SongFeed.resetSongFeed()
            }
        }
        SpMp.addLowMemoryListener(low_memory_listener)

        prefs_listener = object : PlatformPreferences.Listener {
            override fun onChanged(prefs: PlatformPreferences, key: String) {
                when (key) {
                    Settings.KEY_NOWPLAYING_THEME_MODE.name -> {
                        np_theme_mode = Settings.getEnum(Settings.KEY_NOWPLAYING_THEME_MODE, prefs)
                    }
                }
            }
        }
        context.getPrefs().addListener(prefs_listener)
    }

    fun onStart() {
        if (service_connecting) {
            return
        }

        service_connecting = true
        service_connection = MediaPlayerService.connect(
            context,
            PlayerService::class.java,
            _player,
            { service ->
                synchronized(service_connected_listeners) {
                    _player = service
                    status = PlayerStatus(_player!!)
                    service_connecting = false

                    service_connected_listeners.forEach { it(service) }
                    service_connected_listeners.clear()
                }
            },
            {
                service_connecting = false
            }
        )
    }

    fun onStop() {
        MediaPlayerService.disconnect(context, service_connection)
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
        val screen_height: Dp = screen_size.height
        val bottom_padding: Dp = context.getNavigationBarHeightDp()
        val keyboard_insets: WindowInsets? = context.getImeInsets()

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

    override fun onNavigationBarTargetColourChanged(colour: Color?, from_lpm: Boolean) {
        if (!from_lpm && long_press_menu_showing) {
            return
        }

        context.setNavigationBarColour(
            colour ?: if (from_lpm) getNPBackground() else null
        )
    }

    override fun setMainPage(page: MainPage?) {
        val previous_page = main_page_state.current_page
        if (main_page_state.setPage(page)) {
            main_page_undo_stack.add(previous_page)
        }
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
        if (overlay_page != null) {
            overlay_page = overlay_page_undo_stack.removeLastOrNull()
        }
        else {
            main_page_state.setPage(main_page_undo_stack.removeLastOrNull(), true)
        }
    }

    override fun onMediaItemClicked(item: MediaItem, multiselect_key: Int?) {
        if (item is Song) {
            playMediaItem(item)
        }
        else {
            openMediaItem(item,)
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

    override fun openMediaItem(item: MediaItem, from_current: Boolean, browse_params: String?) {
        if (item is Artist && item.IsForItem.get(context.database)) {
            return
        }
        openPage(PlayerOverlayPage.MediaItemPage(item.getHolder(), browse_params), from_current)
    }

    override fun openViewMorePage(browse_id: String, title: String?) {
        openPage(PlayerOverlayPage.getViewMorePage(browse_id, title))
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
        val bottom_padding = getCurrentBottomPadding()
        OnChangedEffect(bottom_padding) {
            bottom_padding_anim.animateTo(bottom_padding)
        }

        val navigation_bar_height = context.getNavigationBarHeightDp()

        LaunchedEffect(screen_size.height, navigation_bar_height) {
            val half_screen_height = screen_size.height.value * 0.5f

            np_swipe_anchors = (0..NOW_PLAYING_VERTICAL_PAGE_COUNT)
                .associateBy { anchor ->
                    if (anchor == 0) MINIMISED_NOW_PLAYING_HEIGHT_DP.toFloat() - half_screen_height
                    else ((screen_size.height - navigation_bar_height).value * anchor) - half_screen_height
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
        BackHandler(main_page_undo_stack.isNotEmpty() || overlay_page != null) {
            navigateBack()
        }

        CompositionLocalProvider(LocalContentColor provides Theme.on_background) {
            Crossfade(targetState = overlay_page) { page ->
                Column(Modifier.fillMaxSize()) {
                    if (page != null && page.first !is PlayerOverlayPage.MediaItemPage) {
                        Spacer(Modifier.requiredHeight(context.getStatusBarHeight()))
                    }

                    val close = remember { { navigateBack() } }

                    if (page == null) {
                        DisposableEffect(Unit) {
                            check(!main_page_showing)
                            main_page_showing = true

                            onDispose {
                                check(main_page_showing)
                                main_page_showing = false
                            }
                        }

                        MainPageDisplay()
                    }
                    else {
                        page.first.Page(
                            page.second,
                            (if (session_started) MINIMISED_NOW_PLAYING_HEIGHT_DP.dp else 0.dp) + context.getNavigationBarHeightDp() + getDefaultVerticalPadding(),
                            close
                        )
                    }
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
