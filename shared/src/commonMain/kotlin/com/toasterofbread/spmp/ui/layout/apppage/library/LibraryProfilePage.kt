package com.toasterofbread.spmp.ui.layout.apppage.library

import LocalPlayerState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.vector.ImageVector
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistRef
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.layout.apppage.AppPage
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.artistpage.ArtistAppPage

class LibraryProfilePage(context: AppContext): LibrarySubPage(context) {
    override fun getIcon(): ImageVector =
        Icons.Default.Person

    override fun isHidden(): Boolean = own_channel == null
    override fun enableSearching(): Boolean = false
    override fun enableSorting(): Boolean = false

    private val own_channel: Artist? get() = context.ytapi.user_auth_state?.own_channel_id?.let { ArtistRef(it) }

    @Composable
    override fun Page(
        library_page: LibraryAppPage,
        content_padding: PaddingValues,
        multiselect_context: MediaItemMultiSelectContext,
        showing_alt_content: Boolean,
        modifier: Modifier
    ) {
        val channel: Artist = own_channel ?: return
        val player: PlayerState = LocalPlayerState.current

        val page: AppPage = remember {
            ArtistAppPage(player.app_page_state, channel)
        }

        Column {
            with (page) {
                Page(multiselect_context, modifier.clipToBounds(), content_padding) {}
            }
        }
    }
}
