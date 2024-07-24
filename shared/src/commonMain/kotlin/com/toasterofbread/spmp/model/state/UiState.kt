package com.toasterofbread.spmp.model.state

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.playlist.Playlist
import com.toasterofbread.spmp.platform.AppThemeManager
import com.toasterofbread.spmp.platform.FormFactor
import com.toasterofbread.spmp.ui.component.longpressmenu.LongPressMenuData
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.component.multiselect.MultiSelectItem
import com.toasterofbread.spmp.ui.layout.BarColourState
import com.toasterofbread.spmp.ui.layout.apppage.AppPage
import com.toasterofbread.spmp.ui.layout.apppage.AppPageState
import com.toasterofbread.spmp.ui.layout.nowplaying.overlay.PlayerOverlayMenu

interface UiState {
    val theme: AppThemeManager

    val app_page: AppPage
    val app_page_state: AppPageState

    val form_factor: FormFactor
    val bar_colour_state: BarColourState
    var screen_size: DpSize
    val main_multiselect_context: MediaItemMultiSelectContext

    fun addMultiselectInfoAllItemsGetter(getter: () -> List<List<MultiSelectItem>>)
    fun removeMultiselectInfoAllItemsGetter(getter: () -> List<List<MultiSelectItem>>)

    fun openAppPage(page: AppPage?, from_current: Boolean = false, replace_current: Boolean = false)
    fun navigateBack()
    fun clearBackHistory()
    fun openMediaItem(
        item: MediaItem,
        from_current: Boolean = false,
        replace_current: Boolean = false,
        browse_params: Any? = null
    )

    fun openViewMorePage(browse_id: String, title: String?)
    fun onPlayActionOccurred()
    fun playMediaItem(item: MediaItem, shuffle: Boolean = false, at_index: Int = 0)
    fun playPlaylist(playlist: Playlist, from_index: Int = 0)
    fun showLongPressMenu(item: MediaItem)
    fun showLongPressMenu(data: LongPressMenuData)
    fun hideLongPressMenu()

    @Composable
    fun PersistentContent()

    @Composable
    fun HomePage()
}
