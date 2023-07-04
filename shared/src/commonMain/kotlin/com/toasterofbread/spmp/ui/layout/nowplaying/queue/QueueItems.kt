package com.toasterofbread.spmp.ui.layout.nowplaying.queue

import SpMp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.platform.vibrateShort
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.layout.mainpage.PlayerState
import com.toasterofbread.spmp.ui.layout.nowplaying.getNPAltBackground
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
    setPlayingKey: (Int?) -> Unit
) {
    items(song_items.size, { song_items[it].key }) { index ->
        val item = song_items[index]
        ReorderableItem(queue_list_state, key = item.key) { is_dragging ->
            LaunchedEffect(is_dragging) {
                if (is_dragging) {
                    SpMp.context.vibrateShort()
                    setPlayingKey(song_items[player.status.m_index].key)
                }
            }

            Box(Modifier.height(50.dp)) {
                item.QueueElement(
                    queue_list_state,
                    index,
                    {
                        val playing_key = getPlayingKey()
                        val current = if (playing_key != null) playing_key == item.key else player.status.m_index == index
                        if (current) getNPBackground()
                        else getNPAltOnBackground()
                    },
                    multiselect_context
                ) {
                    player.player?.undoableAction {
                        player.player?.removeFromQueue(index)
                    }
                }
            }
        }
    }
}
