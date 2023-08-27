package com.toasterofbread.spmp.ui.layout.library

import LocalPlayerState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.model.mediaitem.playlist.rememberLocalPlaylists
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.utils.composable.EmptyListCrossfade

class LibraryPlaylistsPage: LibrarySubPage {
    override fun getIcon(): ImageVector =
        MediaItemType.PLAYLIST_ACC.getIcon()

    override fun getTitle(): String =
        getString("library_tab_playlists")

    @Composable
    override fun Page(
        library_page: LibraryPage,
        content_padding: PaddingValues,
        multiselect_context: MediaItemMultiSelectContext,
        modifier: Modifier,
    ) {
        val player = LocalPlayerState.current

        val local_playlists = rememberLocalPlaylists(player.context)
        val account_playlists =

        EmptyListCrossfade(local_playlists) {

        }
    }
}
