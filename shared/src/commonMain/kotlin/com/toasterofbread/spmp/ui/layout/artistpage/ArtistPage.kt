package com.toasterofbread.spmp.ui.layout.artistpage

import LocalPlayerState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.*
import androidx.compose.ui.unit.*
import com.toasterofbread.spmp.model.*
import com.toasterofbread.spmp.model.mediaitem.*
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistLayout
import com.toasterofbread.spmp.model.mediaitem.layout.BrowseParamsData
import com.toasterofbread.spmp.model.mediaitem.layout.MediaItemLayout
import com.toasterofbread.spmp.model.mediaitem.loader.MediaItemLoader
import com.toasterofbread.spmp.model.mediaitem.loader.loadDataOnChange
import com.toasterofbread.spmp.model.mediaitem.playlist.Playlist
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.resources.uilocalisation.LocalisedYoutubeString
import com.toasterofbread.spmp.resources.uilocalisation.YoutubeUILocalisation
import com.toasterofbread.spmp.ui.component.ErrorInfoDisplay
import com.toasterofbread.spmp.ui.component.longpressmenu.LongPressMenuData
import com.toasterofbread.spmp.ui.component.mediaitemlayout.MediaItemList
import com.toasterofbread.spmp.ui.component.mediaitempreview.MediaItemPreviewLong
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.PlayerState
import com.toasterofbread.spmp.ui.theme.Theme
import com.toasterofbread.spmp.youtubeapi.endpoint.ArtistWithParamsEndpoint
import com.toasterofbread.spmp.youtubeapi.endpoint.ArtistWithParamsRow
import com.toasterofbread.utils.*
import com.toasterofbread.utils.composable.*
import com.toasterofbread.utils.modifier.horizontal
import kotlinx.coroutines.*

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
    val player = LocalPlayerState.current
    val coroutine_scope = rememberCoroutineScope()

    val own_multiselect_context = remember(multiselect_context) { if (multiselect_context != null) null else MediaItemMultiSelectContext() {} }
    val apply_filter: Boolean by Settings.KEY_FILTER_APPLY_TO_ARTIST_ITEMS.rememberMutableState()

    var load_error: Throwable? by remember { mutableStateOf(null) }
    val loading by artist.loadDataOnChange(player.context, load = browse_params == null) { load_error = it }
    var refreshed by remember { mutableStateOf(false) }

    val item_layouts: List<ArtistLayout>? by artist.Layouts.observe(player.database)
    val single_layout: MediaItemLayout? = item_layouts?.singleOrNull()?.rememberMediaItemLayout(player.database)
    var browse_params_rows: List<ArtistWithParamsRow>? by remember { mutableStateOf(null) }

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

    ArtistLayout(
        artist,
        modifier,
        previous_item,
        content_padding,
        multiselect_context ?: own_multiselect_context,
        show_top_bar = show_top_bar,
        loading = refreshed && loading,
        onReload = {
            refreshed = true
            load_error = null
            coroutine_scope.launch {
                MediaItemLoader.loadArtist(artist.getEmptyData(), player.context)
            }
        }
    ) { accent_colour, show_info, content_modifier ->
        if (load_error != null) {
            item {
                load_error?.also { error ->
                    ErrorInfoDisplay(error, content_modifier)
                }
            }
        }
        else if (loading) {
            item {
                Box(content_modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    SubtleLoadingIndicator()
                }
            }
        }
        else if ((browse_params != null && browse_params_rows == null) || (browse_params == null && item_layouts == null)) {
            item {
                Box(content_modifier.fillMaxSize().padding(content_padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = accent_colour ?: Color.Unspecified)
                }
            }
        }
        else if (browse_params != null) {
            items(browse_params_rows.orEmpty()) { row ->
                MediaItemList(
                    row.items,
                    content_modifier.padding(content_padding),
                    title = row.title?.let { title ->
                        LocalisedYoutubeString.Type.RAW.create(title)
                    }
                )
            }
        }
        else if (single_layout != null) {
            item {
                single_layout.TitleBar(content_modifier.padding(content_padding).padding(bottom = 5.dp))
            }

            items(single_layout.items) { item ->
                Row(content_modifier.padding(content_padding), verticalAlignment = Alignment.CenterVertically) {
                    MediaItemPreviewLong(
                        item,
                        multiselect_context = multiselect_context ?: own_multiselect_context
                    )
                }
            }
        }
        else {
            item {
                Column(
                    content_modifier
                        .fillMaxSize()
                        .padding(content_padding.horizontal),
                    verticalArrangement = Arrangement.spacedBy(30.dp)
                ) {
                    for (artist_layout in item_layouts ?: emptyList()) {
                        val layout = artist_layout.rememberMediaItemLayout(player.database)
                        val layout_id = layout.title?.getID()

                        val is_singles =
                            Settings.KEY_TREAT_SINGLES_AS_SONG.get()
                                    && layout_id == YoutubeUILocalisation.StringID.ARTIST_ROW_SINGLES

                        val is_artist_row: Boolean =
                            layout_id == YoutubeUILocalisation.StringID.ARTIST_ROW_SINGLES || layout_id == YoutubeUILocalisation.StringID.ARTIST_ROW_OTHER

                        CompositionLocalProvider(LocalPlayerState provides remember {
                            player.copy(
                                onClickedOverride = { item, multiselect_key ->
                                    if (is_singles && item is Playlist) {
                                        onSinglePlaylistClicked(item, player)
                                    }
                                    else if (item !is Song) {
                                        player.openMediaItem(item, is_artist_row)
                                    }
                                    else {
                                        player.playMediaItem(item)
                                    }
                                },
                                onLongClickedOverride = { item, long_press_data ->
                                    player.onMediaItemLongClicked(
                                        item,
                                        if (is_singles && item is Playlist)
                                            long_press_data?.copy(playlist_as_song = true)
                                                ?: LongPressMenuData(item, playlist_as_song = true)
                                        else long_press_data
                                    )
                                }
                            )
                        }) {
                            val type =
                                if (layout.type == null) MediaItemLayout.Type.GRID
                                else if (layout.type == MediaItemLayout.Type.NUMBERED_LIST && artist is Artist) MediaItemLayout.Type.LIST
                                else layout.type

                            type.Layout(
                                if (previous_item == null) layout else layout.copy(title = null, subtitle = null),
                                multiselect_context = multiselect_context ?: own_multiselect_context,
                                apply_filter = apply_filter
                            )
                        }
                    }

                    val artist_description: String? by artist.Description.observe(player.database)
                    artist_description?.also { description ->
                        if (description.isNotBlank()) {
                            DescriptionCard(description, { Theme.background }, { accent_colour }) { show_info.value = !show_info.value }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(DelicateCoroutinesApi::class)
private fun onSinglePlaylistClicked(playlist: Playlist, player: PlayerState) {
    GlobalScope.launch {
        playlist.loadData(player.context).onSuccess { data ->
            data.items?.firstOrNull()?.also { first_item ->
                withContext(Dispatchers.Main) {
                    player.onMediaItemClicked(first_item)
                }
            }
        }
    }
}
