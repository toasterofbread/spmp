package com.spectre7.spmp.ui.components

import com.spectre7.spmp.ui.layout.PlayerStatus
import com.spectre7.spmp.model.Song
import com.spectre7.spmp.PlayerHost
import com.spectre7.utils.vibrate
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import org.burnoutcrew.reorderable.*
import com.google.android.exoplayer2.C

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun QueueTab(p_status: PlayerStatus, on_background_colour: Color) {

    var key_inc by remember { mutableStateOf(0) }

    data class Item(val song: Song, val key: Int, val p_status: PlayerStatus) {
        @Composable
        fun QueueElement(handle_modifier: Modifier, current: Boolean, index: Int, colour: Color, on_remove_request: () -> Unit) {

            val modifier = Modifier.padding(end = 10.dp).run<Modifier, Modifier> {
                if (current) this.border(Dp.Hairline, colour, CircleShape) else this
            }

            Row(horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
                song.PreviewBasic(false, Modifier.weight(1f).clickable {
                    PlayerHost.interact {
                        it.seekTo(index, C.TIME_UNSET)
                    }
                }, colour)

                // Remove button
                Image(rememberVectorPainter(Icons.Filled.Clear), "", modifier = Modifier
                    .requiredSize(25.dp)
                    .clickable(onClick = on_remove_request),
                    colorFilter = ColorFilter.tint(colour)
                )

                // Drag handle
                Image(rememberVectorPainter(Icons.Filled.Menu), "", modifier = handle_modifier
                    .requiredSize(25.dp),
                    colorFilter = ColorFilter.tint(colour)
                )
            }
        }
    }

    var song_items by remember { mutableStateOf(
        List(p_status.queue.size) {
            Item(p_status.queue[it], key_inc++, p_status)
        }
    ) }

    val queue_listener = remember {
        object : PlayerHost.PlayerQueueListener {
            override fun onSongAdded(song: Song, index: Int) {
                song_items = song_items.toMutableList().apply {
                    add(index, Item(song, key_inc++, p_status))
                }
            }
            override fun onSongRemoved(song: Song, index: Int) {
                song_items = song_items.toMutableList().apply {
                    removeAt(index)
                }
            }
            override fun onCleared() {
                song_items = emptyList()
            }
        }
    }

    var playing_key by remember { mutableStateOf<Int?>(null) }

    // TODO
    // LaunchedEffect(p_status.index) {
    //     playing_key =
    // }

    DisposableEffect(Unit) {
        PlayerHost.interactService {
            it.addQueueListener(queue_listener)
        }
        onDispose {
            PlayerHost.interactService {
                it.removeQueueListener(queue_listener)
            }
        }
    }

    val state = rememberReorderableLazyListState(
        onMove = { from, to ->
            song_items = song_items.toMutableList().apply {
                add(to.index, removeAt(from.index))
            }
        },
        onDragEnd = { from, to ->
            PlayerHost.interact {
                it.moveMediaItem(from, to)
            }
            playing_key = null
        }
    )

    LazyColumn(
        state = state.listState,
        modifier = Modifier
            .reorderable(state)
    ) {
        items(song_items.size, { song_items[it].key }) { index ->
            val item = song_items[index]
            ReorderableItem(state, item.key) { is_dragging ->
                LaunchedEffect(is_dragging) {
                    if (is_dragging) {
                        vibrate(0.01)
                        playing_key = song_items[p_status.index].key
                    }
                }

                val current = if (playing_key != null) playing_key == item.key else p_status.index == index
                item.QueueElement(Modifier.detectReorder(state), current, index, on_background_colour) {
                    song_items = song_items.toMutableList().apply {
                        removeAt(index)
                    }
                }
            }
        }
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        Button(
            onClick = {
                PlayerHost.interactService {
                    it.clearQueue()
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent
            )
        ) {
            Text(
                text = "Clear",
                color = on_background_colour
            )
        }

        Button(
            onClick = {
                PlayerHost.interactService {
                    // TODO
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent
            )
        ) {
            Text(
                text = "Shuffle",
                color = on_background_colour
            )
        }
    }
}