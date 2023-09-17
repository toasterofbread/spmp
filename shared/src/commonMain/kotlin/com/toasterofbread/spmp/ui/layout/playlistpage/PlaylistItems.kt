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
import androidx.compose.ui.unit.sp
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemThumbnailProvider
import com.toasterofbread.spmp.model.mediaitem.mediaItemPreviewInteraction
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.resources.uilocalisation.durationToString
import com.toasterofbread.spmp.ui.component.Thumbnail
import com.toasterofbread.spmp.ui.component.longpressmenu.longPressMenuIcon
import com.toasterofbread.spmp.ui.component.mediaitempreview.MediaItemPreviewLong
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

        val long_press_menu_data = remember(item, index) {
            getSongLongPressMenuData(
                item,
                multiselect_context = multiselect_context,
                multiselect_key = index
            )
        }

        ReorderableItem(list_state, key = index) { dragging ->
            Row(Modifier.fillMaxWidth().padding(top = 10.dp)) {
                MediaItemPreviewLong(
                    item,
                    Modifier.fillMaxWidth().weight(1f),
                    long_press_menu_data = long_press_menu_data,
                    title_lines = 2,
                    show_artist = false,
                    show_type = false,
                    getExtraInfo = {
                        val item_duration: Long? by item.Duration.observe(player.database)
                        remember(item_duration) {
                            listOfNotNull(
                                item_duration?.let { duration ->
                                    durationToString(duration, true, hl = SpMp.ui_language)
                                }
                            )
                        }
                    }
                )

                AnimatedVisibility(reordering) {
                    Icon(Icons.Default.Reorder, null, Modifier.padding(end = 20.dp).detectReorder(list_state))
                }
            }
        }
    }
}
