package com.toasterofbread.spmp.service.playercontroller

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import com.toasterofbread.composekit.platform.PlatformPreferences
import com.toasterofbread.composekit.platform.PlatformPreferencesListener
import com.toasterofbread.composekit.platform.composable.BackHandler
import com.toasterofbread.composekit.platform.composable.composeScope
import com.toasterofbread.composekit.settings.ui.Theme
import com.toasterofbread.composekit.utils.common.init
import com.toasterofbread.composekit.utils.composable.getEnd
import com.toasterofbread.composekit.utils.composable.getStart
import com.toasterofbread.db.Database
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
import com.toasterofbread.spmp.platform.download.DownloadStatus
import com.toasterofbread.spmp.platform.form_factor
import com.toasterofbread.spmp.platform.playerservice.PlatformPlayerService
import com.toasterofbread.spmp.platform.playerservice.PlayerServicePlayer
import com.toasterofbread.spmp.ui.component.longpressmenu.LongPressMenu
import com.toasterofbread.spmp.ui.component.longpressmenu.LongPressMenuData
import com.toasterofbread.spmp.ui.component.multiselect.AppPageMultiSelectContext
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.component.multiselect.MultiSelectInfoDisplayContent
import com.toasterofbread.spmp.ui.component.multiselect.MultiSelectItem
import com.toasterofbread.spmp.ui.layout.apppage.AppPage
import com.toasterofbread.spmp.ui.layout.apppage.AppPageState
import com.toasterofbread.spmp.ui.layout.apppage.AppPageWithItem
import com.toasterofbread.spmp.ui.layout.apppage.MediaItemAppPage
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.MINIMISED_NOW_PLAYING_HEIGHT_DP
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.MainPageDisplay
import com.toasterofbread.spmp.ui.layout.artistpage.ArtistAppPage
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingExpansionState
import com.toasterofbread.spmp.ui.layout.nowplaying.ThemeMode
import com.toasterofbread.spmp.ui.layout.nowplaying.getNPBackground
import com.toasterofbread.spmp.ui.layout.nowplaying.getNowPlayingVerticalPageCount
import com.toasterofbread.spmp.ui.layout.nowplaying.overlay.PlayerOverlayMenu
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingTopOffsetSection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.toasterofbread.spmp.ui.layout.contentbar.layoutslot.*
import com.toasterofbread.spmp.ui.layout.contentbar.*
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import com.toasterofbread.composekit.utils.composable.getTop
import com.toasterofbread.composekit.utils.composable.OnChangedEffect
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.requiredWidth
import kotlin.math.roundToInt

typealias DownloadRequestCallback = (DownloadStatus?) -> Unit

enum class FeedLoadState { PREINIT, NONE, LOADING, CONTINUING }

@OptIn(ExperimentalMaterialApi::class)
class PlayerState(val context: AppContext, internal val coroutine_scope: CoroutineScope) {
    val database: Database get() = context.database
    val theme: Theme get() = context.theme
    val app_page: AppPage get() = app_page_state.current_page

    private var _player: PlatformPlayerService? by mutableStateOf(null)

    private val app_page_undo_stack: MutableList<AppPage?> = mutableStateListOf()

    private val low_memory_listener: () -> Unit
    private val prefs_listener: PlatformPreferencesListener

    fun switchNowPlayingPage(page: Int) {
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
    private var np_bottom_bar_height: Dp by mutableStateOf(0.dp)
    private var np_bottom_bar_config: LayoutSlot.BelowPlayerConfig? by mutableStateOf(null)

    private var download_request_songs: List<Song>? by mutableStateOf(null)
    private var download_request_always_show_options: Boolean by mutableStateOf(false)
    private var download_request_callback: DownloadRequestCallback? by mutableStateOf(null)

    val expansion: NowPlayingExpansionState = NowPlayingExpansionState(this, np_swipe_state, coroutine_scope)
    var screen_size: DpSize by mutableStateOf(DpSize.Zero)

    val session_started: Boolean get() = _player?.service_player?.session_started == true
    var hide_player: Boolean by mutableStateOf(false)
    val player_showing: Boolean get() = session_started && !hide_player

    val app_page_state: AppPageState = AppPageState(this)
    val main_multiselect_context: MediaItemMultiSelectContext = AppPageMultiSelectContext(this)
    var np_theme_mode: ThemeMode by mutableStateOf(Settings.getEnum(ThemeSettings.Key.NOWPLAYING_THEME_MODE, context.getPrefs()))

    val np_overlay_menu: MutableState<PlayerOverlayMenu?> = mutableStateOf(null)
    private val np_overlay_menu_queue: MutableList<PlayerOverlayMenu> = mutableListOf()

    fun navigateNpOverlayMenuBack() {
        np_overlay_menu.value = np_overlay_menu_queue.removeLastOrNull()
    }

    fun openNpOverlayMenu(menu: PlayerOverlayMenu?) {
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
    }

    fun interactService(action: (player: PlatformPlayerService) -> Unit) {
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

    private var now_playing_top_offset_items: MutableMap<NowPlayingTopOffsetSection, MutableList<TopOffsetItem?>> = mutableStateMapOf()
    private data class TopOffsetItem(
        val height: Dp,
        val apply_spacing: Boolean = true,
        val displaying: Boolean = true
    )

    private fun getTopItemsHeight(spacing: Dp = 15.dp, filter: ((NowPlayingTopOffsetSection, Int) -> Boolean)? = null): Dp =
        now_playing_top_offset_items.entries.sumOf { items ->
            var acc: Float = 0f

            for (item in items.value.withIndex()) {
                if (item.value?.displaying != true) {
                    continue
                }

                if (filter?.invoke(items.key, item.index) == false) {
                    continue
                }

                val height: Float =
                    (item.value!!.height + (if (item.value!!.apply_spacing) spacing else 0.dp)).value

                if (items.key.isMerged()) {
                    acc = maxOf(acc, height)
                }
                else {
                    acc += height
                }
            }

            // https://youtrack.jetbrains.com/issue/KT-43310/Add-sumOf-with-Float-return-type
            return@sumOf acc.toDouble()
        }.dp

    @Composable
    fun nowPlayingTopOffset(
        base: Modifier,
        section: NowPlayingTopOffsetSection,
        apply_spacing: Boolean = true,
        displaying: Boolean = true
    ): Modifier {
        val density: Density = LocalDensity.current
        val system_insets: WindowInsets = WindowInsets.systemBars
        val navigation_insets: WindowInsets = WindowInsets.navigationBars
        val keyboard_insets: WindowInsets = WindowInsets.ime

        val offset_items: MutableList<TopOffsetItem?> = remember (section) {
            now_playing_top_offset_items.getOrPut(section) {
                mutableStateListOf()
            }
        }
        val index: Int = remember(offset_items) {
            offset_items.add(null)
            return@remember offset_items.size - 1
        }

        OnChangedEffect(displaying) {
            val item: TopOffsetItem? = offset_items.getOrNull(index)
            if (item != null) {
                offset_items[index] = item.copy(
                    displaying = displaying
                )
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                offset_items[index] = null
            }
        }

        val additional_offset: Dp by animateDpAsState(
            getTopItemsHeight(filter = { item_section, item_index ->
                !section.shouldIgnoreSection(item_section)
                && (
                    item_section.ordinal > section.ordinal
                    || (item_section.ordinal == section.ordinal && item_index < index)
                )
            })
        )

        return base
            .offset {
                val bottom_padding: Int = getNpBottomPadding(system_insets, navigation_insets, keyboard_insets)
                val swipe_offset: Dp =
                    if (player_showing) -np_swipe_state.value.offset.value.dp - ((screen_size.height + np_bottom_bar_height) * 0.5f)
                    else -np_bottom_bar_height

                IntOffset(
                    0,
                    swipe_offset.roundToPx() - bottom_padding - additional_offset.roundToPx() + 1 // Avoid single-pixel gap
                )
            }
            .padding(start = system_insets.getStart(), end = system_insets.getEnd())
            .onSizeChanged {
                with (density) {
                    offset_items[index] = TopOffsetItem(
                        height = it.height.toDp(),
                        apply_spacing = apply_spacing,
                        displaying = displaying
                    )
                }
            }
    }

    @Composable
    fun nowPlayingBottomPadding(include_np: Boolean = false, include_top_items: Boolean = include_np): Dp {
        var bottom_padding: Dp =
            with(LocalDensity.current) {
                LocalDensity.current.getNpBottomPadding(WindowInsets.systemBars, WindowInsets.navigationBars, WindowInsets.ime).toDp()
            }

        if (include_np) {
            bottom_padding += animateDpAsState(
                (
                    if (np_bottom_bar_config?.show_when_expanded == true) np_bottom_bar_height
                    else 0.dp
                )
                + (
                    if (player_showing) MINIMISED_NOW_PLAYING_HEIGHT_DP.dp
                    else 0.dp
                )
            ).value
        }
        if (include_top_items) {
            bottom_padding += animateDpAsState(
                getTopItemsHeight()
            ).value
        }

        return bottom_padding
    }

    fun onNavigationBarTargetColourChanged(colour: Color?, from_lpm: Boolean) {
        if (!from_lpm && long_press_menu_showing) {
            return
        }

        context.setNavigationBarColour(
            colour ?: if (from_lpm) getNPBackground() else null
        )
    }

    fun openAppPage(page: AppPage?, from_current: Boolean = false, replace_current: Boolean = false) {
        if (page == app_page) {
            page.onReopened()
            return
        }

        if (!replace_current) {
            app_page_undo_stack.add(app_page)
        }
        app_page_state.setPage(page, from_current = from_current, going_back = false)

        if (np_swipe_state.value.targetValue != 0) {
            switchNowPlayingPage(0)
        }
        hideLongPressMenu()
    }

    fun navigateBack() {
        if (app_page.onBackNavigation()) {
            return
        }
        app_page_state.setPage(app_page_undo_stack.removeLastOrNull(), from_current = false, going_back = true)
    }

    fun openMediaItem(
        item: MediaItem,
        from_current: Boolean = false,
        replace_current: Boolean = false,
        browse_params: BrowseParamsData? = null
    ) {
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

    fun openViewMorePage(browse_id: String, title: String?) {
        openAppPage(app_page_state.getViewMorePage(browse_id, title))
    }

    fun openNowPlayingPlayerOverlayMenu(menu: PlayerOverlayMenu? = null) {
        np_overlay_menu.value = menu
        expansion.scrollTo(1)
    }

    fun onPlayActionOccurred() {
        if (np_swipe_state.value.targetValue == 0 && Settings.get(BehaviourSettings.Key.OPEN_NP_ON_SONG_PLAYED)) {
            switchNowPlayingPage(1)
        }
    }

    fun playMediaItem(item: MediaItem, shuffle: Boolean = false, at_index: Int = 0) {
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

    fun playPlaylist(playlist: Playlist, from_index: Int = 0) {
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

    fun showLongPressMenu(item: MediaItem) { showLongPressMenu(LongPressMenuData(item)) }
    fun showLongPressMenu(data: LongPressMenuData) {
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
        long_press_menu_showing = false
        long_press_menu_direct = false
        long_press_menu_data = null
    }

    @Composable
    fun NowPlaying() {
        val player: PlayerState = LocalPlayerState.current
        val density: Density = LocalDensity.current
        val bottom_padding: Int = density.getNpBottomPadding(WindowInsets.systemBars, WindowInsets.navigationBars, WindowInsets.ime)

        val vertical_page_count: Int = getNowPlayingVerticalPageCount(this)
        val minimised_now_playing_height: Dp = MINIMISED_NOW_PLAYING_HEIGHT_DP.dp

        val bottom_layout_slot: LayoutSlot =
            when (form_factor) {
                FormFactor.LANDSCAPE -> LandscapeLayoutSlot.BELOW_PLAYER
                FormFactor.PORTRAIT -> PortraitLayoutSlot.BELOW_PLAYER
            }

        np_bottom_bar_config = bottom_layout_slot.observeConfig { LayoutSlot.BelowPlayerConfig() }
        val show_bottom_slot_when_expanded: Boolean =
            np_bottom_bar_config?.show_when_expanded == true || bottom_layout_slot.mustShow()

        val player_height: Dp = screen_size.height

        LaunchedEffect(player_height, bottom_padding, vertical_page_count, np_bottom_bar_height) {
            val half_screen_height: Float = player_height.value * 0.5f

            with(density) {
                np_swipe_anchors = (0..vertical_page_count)
                    .associateBy { anchor ->
                        if (anchor == 0) minimised_now_playing_height.value - half_screen_height + (np_bottom_bar_height.value / 2)
                        else (
                            ((player_height - bottom_padding.toDp()).value * anchor)
                            - half_screen_height
                            -np_bottom_bar_height.value / 2
                        )
                    }
            }

            val current_swipe_value = np_swipe_state.value.targetValue
            np_swipe_state.value = SwipeableState(0).apply {
                init(mapOf(-half_screen_height to 0))
                snapTo(current_swipe_value)
            }
        }

        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
            if (np_swipe_anchors != null) {
                val expanded_bottom_bar_height: Dp =
                    if (show_bottom_slot_when_expanded) np_bottom_bar_height
                    else 0.dp

                val page_height: Dp = (
                    player_height
                    - expanded_bottom_bar_height
                    - nowPlayingBottomPadding()
                    - WindowInsets.getTop()
                )

                com.toasterofbread.spmp.ui.layout.nowplaying.NowPlaying(
                    page_height,
                    np_swipe_state.value,
                    np_swipe_anchors!!,
                    PaddingValues(
                        start = WindowInsets.getStart(),
                        end = WindowInsets.getEnd(),
                        bottom = np_bottom_bar_height
                    ),
                    Modifier.weight(1f).offset(y = (page_height) / 2)
                )
            }

            bottom_layout_slot.DisplayBar(
                0.dp,
                Modifier
                    .fillMaxWidth()
                    .onSizeChanged {
                        np_bottom_bar_height = with (density) {
                            it.height.toDp()
                        }
                    }
                    .offset {
                        val slot_expansion: Float =
                            if (show_bottom_slot_when_expanded) 1f
                            else (1f - player.expansion.getBounded()).coerceAtLeast(0f)

                        IntOffset(
                            x = 0,
                            y = with (density) {
                                (np_bottom_bar_height.toPx() * (1f - slot_expansion)).roundToInt()
                            }
                        )
                    }
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
                                .then(nowPlayingTopOffset(Modifier, NowPlayingTopOffsetSection.MULTISELECT))
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

    val controller: PlatformPlayerService? get() = _player
    fun withPlayer(action: PlayerServicePlayer.() -> Unit) {
        _player?.also {
            action(it.service_player)
            return
        }

        connectService {
            action(it.service_player)
        }
    }

    @Composable
    fun withPlayerComposable(action: @Composable PlayerServicePlayer.() -> Unit) {
        connectService(null)
        _player?.service_player?.also {
            action(it)
        }
    }

    val service_connected: Boolean get() = _player?.load_state?.loading == false
    val service_loading_message: String? get() = _player?.load_state?.takeIf { it.loading }?.loading_message
    val service_connection_error: Throwable? get() = _player?.connection_error

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

    val status: PlayerStatus = PlayerStatus()

    fun isRunningAndFocused(): Boolean {
        return controller?.has_focus == true
    }

    fun onSongDownloadRequested(song: Song, always_show_options: Boolean = false, onCompleted: DownloadRequestCallback? = null) {
        onSongDownloadRequested(listOf(song), always_show_options, onCompleted)
    }
    fun onSongDownloadRequested(songs: List<Song>, always_show_options: Boolean = false, callback: DownloadRequestCallback? = null) {
        download_request_songs = songs
        download_request_always_show_options = always_show_options
        download_request_callback = callback
    }
}
