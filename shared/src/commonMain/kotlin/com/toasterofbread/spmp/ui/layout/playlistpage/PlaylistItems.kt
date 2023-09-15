package com.toasterofbread.spmp.ui.layout.playlistpage

import SpMp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemThumbnailProvider
import com.toasterofbread.spmp.model.mediaitem.mediaItemPreviewInteraction
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.resources.uilocalisation.durationToString
import com.toasterofbread.spmp.ui.component.Thumbnail
import com.toasterofbread.spmp.ui.component.longpressmenu.longPressMenuIcon
import com.toasterofbread.spmp.ui.component.mediaitempreview.getSongLongPressMenuData
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.ReorderableLazyListState
import org.burnoutcrew.reorderable.detectReorder

internal fun PlaylistPage.PlaylistItems(
    list_scope: LazyListScope,
    list_state: ReorderableLazyListState,
    sorted_items: List<Pair<MediaItem, Int>>?
) {
    list_scope.items(sorted_items ?: emptyList(), key = { it.second }) {
        val (item, index) = it
        check(item is Song)

        val long_press_menu_data = remember(item) {
            getSongLongPressMenuData(
                item,
                multiselect_context = multiselect_context,
                multiselect_key = index
            )
        }

        ReorderableItem(list_state, key = index) { dragging ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .mediaItemPreviewInteraction(
                        item,
                        long_press_menu_data,
                        onClick = { item, index ->
                            player.playPlaylist(playlist, index!!)
                        }
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(Modifier.size(50.dp)) {
                    item.Thumbnail(
                        MediaItemThumbnailProvider.Quality.LOW,
                        Modifier.fillMaxSize().longPressMenuIcon(long_press_menu_data)
                    )
                    multiselect_context.SelectableItemOverlay(item, Modifier.fillMaxSize(), key = index)
                }

                Column(
                    Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    val item_title: String? by item.observeActiveTitle()
                    Text(
                        item_title ?: "",
                        style = MaterialTheme.typography.titleSmall
                    )

                    val item_duration: Long? by item.Duration.observe(player.database)
                    val duration_text = remember(item_duration) {
                        item_duration?.let { duration ->
                            durationToString(duration, true, hl = SpMp.ui_language)
                        }
                    }
                    duration_text?.also { text ->
                        Text(text, style = MaterialTheme.typography.labelSmall)
                    }
                }

                AnimatedVisibility(reordering) {
                    Icon(Icons.Default.Reorder, null, Modifier.padding(end = 20.dp).detectReorder(list_state))
                }
            }
        }
    }
}
