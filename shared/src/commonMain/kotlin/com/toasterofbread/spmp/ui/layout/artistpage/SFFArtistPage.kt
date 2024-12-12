package com.toasterofbread.spmp.ui.layout.artistpage

import LocalPlayerState
import SpMp.isDebugBuild
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.*
import androidx.compose.ui.unit.*
import dev.toastbits.composekit.components.utils.composable.*
import dev.toastbits.composekit.components.utils.modifier.horizontal
import com.toasterofbread.spmp.model.*
import com.toasterofbread.spmp.model.mediaitem.*
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistLayout
import com.toasterofbread.spmp.model.mediaitem.layout.Layout
import com.toasterofbread.spmp.model.mediaitem.layout.AppMediaItemLayout
import com.toasterofbread.spmp.model.mediaitem.loader.MediaItemLoader
import com.toasterofbread.spmp.model.mediaitem.playlist.Playlist
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.MediaItemLayoutParams
import com.toasterofbread.spmp.model.MediaItemListParams
import com.toasterofbread.spmp.service.playercontroller.LocalPlayerClickOverrides
import com.toasterofbread.spmp.service.playercontroller.PlayerClickOverrides
import com.toasterofbread.spmp.ui.component.ErrorInfoDisplay
import com.toasterofbread.spmp.ui.component.longpressmenu.LongPressMenuData
import com.toasterofbread.spmp.ui.component.mediaitemlayout.MediaItemList
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import dev.toastbits.composekit.util.composable.copy
import dev.toastbits.ytmkt.endpoint.ArtistWithParamsRow
import dev.toastbits.ytmkt.model.external.ItemLayoutType
import dev.toastbits.ytmkt.uistrings.RawUiString
import dev.toastbits.ytmkt.uistrings.YoutubeUILocalisation
import dev.toastbits.ytmkt.uistrings.YoutubeUiString
import kotlinx.coroutines.*

@Composable
internal fun ArtistAppPage.SFFArtistPage(
    artist: Artist,
    modifier: Modifier = Modifier,
    content_padding: PaddingValues = PaddingValues(),
    multiselect_context: MediaItemMultiSelectContext? = null
) {
    val player: PlayerState = LocalPlayerState.current
    val click_overrides: PlayerClickOverrides = LocalPlayerClickOverrides.current

    val own_multiselect_context = remember(multiselect_context) { if (multiselect_context != null) null else MediaItemMultiSelectContext(player.context) {} }
    val apply_filter: Boolean by player.settings.Filter.APPLY_TO_ARTIST_ITEMS.observe()

    val item_layouts: List<ArtistLayout>? by artist.Layouts.observe(player.database)
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
        previous_item?.item,
        content_padding,
        multiselect_context ?: own_multiselect_context,
        loading = refreshed && loading,
        onReload = {
            refreshed = true
            load_error = null
            coroutine_scope.launch {
                MediaItemLoader.loadArtist(artist.getEmptyData(), player.context)
            }
        },
        getAllSelectableItems = { artistPageGetAllItems(player, browse_params_rows, item_layouts) }
    ) { accent_colour, content_modifier ->
        if (load_error != null) {
            item {
                load_error?.also { error ->
                    ErrorInfoDisplay(
                        error,
                        isDebugBuild(),
                        content_modifier.padding(content_padding),
                        onDismiss = null
                    )
                }
            }
        }
        else if (loading || (browse_params != null && browse_params_rows == null) || (browse_params == null && item_layouts == null)) {
            item {
                Box(content_modifier, contentAlignment = Alignment.Center) {
                    SubtleLoadingIndicator()
                }
            }
        }
        else if (browse_params != null) {
            val row: ArtistWithParamsRow? = browse_params_rows?.firstOrNull()
            if (row != null) {
                item {
                    val items: List<MediaItem> = remember(row) {
                        row.items.map { it.toMediaItemRef() }
                    }

                    MediaItemList(
                        MediaItemLayoutParams(
                            items = items,
                            modifier = content_modifier.padding(content_padding.copy(top = 0.dp)),
                            title = row.title?.let { title ->
                                RawUiString(title)
                            },
                            multiselect_context = multiselect_context ?: own_multiselect_context
                        ),
                        list_params = MediaItemListParams(
                            play_as_list = true
                        )
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
                        val layout: AppMediaItemLayout = artist_layout.rememberMediaItemLayout(player.database).layout
                        val layout_id: YoutubeUILocalisation.StringID? = (layout.title as? YoutubeUiString)?.getYoutubeStringId()

                        val is_singles: Boolean =
                            player.settings.Behaviour.TREAT_SINGLES_AS_SONG.observe().value
                            && layout_id == YoutubeUILocalisation.StringID.ARTIST_ROW_SINGLES

                        val is_artist_row: Boolean =
                            layout_id == YoutubeUILocalisation.StringID.ARTIST_ROW_SINGLES
                            || layout_id == YoutubeUILocalisation.StringID.ARTIST_ROW_OTHER

                        CompositionLocalProvider(
                            LocalPlayerClickOverrides provides click_overrides.copy(
                                onClickOverride = { item, multiselect_key ->
                                    if (is_singles && item is Playlist) {
                                        onSinglePlaylistClicked(item, player, click_overrides)
                                    }
                                    else if (item !is Song) {
                                        player.openMediaItem(item, is_artist_row)
                                    }
                                    else {
                                        player.playMediaItem(item)
                                    }
                                },
                                onAltClickOverride = { item, long_press_data ->
                                    click_overrides.onMediaItemAltClicked(
                                        item,
                                        player,
                                        long_press_data =
                                            if (is_singles && item is Playlist)
                                                long_press_data?.copy(playlist_as_song = true)
                                                    ?: LongPressMenuData(item, playlist_as_song = true)
                                            else long_press_data
                                    )
                                }
                            )
                        ) {
                            val type: ItemLayoutType = layout.type.let { type ->
                                if (type == null) ItemLayoutType.GRID
                                else if (type == ItemLayoutType.NUMBERED_LIST) ItemLayoutType.LIST
                                else type
                            }

                            type.Layout(
                                if (previous_item == null) layout else layout.copy(title = null, subtitle = null),
                                MediaItemLayoutParams(
                                    multiselect_context = multiselect_context ?: own_multiselect_context,
                                    apply_filter = apply_filter
                                )
                            )
                        }
                    }

                    val artist_description: String? by artist.Description.observe(player.database)
                    artist_description?.also { description ->
                        if (description.isNotBlank()) {
                            DescriptionCard(description)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(DelicateCoroutinesApi::class)
private fun onSinglePlaylistClicked(playlist: Playlist, player: PlayerState, click_overrides: PlayerClickOverrides) {
    GlobalScope.launch {
        playlist.loadData(player.context).onSuccess { data ->
            data.items?.firstOrNull()?.also { first_item ->
                withContext(Dispatchers.Main) {
                    click_overrides.onMediaItemClicked(first_item, player)
                }
            }
        }
    }
}
