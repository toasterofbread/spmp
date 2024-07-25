package com.toasterofbread.spmp.model.state

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.playlist.Playlist
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.platform.AppThemeManager
import com.toasterofbread.spmp.platform.FormFactor
import com.toasterofbread.spmp.platform.download.DownloadStatus
import com.toasterofbread.spmp.ui.component.longpressmenu.LongPressMenuData
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.component.multiselect.MultiSelectItem
import com.toasterofbread.spmp.ui.layout.BarColourState
import com.toasterofbread.spmp.ui.layout.apppage.AppPage
import com.toasterofbread.spmp.ui.layout.apppage.AppPageState
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingTopOffsetSection
import com.toasterofbread.spmp.ui.layout.nowplaying.PlayerExpansionState
import com.toasterofbread.spmp.ui.layout.nowplaying.overlay.PlayerOverlayMenu
import dev.toastbits.ytmkt.model.external.YoutubePage

interface UiState {
    val theme: AppThemeManager

    val player_state: PlayerState
    val player_expansion: PlayerExpansionState

    val app_page: AppPage
    val app_page_state: AppPageState

    val form_factor: FormFactor
    val bar_colour_state: BarColourState
    val screen_size: DpSize
    val main_multiselect_context: MediaItemMultiSelectContext

    val current_player_page: Int
    fun switchPlayerPage(page: Int)

    fun addMultiselectInfoAllItemsGetter(getter: () -> List<List<MultiSelectItem>>)
    fun removeMultiselectInfoAllItemsGetter(getter: () -> List<List<MultiSelectItem>>)

    fun openAppPage(page: AppPage?, from_current: Boolean = false, replace_current: Boolean = false)
    fun navigateBack()
    fun clearBackHistory()
    fun openMediaItem(
        item: MediaItem,
        from_current: Boolean = false,
        replace_current: Boolean = false,
        browse_params: YoutubePage.BrowseParamsData? = null
    )

    fun openViewMorePage(browse_id: String, title: String?)
    fun onPlayActionOccurred()
    fun showLongPressMenu(item: MediaItem)
    fun showLongPressMenu(data: LongPressMenuData)
    fun hideLongPressMenu()

    fun onSongDownloadRequested(
        songs: List<Song>,
        always_show_options: Boolean = false,
        callback: DownloadRequestCallback? = null
    )

    @Composable
    fun PersistentContent()

    @Composable
    fun HomePage()
}

typealias DownloadRequestCallback = (DownloadStatus?) -> Unit
