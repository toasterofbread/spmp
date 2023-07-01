package com.toasterofbread.spmp.ui.layout.nowplaying.queue

import LocalPlayerState
import SpMp
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import com.toasterofbread.spmp.model.MusicTopBarMode
import com.toasterofbread.spmp.model.NowPlayingQueueRadioInfoPosition
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.model.mediaitem.Song
import com.toasterofbread.spmp.platform.MediaPlayerService
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.layout.mainpage.MINIMISED_NOW_PLAYING_HEIGHT
import com.toasterofbread.spmp.ui.layout.mainpage.MINIMISED_NOW_PLAYING_V_PADDING
import com.toasterofbread.spmp.ui.layout.nowplaying.*
import com.toasterofbread.spmp.ui.layout.nowplaying.NOW_PLAYING_TOP_BAR_HEIGHT
import com.toasterofbread.spmp.ui.layout.nowplaying.getNPAltOnBackground
import com.toasterofbread.spmp.ui.layout.nowplaying.getNPBackground
import com.toasterofbread.utils.*
import com.toasterofbread.utils.composable.Divider
import com.toasterofbread.utils.composable.SubtleLoadingIndicator
import org.burnoutcrew.reorderable.*

@Composable
fun QueueTab(page_height: Dp, modifier: Modifier = Modifier) {
    var key_inc by remember { mutableStateOf(0) }
    val radio_info_position: NowPlayingQueueRadioInfoPosition = Settings.getEnum(Settings.KEY_NP_QUEUE_RADIO_INFO_POSITION)
    val multiselect_context: MediaItemMultiSelectContext = remember { MediaItemMultiSelectContext() }
    val player = LocalPlayerState.current

    val song_items: SnapshotStateList<QueueTabItem> = remember { mutableStateListOf<QueueTabItem>().also { list ->
        player.player?.iterateSongs { _, song: Song ->
            list.add(QueueTabItem(song, key_inc++))
        }
    } }

    val queue_listener = remember {
        object : MediaPlayerService.Listener() {
            override fun onSongAdded(index: Int, song: Song) {
                song_items.add(index, QueueTabItem(song, key_inc++))
            }
            override fun onSongRemoved(index: Int) {
                song_items.removeAt(index)
            }
            override fun onSongMoved(from: Int, to: Int) {
                if (from == to) {
                    return
                }

                song_items.add(to, song_items.removeAt(from))

                for (item in multiselect_context.getSelectedItems().map { it.second!! }.withIndex()) {
                    if (item.value == from) {
                        multiselect_context.updateKey(item.index, to)
                    }
                    else if (from > to) {
                        if (item.value in to until from) {
                            multiselect_context.updateKey(item.index, item.value + 1)
                        }
                    }
                    else if (item.value in (from + 1) .. to) {
                        multiselect_context.updateKey(item.index, item.value - 1)
                    }
                }
            }
        }
    }

    var playing_key: Int? by remember { mutableStateOf(null) }
    LaunchedEffect(player.status.m_index, song_items.size) {
        if (player.status.m_index in song_items.indices) {
            playing_key = song_items[player.status.m_index].key
        }
        else {
            playing_key = null
        }
    }

    DisposableEffect(Unit) {
        player.player?.addListener(queue_listener)
        onDispose {
            player.player?.removeListener(queue_listener)
        }
    }

    val backgroundColourProvider = { getNPBackground() }
    val queue_background_colour = getNPAltOnBackground()

    val shape = RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp)

    val show_lyrics_in_queue: Boolean by Settings.KEY_TOPBAR_SHOW_LYRICS_IN_QUEUE.rememberMutableState()
    val show_visualiser_in_queue: Boolean by Settings.KEY_TOPBAR_SHOW_VISUALISER_IN_QUEUE.rememberMutableState()

    val items_above_queue = if (radio_info_position == NowPlayingQueueRadioInfoPosition.ABOVE_ITEMS) 1 else 0
    val queue_list_state = rememberReorderableLazyListState(
        onMove = { from, to ->
            song_items.add(to.index - items_above_queue, song_items.removeAt(from.index - items_above_queue))
        },
        onDragEnd = { from, to ->
            if (from != to) {
                song_items.add(from - items_above_queue, song_items.removeAt(to - items_above_queue))
                player.player?.undoableAction {
                    moveSong(from - items_above_queue, to - items_above_queue)
                }
                playing_key = null
            }
        }
    )

    val expansion = LocalNowPlayingExpansion.current
    val top_bar_height by animateDpAsState(
        when (expansion.top_bar_mode.value) {
            MusicTopBarMode.VISUALISER -> if (show_visualiser_in_queue) NOW_PLAYING_TOP_BAR_HEIGHT.dp else 0.dp
            MusicTopBarMode.LYRICS -> if (show_lyrics_in_queue) NOW_PLAYING_TOP_BAR_HEIGHT.dp else 0.dp
        }
    )

    val expanded by remember { derivedStateOf { expansion.get() > 1f } }
    LaunchedEffect(expanded) {
        if (expanded) {
            queue_list_state.listState.scrollToItem(player.status.m_index)
        }
    }

    CompositionLocalProvider(LocalContentColor provides queue_background_colour.getContrasted()) {
        Box(
            modifier
                // Add extra height for overscroll
                .requiredHeight(page_height + player.nowPlayingBottomPadding() + 200.dp)
                .requiredWidth(SpMp.context.getScreenWidth())
                .padding(top = MINIMISED_NOW_PLAYING_HEIGHT.dp + (SpMp.context.getStatusBarHeight() * 0.5f) + top_bar_height + MINIMISED_NOW_PLAYING_V_PADDING.dp)
                .background(queue_background_colour, shape)
                .clip(shape)
        ) {
            val list_padding = 10.dp

            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                QueueButtonsRow(
                    backgroundColourProvider,
                    multiselect_context
                )

                if (radio_info_position == NowPlayingQueueRadioInfoPosition.TOP_BAR) {
                    CurrentRadioIndicator(backgroundColourProvider, multiselect_context)
                }

                Divider(Modifier.padding(horizontal = list_padding), 1.dp, backgroundColourProvider)

                CompositionLocalProvider(
                    LocalPlayerState provides remember { player.copy(onClickedOverride = { _, index: Int? ->
                        player.player?.seekToSong(index!!)
                    }) }
                ) {
                    val density = LocalDensity.current
                    var list_position by remember { mutableStateOf(0.dp) }
                    LazyColumn(
                        state = queue_list_state.listState,
                        contentPadding = PaddingValues(top = list_padding),
                        modifier = Modifier
                            .reorderable(queue_list_state)
                            .padding(horizontal = list_padding)
                            .onPlaced { coords ->
                                list_position = with(density) { coords.positionInParent().y.toDp() }
                            },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (radio_info_position == NowPlayingQueueRadioInfoPosition.ABOVE_ITEMS) {
                            item {
                                CurrentRadioIndicator(backgroundColourProvider, multiselect_context)
                            }
                        }

                        QueueItems(
                            song_items,
                            queue_list_state,
                            multiselect_context,
                            player,
                            { playing_key },
                            { playing_key = it }
                        )

                        if (player.player?.radio_loading == true) {
                            item {
                                Box(Modifier.height(50.dp), contentAlignment = Alignment.Center) {
                                    SubtleLoadingIndicator()
                                }
                            }
                        }

                        item {
                            Spacer(Modifier.height(
                                60.dp + list_position + MINIMISED_NOW_PLAYING_HEIGHT.dp + top_bar_height
                            ))
                        }
                    }
                }
            }
        }
    }
}
