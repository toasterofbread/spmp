package com.toasterofbread.spmp.ui.layout.artistpage.lff

import LocalPlayerState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import dev.toastbits.composekit.util.getThemeColour
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistLayout
import com.toasterofbread.spmp.model.mediaitem.loader.MediaItemThumbnailLoader
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.artistpage.ArtistAppPage
import dev.toastbits.composekit.theme.core.makeVibrant
import dev.toastbits.composekit.theme.core.vibrantAccent
import dev.toastbits.ytmkt.endpoint.ArtistWithParamsRow

@Composable
internal fun ArtistAppPage.LFFArtistPage(
    artist: Artist,
    modifier: Modifier = Modifier,
    content_padding: PaddingValues = PaddingValues(),
    multiselect_context: MediaItemMultiSelectContext? = null
) {
    val player: PlayerState = LocalPlayerState.current

    val own_multiselect_context: MediaItemMultiSelectContext? = remember(multiselect_context) { if (multiselect_context != null) null else MediaItemMultiSelectContext(player.context) {} }
    val apply_filter: Boolean by player.settings.Filter.APPLY_TO_ARTIST_ITEMS.observe()

    val item_layouts: List<ArtistLayout>? by artist.Layouts.observe(player.database)
    var browse_params_rows: List<ArtistWithParamsRow>? by remember { mutableStateOf(null) }

    var accent_colour: Color? by remember { mutableStateOf(null) }
    val current_accent_colour: Color = accent_colour ?: player.theme.vibrantAccent

    val thumbnail_load_state: MediaItemThumbnailLoader.ItemState = MediaItemThumbnailLoader.rememberItemState(artist)
    val thumbnail: ImageBitmap? = thumbnail_load_state.getHighestQuality()
    LaunchedEffect(thumbnail) {
        accent_colour = thumbnail?.getThemeColour()?.let {
            player.theme.makeVibrant(it)
        }
    }

    LaunchedEffect(artist.id, browse_params) {
        assert(!artist.isForItem()) { artist.toString() }

        browse_params_rows = null

        if (browse_params == null) {
            return@LaunchedEffect
        }

        load_error = null

        val (params, params_endpoint) = browse_params

        params_endpoint.loadArtistWithParams(params).fold(
            { browse_params_rows = it },
            { load_error = it }
        )
    }

    Row(modifier, horizontalArrangement = Arrangement.spacedBy(25.dp)) {
        LFFArtistStartPane(
            Modifier
                .widthIn(max = 1500.dp)
                .fillMaxWidth(0.3f),
            artist,
            multiselect_context ?: own_multiselect_context,
            content_padding,
            current_accent_colour,
            item_layouts,
            apply_filter
        )

        LFFArtistEndPane(
            multiselect_context ?: own_multiselect_context!!,
            content_padding,
            browse_params_rows,
            current_accent_colour,
            item_layouts,
            apply_filter
        )
    }
}
