package com.toasterofbread.spmp.ui.layout.playlistpage

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material3.Icon
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.platform.getUiLanguage
import com.toasterofbread.spmp.ui.component.mediaitempreview.MediaItemPreviewLong
import com.toasterofbread.spmp.ui.component.mediaitempreview.getLongPressMenuData
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import dev.toastbits.ytmkt.uistrings.durationToString
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.ReorderableLazyListState
import org.burnoutcrew.reorderable.detectReorder
import LocalPlayerState

internal fun PlaylistAppPage.PlaylistItems(
    list_scope: LazyListScope,
    list_state: ReorderableLazyListState,
    sorted_items: List<Pair<MediaItem, Int>>?
) {
    list_scope.itemsIndexed(sorted_items ?: emptyList(), key = { _, item -> item.second }) { i, data ->
        val player: PlayerState = LocalPlayerState.current

        val (item, index) = data
        check(item is Song)

        val long_press_menu_data = remember(item, index) {
            item.getLongPressMenuData(multiselect_context, multiselect_key = index)
        }

        ReorderableItem(list_state, key = index) { dragging ->
            Row(Modifier.fillMaxWidth().padding(top = 10.dp)) {
                MediaItemPreviewLong(
                    item,
                    Modifier.fillMaxWidth().weight(1f),
                    long_press_menu_data = long_press_menu_data,
                    title_lines = 2,
                    show_artist = true,
                    show_type = false,
                    getExtraInfo = {
                        val item_duration: Long? by item.Duration.observe(player.database)
                        remember(item_duration) {
                            listOfNotNull(
                                item_duration?.let { duration ->
                                    durationToString(duration, player.context.getUiLanguage(), true)
                                }
                            )
                        }
                    },
                    multiselect_key = i,
                    multiselect_context = multiselect_context
                )

                AnimatedVisibility(reordering) {
                    Icon(Icons.Default.Reorder, null, Modifier.padding(end = 20.dp).detectReorder(list_state))
                }
            }
        }
    }
}
