package com.spectre7.spmp.ui.components

import com.spectre7.spmp.ui.layout.PlayerStatus
import com.spectre7.spmp.model.Song
import com.spectre7.spmp.PlayerHost
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
// import androidx.compose.foundation.lazy.itemsIndexed

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun QueueTab(p_status: PlayerStatus, on_background_colour: Color) {

    var key_inc by remember { mutableStateOf(0) }

    data class Item(val song: Song, val key: Int, val p_status: PlayerStatus) {
        @Composable
        fun QueueElement(handle_modifier: Modifier, current: Boolean, index: Int, colour: Color) {

            val modifier = Modifier.padding(end = 10.dp).apply {
                if (current) it.border(Dp.Hairline, colour, CircleShape) else it
            }

            Row(horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
                song.PreviewBasic(false, Modifier.weight(1f).clickable {
                    PlayerHost.interact {
                        it.seekTo(index, 0)
                    }
                }, colour)

                // Remove button
                Image(rememberVectorPainter(Icons.Filled.Clear), "", modifier = Modifier
                    .requiredSize(25.dp),
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
                item.QueueElement(Modifier.detectReorder(state), current, index, on_background_colour)
            }
        }
    }
}