@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
package com.toasterofbread.spmp.ui.layout.nowplaying.queue

import LocalPlayerState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.resources.uilocalisation.durationToString
import com.toasterofbread.spmp.ui.component.mediaitempreview.MediaItemPreviewLong
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.utils.common.getContrasted
import com.toasterofbread.utils.modifier.background
import org.burnoutcrew.reorderable.ReorderableLazyListState
import org.burnoutcrew.reorderable.detectReorder
import kotlin.math.roundToInt

class QueueTabItem(val song: Song, val key: Int) {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun queueElementSwipeState(requestRemove: () -> Unit): SwipeableState<Int> {
        val swipe_state = rememberSwipeableState(1)
        var removed by remember { mutableStateOf(false) }

        LaunchedEffect(remember { derivedStateOf { swipe_state.progress.fraction > 0.8f } }.value) {
            if (!removed && swipe_state.targetValue != 1 && swipe_state.progress.fraction > 0.8f) {
                requestRemove()
                removed = true
            }
        }

        return swipe_state
    }

    @Composable
    private fun getLPMTitle(index: Int): String? {
        val player = LocalPlayerState.current
        val playing_index = player.status.m_index
        if (index == playing_index) {
            return getString("lpm_song_now_playing")
        }

        val service = player.controller ?: return null

        var delta = 0L
        val indices = if (index < playing_index) index + 1 .. playing_index else playing_index until index
        for (i in indices) {
            val duration =
                service.getSong(i)?.Duration?.observe(player.database)?.value
                ?: return null
            delta += duration
        }

        return remember(delta) {
            (
                if (index < playing_index) getString("lpm_song_played_\$x_ago")
                else getString("lpm_song_playing_in_\$x")
            ).replace("\$x", durationToString(delta, true))
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun QueueElement(
        list_state: ReorderableLazyListState,
        index: Int,
        getBackgroundColour: () -> Color,
        multiselect_context: MediaItemMultiSelectContext,
        requestRemove: () -> Unit
    ) {
        val player = LocalPlayerState.current
        val swipe_state = queueElementSwipeState(requestRemove)
        val max_offset = with(LocalDensity.current) { player.screen_size.width.toPx() }
        val anchors = mapOf(-max_offset to 0, 0f to 1, max_offset to 2)

        Box(
            Modifier
                .offset { IntOffset(swipe_state.offset.value.roundToInt(), 0) }
                .background(RoundedCornerShape(45), getBackgroundColour)
        ) {
            val padding = 7.dp
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = padding, end = 10.dp)
            ) {
                MediaItemPreviewLong(
                    song,
                    Modifier
                        .weight(1f)
                        .padding(vertical = padding)
                        .swipeable(
                            swipe_state,
                            anchors,
                            Orientation.Horizontal,
                            thresholds = { _, _ -> FractionalThreshold(0.2f) }
                        ),
                    contentColour = { getBackgroundColour().getContrasted() },
                    show_type = false,
                    multiselect_context = multiselect_context,
                    multiselect_key = index,
                    getTitle = { getLPMTitle(index) }
                )

                val radio_item_index = player.controller?.radio_item_index
                if (radio_item_index == index) {
                    Icon(Icons.Default.Radio, null, Modifier.size(20.dp))
                }

                // Drag handle
                Icon(
                    Icons.Default.Menu,
                    null,
                    Modifier
                        .detectReorder(list_state)
                        .requiredSize(25.dp),
                    tint = getBackgroundColour().getContrasted()
                )
            }
        }
    }
}
