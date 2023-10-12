package com.toasterofbread.spmp.ui.component.mediaitemlayout

import LocalPlayerState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
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
import com.toasterofbread.spmp.model.mediaitem.layout.MediaItemLayout
import com.toasterofbread.spmp.model.mediaitem.layout.ViewMore
import com.toasterofbread.spmp.model.mediaitem.rememberFilteredItems
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.resources.uilocalisation.LocalisedString
import com.toasterofbread.spmp.ui.component.mediaitempreview.MediaItemPreviewLong
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.PlayerState

const val MEDIAITEM_LIST_DEFAULT_SPACING_DP: Float = 10f

@Composable
fun MediaItemList(
    layout: MediaItemLayout,
    modifier: Modifier = Modifier,
    title_modifier: Modifier = Modifier,
    numbered: Boolean = false,
    multiselect_context: MediaItemMultiSelectContext? = null,
    apply_filter: Boolean = false,
    show_download_indicators: Boolean = true
) {
    MediaItemList(layout.items, modifier, title_modifier, numbered, layout.title, layout.subtitle, layout.view_more, multiselect_context, apply_filter, show_download_indicators = show_download_indicators)
}

@Composable
fun MediaItemList(
    items: List<MediaItemHolder>,
    modifier: Modifier = Modifier,
    title_modifier: Modifier = Modifier,
    numbered: Boolean = false,
    title: LocalisedString? = null,
    subtitle: LocalisedString? = null,
    view_more: ViewMore? = null,
    multiselect_context: MediaItemMultiSelectContext? = null,
    apply_filter: Boolean = false,
    show_download_indicators: Boolean = true,
    play_as_list: Boolean = false
) {
    val filtered_items: List<MediaItem> by items.rememberFilteredItems(apply_filter)
    val player: PlayerState = LocalPlayerState.current

    Column(modifier, verticalArrangement = Arrangement.spacedBy(MEDIAITEM_LIST_DEFAULT_SPACING_DP.dp)) {
        TitleBar(
            items,
            title,
            subtitle,
            title_modifier.padding(bottom = 5.dp),
            view_more = view_more,
            multiselect_context = multiselect_context
        )

        CompositionLocalProvider(LocalPlayerState provides remember(play_as_list) {
            player.copy(onClickedOverride = { item, index ->  
                if (play_as_list && item is Song) {
                    player.withPlayer {
                        undoableAction {
                            addMultipleToQueue(
                                filtered_items.filterIsInstance<Song>(),
                                clear = true
                            )

                            seekToSong(index!!)
                        }
                    }
                }
                else {
                    player.onMediaItemClicked(item, index)
                }
            })
        }) {
            for (item in filtered_items.withIndex()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (numbered) {
                        Text(
                            (item.index + 1).toString().padStart((filtered_items.size + 1).toString().length, '0'), 
                            fontWeight = FontWeight.Light
                        )
                    }

                    MediaItemPreviewLong(
                        item.value, 
                        multiselect_context = multiselect_context, 
                        show_download_indicator = show_download_indicators, 
                        multiselect_key = item.index
                    )
                }
            }
        }
    }
}
