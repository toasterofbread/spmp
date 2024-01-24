package com.toasterofbread.spmp.ui.layout.apppage.mainpage

import LocalPlayerState
import SpMp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeableState
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import com.toasterofbread.composekit.platform.PlatformPreferences
import com.toasterofbread.composekit.platform.PlatformPreferencesListener
import com.toasterofbread.composekit.platform.composable.BackHandler
import com.toasterofbread.composekit.utils.common.init
import com.toasterofbread.composekit.utils.composable.getEnd
import com.toasterofbread.composekit.utils.composable.getStart
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.layout.BrowseParamsData
import com.toasterofbread.spmp.model.mediaitem.playlist.Playlist
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.settings.Settings
import com.toasterofbread.spmp.model.settings.category.BehaviourSettings
import com.toasterofbread.spmp.model.settings.category.ThemeSettings
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.FormFactor
import com.toasterofbread.spmp.platform.download.DownloadMethodSelectionDialog
import com.toasterofbread.spmp.platform.form_factor
import com.toasterofbread.spmp.platform.playerservice.PlatformPlayerService
import com.toasterofbread.spmp.platform.playerservice.PlayerServicePlayer
import com.toasterofbread.spmp.service.playercontroller.PersistentQueueHandler
import com.toasterofbread.spmp.ui.component.MusicTopBar
import com.toasterofbread.spmp.ui.component.longpressmenu.LongPressMenu
import com.toasterofbread.spmp.ui.component.longpressmenu.LongPressMenuData
import com.toasterofbread.spmp.ui.component.mediaitempreview.getLongPressMenuData
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.component.multiselect.MultiSelectInfoDisplayContent
import com.toasterofbread.spmp.ui.component.multiselect.MultiSelectItem
import com.toasterofbread.spmp.ui.layout.apppage.AppPage
import com.toasterofbread.spmp.ui.layout.apppage.AppPageState
import com.toasterofbread.spmp.ui.layout.apppage.AppPageWithItem
import com.toasterofbread.spmp.ui.layout.apppage.MediaItemAppPage
import com.toasterofbread.spmp.ui.layout.artistpage.ArtistAppPage
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingExpansionState
import com.toasterofbread.spmp.ui.layout.nowplaying.ThemeMode
import com.toasterofbread.spmp.ui.layout.nowplaying.getNPBackground
import com.toasterofbread.spmp.ui.layout.nowplaying.getNowPlayingVerticalPageCount
import com.toasterofbread.spmp.ui.layout.nowplaying.overlay.PlayerOverlayMenu
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class FeedLoadState { PREINIT, NONE, LOADING, CONTINUING }

// TODO | There is almost zero reason for this to be two classes
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
                    FormFactor.LANDSCAPE -> spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMediumLow)
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
    private var download_request_always_show_options: Boolean by mutableStateOf(false)
    private var download_request_callback: DownloadRequestCallback? by mutableStateOf(null)

    override val expansion = NowPlayingExpansionState(this, np_swipe_state, coroutine_scope)
    override val session_started: Boolean get() = _player?.service_player?.session_started == true
    override var screen_size: DpSize by mutableStateOf(DpSize.Zero)

    override val app_page_state = AppPageState(this)
    override val main_multiselect_context: MediaItemMultiSelectContext = AppPageMultiSelectContext(this)
    override var np_theme_mode: ThemeMode by mutableStateOf(Settings.getEnum(ThemeSettings.Key.NOWPLAYING_THEME_MODE, context.getPrefs()))
    override val top_bar: MusicTopBar = MusicTopBar(this)

    override val np_overlay_menu: MutableState<PlayerOverlayMenu?> = mutableStateOf(null)
    private val np_overlay_menu_queue: MutableList<PlayerOverlayMenu> = mutableListOf()

    override fun navigateNpOverlayMenuBack() {
        np_overlay_menu.value = np_overlay_menu_queue.removeLastOrNull()
    }

    override fun openNpOverlayMenu(menu: PlayerOverlayMenu?) {
        if (menu == null) {
            np_overlay_menu.value = null
            np_overlay_menu_queue.clear()
            return
        }

        np_overlay_menu.value?.also {
            np_overlay_menu_queue.add(it)
        }
        np_overlay_menu.value = menu
    }

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

    private var now_playing_top_offset_id: Int = 0
    private var now_playing_top_offset_item_sizes: MutableMap<Int, Dp> = mutableStateMapOf()

    @Composable
    override fun nowPlayingTopOffset(base: Modifier, force_top: Boolean): Modifier {
        val system_insets: WindowInsets = WindowInsets.systemBars
        val navigation_insets: WindowInsets = WindowInsets.navigationBars
        val keyboard_insets: WindowInsets = WindowInsets.ime

        val id: Int = remember {
            if (force_top) -(now_playing_top_offset_id++)
            else now_playing_top_offset_id++
        }
        val density: Density = LocalDensity.current

        DisposableEffect(Unit) {
            onDispose {
                now_playing_top_offset_item_sizes.remove(id)
            }
        }

        val additional_offset: Dp by animateDpAsState(
            if (force_top)
                now_playing_top_offset_item_sizes.entries.sumOf {
                    if (it.key <= id) 0.0
                    else it.value.value + 15.0
                }.dp
            else 0.dp
        )

        return base
            .offset {
                val bottom_padding: Int = getNpBottomPadding(system_insets, navigation_insets, keyboard_insets)
                val swipe_offset: Dp =
                    if (session_started) -np_swipe_state.value.offset.value.dp - (screen_size.height * 0.5f)
                    else 0.dp

                IntOffset(
                    0,
                    swipe_offset.roundToPx() - bottom_padding - additional_offset.roundToPx()
                )
            }
            .padding(start = system_insets.getStart(), end = system_insets.getEnd())
            .onSizeChanged {
                with (density) {
                    now_playing_top_offset_item_sizes[id] = it.height.toDp()
                }
            }
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
        else if (
            item is Playlist
            && BehaviourSettings.Key.TREAT_SINGLES_AS_SONG.get()
            && BehaviourSettings.Key.TREAT_ANY_SINGLE_ITEM_PLAYLIST_AS_SINGLE.get()
        ) {
            coroutine_scope.launch {
                item.loadData(context).onSuccess { data ->
                    val single = data.items?.singleOrNull()
                    if (single != null) {
                        onMediaItemClicked(single)
                    }
                    else {
                        openMediaItem(item)
                    }
                }
            }
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

        val page: AppPageWithItem =
            if (item is Artist)
                ArtistAppPage(
                    app_page_state,
                    item,
                    browse_params = browse_params?.let { params ->
                        Pair(params, context.ytapi.ArtistWithParams)
                    }
                )
            else MediaItemAppPage(app_page_state, item.getHolder(), browse_params)

        openAppPage(page, from_current, replace_current)
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

    private var multiselect_info_display_height: Dp by mutableStateOf(0.dp)
    internal val multiselect_info_all_items_getters: MutableList<() -> List<List<MultiSelectItem>>> = mutableListOf()

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
                songs = songs,
                always_show_options = download_request_always_show_options
            )
        }

        if (form_factor.is_large) {
            val density: Density = LocalDensity.current

            AnimatedVisibility(main_multiselect_context.is_active, enter = fadeIn(), exit = fadeOut()) {
                Box(Modifier.fillMaxSize().padding(15.dp)) {
                    val background_colour: Color = theme.accent

                    CompositionLocalProvider(LocalContentColor provides theme.on_accent) {
                        main_multiselect_context.MultiSelectInfoDisplayContent(
                            Modifier
                                .width(IntrinsicSize.Max)
                                .align(Alignment.BottomEnd)
                                .then(nowPlayingTopOffset(Modifier, true))
                                .background(background_colour, MaterialTheme.shapes.small)
                                .padding(10.dp)
                                .onSizeChanged {
                                    multiselect_info_display_height = with (density) {
                                        it.height.toDp()
                                    }
                                },
                            background_colour,
                            getAllItems = {
                                multiselect_info_all_items_getters.flatMap { it() }
                            }
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun HomePage() {
        BackHandler(app_page_undo_stack.isNotEmpty()) {
            navigateBack()
        }

        CompositionLocalProvider(LocalContentColor provides context.theme.on_background) {
            val bottom_padding: Dp by animateDpAsState(
                if (form_factor.is_large && main_multiselect_context.is_active) multiselect_info_display_height
                else 0.dp
            )

            MainPageDisplay(bottom_padding)
        }
    }

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

    override fun onSongDownloadRequested(songs: List<Song>, always_show_options: Boolean, callback: DownloadRequestCallback?) {
        download_request_songs = songs
        download_request_always_show_options = always_show_options
        download_request_callback = callback
    }
}

private class AppPageMultiSelectContext(private val player: PlayerStateImpl): MediaItemMultiSelectContext() {
    @Composable
    override fun InfoDisplayContent(
        modifier: Modifier,
        content_modifier: Modifier,
        getAllItems: (() -> List<List<MultiSelectItem>>)?,
        wrapContent: @Composable (@Composable () -> Unit) -> Unit,
        show_alt_content: Boolean,
        altContent: (@Composable () -> Unit)?
    ): Boolean {
        if (player.form_factor.is_large) {
            // Displayed in PlayerStateImpl.PersistentContent()

            DisposableEffect(is_active, getAllItems) {
                if (!is_active) {
                    return@DisposableEffect onDispose {}
                }

                if (getAllItems != null) {
                    player.multiselect_info_all_items_getters.add(getAllItems)
                }

                onDispose {
                    if (getAllItems != null) {
                        player.multiselect_info_all_items_getters.remove(getAllItems)
                    }
                }
            }

            return false
        }
        return super.InfoDisplayContent(modifier, content_modifier, getAllItems, wrapContent, show_alt_content, altContent)
    }
}
