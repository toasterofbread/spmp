package com.toasterofbread.spmp.ui.layout.library

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.vector.ImageVector
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.platform.PlatformContext
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.layout.artistpage.ArtistPage

class LibraryProfilePage(context: PlatformContext): LibrarySubPage(context) {
    override fun getIcon(): ImageVector =
        Icons.Default.Person

    override fun isHidden(): Boolean = own_channel == null
    override fun enableSearch(): Boolean = false
    override fun enableSorting(): Boolean = false

    private val own_channel: Artist? get() = context.ytapi.user_auth_state?.own_channel

    @Composable
    override fun Page(
        library_page: LibraryPage,
        content_padding: PaddingValues,
        multiselect_context: MediaItemMultiSelectContext,
        modifier: Modifier
    ) {
        val channel = own_channel ?: return
        ArtistPage(
            channel,
            modifier.clipToBounds(),
            content_padding = content_padding,
            multiselect_context = multiselect_context,
            show_top_bar = false
        )
    }
}