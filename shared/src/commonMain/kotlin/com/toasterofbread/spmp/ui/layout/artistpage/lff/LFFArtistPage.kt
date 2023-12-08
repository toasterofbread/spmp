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
import com.toasterofbread.composekit.utils.common.getThemeColour
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistLayout
import com.toasterofbread.spmp.model.mediaitem.layout.BrowseParamsData
import com.toasterofbread.spmp.model.mediaitem.loader.MediaItemThumbnailLoader
import com.toasterofbread.spmp.model.mediaitem.loader.loadDataOnChange
import com.toasterofbread.spmp.model.settings.category.FilterSettings
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.PlayerState
import com.toasterofbread.spmp.youtubeapi.endpoint.ArtistWithParamsEndpoint
import com.toasterofbread.spmp.youtubeapi.endpoint.ArtistWithParamsRow

@Composable
fun LFFArtistPage(
    artist: Artist,
    modifier: Modifier = Modifier,
    previous_item: MediaItem? = null,
    content_padding: PaddingValues = PaddingValues(),
    browse_params: Pair<BrowseParamsData, ArtistWithParamsEndpoint>? = null,
    multiselect_context: MediaItemMultiSelectContext? = null
) {
    val player: PlayerState = LocalPlayerState.current

    val own_multiselect_context: MediaItemMultiSelectContext? = remember(multiselect_context) { if (multiselect_context != null) null else MediaItemMultiSelectContext() {} }
    val apply_filter: Boolean by FilterSettings.Key.APPLY_TO_ARTIST_ITEMS.rememberMutableState()

    var load_error: Throwable? by remember { mutableStateOf(null) }
    val loading by artist.loadDataOnChange(player.context, load = browse_params == null) { load_error = it }

    val item_layouts: List<ArtistLayout>? by artist.Layouts.observe(player.database)
    var browse_params_rows: List<ArtistWithParamsRow>? by remember { mutableStateOf(null) }

    var accent_colour: Color? by remember { mutableStateOf(null) }
    val current_accent_colour: Color = accent_colour ?: player.theme.vibrant_accent

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
        require(params_endpoint.isImplemented())

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
            loading,
            item_layouts,
            apply_filter
        )

        LFFArtistEndPane(
            multiselect_context ?: own_multiselect_context,
            content_padding,
            browse_params,
            browse_params_rows,
            current_accent_colour,
            load_error,
            loading,
            item_layouts,
            previous_item,
            apply_filter
        )
    }
}
