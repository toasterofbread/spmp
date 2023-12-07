package com.toasterofbread.spmp.ui.layout.artistpage

import LocalPlayerState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.layout.BrowseParamsData
import com.toasterofbread.spmp.platform.getFormFactor
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.layout.artistpage.lff.LFFArtistPage
import com.toasterofbread.spmp.youtubeapi.endpoint.ArtistWithParamsEndpoint

@Composable
fun ArtistPage(
    artist: Artist,
    modifier: Modifier = Modifier,
    previous_item: MediaItem? = null,
    content_padding: PaddingValues = PaddingValues(),
    browse_params: Pair<BrowseParamsData, ArtistWithParamsEndpoint>? = null,
    multiselect_context: MediaItemMultiSelectContext? = null,
    show_top_bar: Boolean = true
) {
    if (LocalPlayerState.current.getFormFactor(0.75f).is_large) {
        LFFArtistPage(artist, modifier, previous_item, content_padding, browse_params, multiselect_context)
    }
    else {
        SFFArtistPage(artist, modifier, previous_item, content_padding, browse_params, multiselect_context, show_top_bar)
    }
}
