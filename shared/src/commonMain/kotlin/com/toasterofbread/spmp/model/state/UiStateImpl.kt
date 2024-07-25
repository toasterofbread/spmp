package com.toasterofbread.spmp.model.state

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.playlist.Playlist
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.AppThemeManager
import com.toasterofbread.spmp.platform.FormFactor
import com.toasterofbread.spmp.platform.download.DownloadMethodSelectionDialog
import com.toasterofbread.spmp.ui.component.longpressmenu.LongPressMenu
import com.toasterofbread.spmp.ui.component.longpressmenu.LongPressMenuData
import com.toasterofbread.spmp.ui.component.multiselect.AppPageMultiSelectContext
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.component.multiselect.MultiSelectInfoDisplayContent
import com.toasterofbread.spmp.ui.component.multiselect.MultiSelectItem
import com.toasterofbread.spmp.ui.layout.BarColourState
import com.toasterofbread.spmp.ui.layout.apppage.AppPage
import com.toasterofbread.spmp.ui.layout.apppage.AppPageState
import com.toasterofbread.spmp.ui.layout.apppage.AppPageWithItem
import com.toasterofbread.spmp.ui.layout.apppage.SongAppPage
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.MainPageDisplay
import com.toasterofbread.spmp.ui.layout.artistpage.ArtistAppPage
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingTopOffsetSection
import com.toasterofbread.spmp.ui.layout.nowplaying.PlayerExpansionState
import com.toasterofbread.spmp.ui.layout.playlistpage.PlaylistAppPage
import dev.toastbits.composekit.platform.composable.BackHandler
import dev.toastbits.composekit.settings.ui.on_accent
import dev.toastbits.ytmkt.model.external.YoutubePage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class UiStateImpl(
    private val context: AppContext,
    private val coroutine_scope: CoroutineScope,
    // TODO | This can probably be removed
    override val player_state: PlayerState,
    private val np_swipe_state: AnchoredDraggableState<Int> = PlayerStateImpl.createSwipeState(),
    screen_size_state: State<DpSize>
): UiState {
    override val theme: AppThemeManager get() = context.theme

    override val player_expansion: PlayerExpansionState =
        object : PlayerExpansionState(this) {
            override val swipe_state: AnchoredDraggableState<Int> = np_swipe_state
        }

    override val app_page: AppPage get() = app_page_state.current_page
    override val app_page_state: AppPageState = AppPageState(context)

    override val bar_colour_state: BarColourState =
        object : BarColourState() {
            fun getDefaultColour(): Color = theme.background

            override fun onCurrentStatusBarColourChanged(colour: Color?) {
                context.setStatusBarColour(colour ?: getDefaultColour())
            }

            override fun onCurrentNavigationBarColourChanged(colour: Color?) {
                context.setNavigationBarColour(colour ?: getDefaultColour())
            }
        }

    override val screen_size: DpSize by screen_size_state

    override val form_factor: FormFactor by derivedStateOf { FormFactor.getCurrent(this) }
    override val main_multiselect_context: MediaItemMultiSelectContext = AppPageMultiSelectContext(this, context)

    override val current_player_page: Int
        get() = np_swipe_state.targetValue

    override fun switchPlayerPage(page: Int) {
        coroutine_scope.launch {
            np_swipe_state.animateTo(page)
        }
    }

    private var download_request_songs: List<Song>? by mutableStateOf(null)
    private var download_request_always_show_options: Boolean by mutableStateOf(false)
    private var download_request_callback: DownloadRequestCallback? by mutableStateOf(null)
    private var open_np_on_song_played: Boolean = false

    init {
        coroutine_scope.launch {
            open_np_on_song_played = context.settings.behaviour.OPEN_NP_ON_SONG_PLAYED.get()
        }
    }

    override fun addMultiselectInfoAllItemsGetter(getter: () -> List<List<MultiSelectItem>>) {
        multiselect_info_all_items_getters.add(getter)
    }
    override fun removeMultiselectInfoAllItemsGetter(getter: () -> List<List<MultiSelectItem>>) {
        multiselect_info_all_items_getters.remove(getter)
    }

    override fun openAppPage(page: AppPage?, from_current: Boolean, replace_current: Boolean) {
        if (current_player_page != 0) {
            switchPlayerPage(0)
        }

        if (page == app_page) {
            page.onReopened()
            return
        }

        if (!replace_current) {
            app_page_undo_stack.add(app_page)
        }
        app_page_state.setPage(page, from_current = from_current, going_back = false)
        hideLongPressMenu()
    }

    override fun navigateBack() {
        if (app_page.onBackNavigation()) {
            return
        }
        app_page_state.setPage(app_page_undo_stack.removeLastOrNull(), from_current = false, going_back = true)
    }

    override fun clearBackHistory() {
        app_page_undo_stack.clear()
    }

    override fun openMediaItem(
        item: MediaItem,
        from_current: Boolean,
        replace_current: Boolean,
        browse_params: YoutubePage.BrowseParamsData?
    ) {
        if (item is Artist && item.isForItem()) {
            return
        }

        val page: AppPageWithItem =
            when (item) {
                is Song ->
                    SongAppPage(app_page_state, item, browse_params)
                is Artist ->
                    ArtistAppPage(
                        app_page_state,
                        item,
                        browse_params = browse_params?.let { params ->
                            Pair(params, context.ytapi.ArtistWithParams)
                        }
                    )
                is Playlist ->
                    PlaylistAppPage(
                        app_page_state,
                        item
                    )
                else -> throw NotImplementedError(item::class.toString())
            }

        openAppPage(page, from_current, replace_current)
    }

    override fun openViewMorePage(browse_id: String, title: String?) {
        openAppPage(app_page_state.getViewMorePage(browse_id, title))
    }

    override fun onPlayActionOccurred() {
        if (current_player_page == 0 && open_np_on_song_played) {
            switchPlayerPage(1)
        }
    }

    override fun showLongPressMenu(item: MediaItem) { showLongPressMenu(LongPressMenuData(item)) }
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

    override fun onSongDownloadRequested(songs: List<Song>, always_show_options: Boolean, callback: DownloadRequestCallback?) {
        download_request_songs = songs
        download_request_always_show_options = always_show_options
        download_request_callback = callback
    }

    private var multiselect_info_display_height: Dp by mutableStateOf(0.dp)

    @Composable
    override fun PersistentContent() {
        val form_factor: FormFactor by FormFactor.observe()

        bar_colour_state.Update()

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
                    coroutine_scope.launch {
                        method.execute(context, songs, download_request_callback)
                    }
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
                                .then(
                                    player_state.topOffset(
                                        Modifier,
                                        NowPlayingTopOffsetSection.MULTISELECT,
                                        apply_spacing = true,
                                        displaying = true
                                    )
                                )
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
    override fun HomePage() {
        BackHandler(app_page_undo_stack.isNotEmpty()) {
            navigateBack()
        }

        val form_factor: FormFactor by FormFactor.observe()

        CompositionLocalProvider(LocalContentColor provides context.theme.on_background) {
            val bottom_padding: Dp by animateDpAsState(
                if (form_factor.is_large && main_multiselect_context.is_active) multiselect_info_display_height
                else 0.dp
            )

            MainPageDisplay(bottom_padding)
        }
    }

    private val multiselect_info_all_items_getters: MutableList<() -> List<List<MultiSelectItem>>> = mutableListOf()
    private val app_page_undo_stack: MutableList<AppPage?> = mutableStateListOf()
    private var long_press_menu_data: LongPressMenuData? by mutableStateOf(null)
    private var long_press_menu_showing: Boolean by mutableStateOf(false)
    private var long_press_menu_direct: Boolean by mutableStateOf(false)
}
