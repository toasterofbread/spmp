@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
package com.toasterofbread.spmp.ui.layout.nowplaying.queue

import LocalPlayerState
import SpMp
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.rememberSwipeableState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.model.mediaitem.MediaItemPreviewParams
import com.toasterofbread.spmp.model.mediaitem.Song
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.resources.uilocalisation.durationToString
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.utils.getContrasted
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
    private fun getInfoText(index: Int): String? {
        val player = LocalPlayerState.current
        val playing_index = player.status.m_index
        if (index == playing_index) {
            return getString("lpm_song_now_playing")
        }

        val service = player.player ?: return null

        var delta = 0L
        val indices = if (index < playing_index) index + 1 .. playing_index else playing_index until index
        for (i in indices) {
            delta += service.getSong(i)?.duration ?: return null
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
        backgroundColourProvider: () -> Color,
        multiselect_context: MediaItemMultiSelectContext,
        requestRemove: () -> Unit
    ) {
        val swipe_state = queueElementSwipeState(requestRemove)
        val max_offset = with(LocalDensity.current) { SpMp.context.getScreenWidth().toPx() }
        val anchors = mapOf(-max_offset to 0, 0f to 1, max_offset to 2)
        val player = LocalPlayerState.current

        val density = LocalDensity.current
        Box(
            Modifier
                .offset { IntOffset(swipe_state.offset.value.roundToInt(), 0) }
                .background(RoundedCornerShape(45), backgroundColourProvider)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 10.dp, end = 20.dp)
            ) {
                song.PreviewLong(
                    MediaItemPreviewParams(
                        Modifier
                            .weight(1f)
                            .swipeable(
                                swipe_state,
                                anchors,
                                Orientation.Horizontal,
                                thresholds = { _, _ -> FractionalThreshold(0.2f) }
                            ),
                        contentColour = { backgroundColourProvider().getContrasted() },
                        multiselect_context = multiselect_context,
                        getInfoText = { getInfoText(index) }
                    ),
                    queue_index = index
                )

                val radio_item_index = player.player?.radio_item_index
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
                    tint = backgroundColourProvider().getContrasted()
                )
            }
        }
    }
}
