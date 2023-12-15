package com.toasterofbread.spmp.ui.layout.artistpage.lff

import LocalPlayerState
import SpMp.isDebugBuild
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.toasterofbread.composekit.platform.composable.ScrollBarLazyColumn
import com.toasterofbread.composekit.utils.common.copy
import com.toasterofbread.composekit.utils.composable.SubtleLoadingIndicator
import com.toasterofbread.composekit.utils.modifier.vertical
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistLayout
import com.toasterofbread.spmp.model.mediaitem.layout.MediaItemLayout
import com.toasterofbread.spmp.model.mediaitem.playlist.Playlist
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.settings.category.BehaviourSettings
import com.toasterofbread.spmp.resources.uilocalisation.RawLocalisedString
import com.toasterofbread.spmp.resources.uilocalisation.YoutubeLocalisedString
import com.toasterofbread.spmp.resources.uilocalisation.YoutubeUILocalisation
import com.toasterofbread.spmp.ui.component.ErrorInfoDisplay
import com.toasterofbread.spmp.ui.component.WAVE_BORDER_HEIGHT_DP
import com.toasterofbread.spmp.ui.component.WaveBorder
import com.toasterofbread.spmp.ui.component.longpressmenu.LongPressMenuData
import com.toasterofbread.spmp.ui.component.mediaitemlayout.MediaItemList
import com.toasterofbread.spmp.ui.component.mediaitempreview.MediaItemPreviewLong
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.component.multiselect.MultiSelectItem
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.PlayerState
import com.toasterofbread.spmp.ui.layout.artistpage.ArtistAppPage
import com.toasterofbread.spmp.ui.layout.artistpage.artistPageGetAllItems
import com.toasterofbread.spmp.youtubeapi.endpoint.ArtistWithParamsRow
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun ArtistAppPage.LFFArtistEndPane(
    multiselect_context: MediaItemMultiSelectContext,
    content_padding: PaddingValues,
    browse_params_rows: List<ArtistWithParamsRow>?,
    current_accent_colour: Color,
    item_layouts: List<ArtistLayout>?,
    apply_filter: Boolean
) {
    val player: PlayerState = LocalPlayerState.current
    val end_padding: Dp = content_padding.calculateEndPadding(LocalLayoutDirection.current)
    val single_layout: MediaItemLayout? = item_layouts?.singleOrNull()?.rememberMediaItemLayout(player.database)

    Column {
        val multiselect_showing: Boolean =
            multiselect_context.InfoDisplay(
                Modifier
                    .zIndex(1f)
                    .padding(
                        top = content_padding.calculateTopPadding(),
                        end = end_padding
                    ),
                getAllItems = { artistPageGetAllItems(player, browse_params_rows, item_layouts) },
                wrapContent = {
                    Column {
                        it()
                        WaveBorder(Modifier.fillMaxWidth().zIndex(1f))
                        Spacer(Modifier.height(WAVE_BORDER_HEIGHT_DP.dp))
                    }
                }
            )

        ScrollBarLazyColumn(
            Modifier.fillMaxWidth(),
            contentPadding = content_padding.vertical.copy(top = if (multiselect_showing) 0.dp else null),
            scrollBarColour = current_accent_colour
        ) {
            if (load_error != null) {
                item {
                    load_error.also { error ->
                        ErrorInfoDisplay(
                            error,
                            isDebugBuild(),
                            Modifier.padding(end = end_padding),
                            onDismiss = null
                        )
                    }
                }
            }
            else if (loading || (browse_params != null && browse_params_rows == null) || (browse_params == null && item_layouts == null)) {
                item {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        SubtleLoadingIndicator()
                    }
                }
            }
            else if (browse_params != null) {
                val row = browse_params_rows?.firstOrNull()
                if (row != null) {
                    item {
                        MediaItemList(
                            row.items,
                            Modifier.fillMaxHeight(),
                            content_padding = PaddingValues(end = end_padding),
                            title = row.title?.let { title ->
                                RawLocalisedString(title)
                            },
                            multiselect_context = multiselect_context,
                            play_as_list = true
                        )
                    }
                }
            }
            else if (single_layout != null) {
                item {
                    single_layout.TitleBar(Modifier.padding(end = end_padding, bottom = 5.dp))
                }

                items(single_layout.items) { item ->
                    Row(Modifier.padding(end = end_padding), verticalAlignment = Alignment.CenterVertically) {
                        MediaItemPreviewLong(
                            item,
                            multiselect_context = multiselect_context
                        )
                    }
                }
            }
            else {
                item {
                    Column(
                        Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(30.dp)
                    ) {
                        for (item_layout in item_layouts ?: emptyList()) {
                            val layout: MediaItemLayout = item_layout.rememberMediaItemLayout(player.database)

                            val layout_id: YoutubeUILocalisation.StringID? = (layout.title as? YoutubeLocalisedString)?.getYoutubeStringId()
                            if (layout_id == YoutubeUILocalisation.StringID.ARTIST_ROW_ARTISTS) {
                                continue
                            }

                            val is_singles: Boolean =
                                BehaviourSettings.Key.TREAT_SINGLES_AS_SONG.get()
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
                                        player.onPlayActionOccurred()
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
                                val type: MediaItemLayout.Type =
                                    if (layout.type == null) MediaItemLayout.Type.GRID
                                    else if (layout.type == MediaItemLayout.Type.NUMBERED_LIST) MediaItemLayout.Type.LIST
                                    else layout.type

                                type.Layout(
                                    if (previous_item == null) layout else layout.copy(title = null, subtitle = null),
                                    multiselect_context = multiselect_context,
                                    apply_filter = apply_filter,
                                    content_padding = PaddingValues(end = end_padding),
                                    grid_rows = Pair(1, 1)
                                )
                            }
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
