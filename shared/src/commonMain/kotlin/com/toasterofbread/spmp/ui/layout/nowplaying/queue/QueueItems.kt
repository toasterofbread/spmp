package com.toasterofbread.spmp.ui.layout.nowplaying.queue

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.toastbits.composekit.context.vibrateShort
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.nowplaying.getNPAltOnBackground
import com.toasterofbread.spmp.ui.layout.nowplaying.getNPBackground
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.ReorderableLazyListState

fun LazyListScope.QueueItems(
    song_items: SnapshotStateList<QueueTabItem>,
    queue_list_state: ReorderableLazyListState,
    multiselect_context: MediaItemMultiSelectContext,
    player: PlayerState,
    getPlayingKey: () -> Int?,
    setPlayingKey: (Int?) -> Unit,
    item_modifier: Modifier = Modifier,
    getItemColour: PlayerState.() -> Color = { getNPAltOnBackground() },
    getCurrentItemColour: PlayerState.() -> Color = { getNPBackground() }
) {
    val items: List<QueueTabItem> = song_items.toList()
    items(items.size, { items[it].key }) { index ->
        val item: QueueTabItem = song_items.getOrNull(index) ?: return@items
        ReorderableItem(queue_list_state, item.key, item_modifier) { is_dragging ->
            LaunchedEffect(is_dragging) {
                if (is_dragging) {
                    player.context.vibrateShort()
                    setPlayingKey(items.getOrNull(player.status.m_index)?.key)
                }
            }

            Box(Modifier.height(50.dp)) {
                item.QueueElement(
                    queue_list_state,
                    index,
                    {
                        val playing_key = getPlayingKey()
                        val current = if (playing_key != null) playing_key == item.key else player.status.m_index == index
                        if (current) getCurrentItemColour(player)
                        else getItemColour(player)
                    },
                    multiselect_context
                ) {
                    player.controller?.service_player?.undoableAction {
                        removeFromQueue(index)
                    }
                }
            }
        }
    }
}
