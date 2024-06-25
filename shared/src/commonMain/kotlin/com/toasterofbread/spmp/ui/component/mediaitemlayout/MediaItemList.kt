package com.toasterofbread.spmp.ui.component.mediaitemlayout

import LocalPlayerState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemHolder
import dev.toastbits.ytmkt.model.external.mediaitem.MediaItemLayout
import com.toasterofbread.spmp.model.mediaitem.layout.getDefaultMediaItemPreviewSize
import com.toasterofbread.spmp.model.mediaitem.layout.AppMediaItemLayout
import com.toasterofbread.spmp.model.mediaitem.rememberFilteredItems
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.toMediaItemData
import com.toasterofbread.spmp.model.MediaItemListParams
import com.toasterofbread.spmp.model.MediaItemLayoutParams
import com.toasterofbread.spmp.service.playercontroller.LocalPlayerClickOverrides
import com.toasterofbread.spmp.service.playercontroller.PlayerClickOverrides
import com.toasterofbread.spmp.ui.component.mediaitempreview.MediaItemPreviewLong
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import dev.toastbits.ytmkt.model.external.YoutubePage
import dev.toastbits.ytmkt.uistrings.UiString

const val MEDIAITEM_LIST_DEFAULT_SPACING_DP: Float = 10f

@Composable
fun MediaItemList(
    layout: AppMediaItemLayout,
    layout_params: MediaItemLayoutParams,
    list_params: MediaItemListParams = MediaItemListParams()
) {
    MediaItemList(
        layout_params =
            remember(layout, layout_params) {
                layout_params.copy(
                    items = layout.items,
                    title = layout.title,
                    subtitle = layout.subtitle,
                    view_more = layout.view_more
                )
            },
        list_params = list_params
    )
}

@Composable
fun MediaItemList(
    layout_params: MediaItemLayoutParams,
    list_params: MediaItemListParams = MediaItemListParams()
) {
    val filtered_items: List<MediaItem> by layout_params.rememberFilteredItems()
    if (filtered_items.isEmpty()) {
        return
    }

    val player: PlayerState = LocalPlayerState.current
    val click_overrides: PlayerClickOverrides = LocalPlayerClickOverrides.current

    Column(
        layout_params.modifier.padding(layout_params.content_padding),
        verticalArrangement = Arrangement.spacedBy(MEDIAITEM_LIST_DEFAULT_SPACING_DP.dp)
    ) {
        TitleBar(
            filtered_items,
            layout_params,
            modifier = layout_params.title_modifier.padding(bottom = 5.dp)
        )

        CompositionLocalProvider(LocalPlayerClickOverrides provides remember(list_params.play_as_list) {
            click_overrides.copy(
                onClickOverride = { item, index ->
                    if (list_params.play_as_list && item is Song) {
                        player.withPlayer {
                            undoableAction {
                                val songs: List<Song> = filtered_items.filterIsInstance<Song>()
                                addMultipleToQueue(songs, clear = true)

                                checkNotNull(index) { "Index is null ($item, ${songs.size}, $songs)" }
                                seekToSong(index)
                            }
                        }
                    }
                    else {
                        click_overrides.onMediaItemClicked(item, player)
                    }
                }
            )
        }) {
            for (item in filtered_items.withIndex()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (list_params.numbered) {
                        Text(
                            (item.index + 1).toString().padStart((filtered_items.size + 1).toString().length, '0'),
                            fontWeight = FontWeight.Light
                        )
                    }

                    MediaItemPreviewLong(
                        item.value,
                        Modifier.height(getDefaultMediaItemPreviewSize(true).height).fillMaxWidth(),
                        multiselect_context = layout_params.multiselect_context,
                        multiselect_key = item.index,
                        show_download_indicator = layout_params.show_download_indicators
                    )
                }
            }
        }
    }
}
