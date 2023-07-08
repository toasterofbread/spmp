package com.toasterofbread.spmp.ui.layout.nowplaying.queue

import LocalPlayerState
import SpMp
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.toasterofbread.spmp.model.NowPlayingQueueRadioInfoPosition
import com.toasterofbread.spmp.model.NowPlayingQueueWaveBorderMode
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.model.mediaitem.Song
import com.toasterofbread.spmp.platform.MediaPlayerService
import com.toasterofbread.spmp.ui.component.WaveBorder
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.layout.mainpage.MINIMISED_NOW_PLAYING_HEIGHT
import com.toasterofbread.spmp.ui.layout.mainpage.MINIMISED_NOW_PLAYING_V_PADDING
import com.toasterofbread.spmp.ui.layout.nowplaying.LocalNowPlayingExpansion
import com.toasterofbread.spmp.ui.layout.nowplaying.LocalNowPlayingExpansion
import com.toasterofbread.spmp.ui.layout.nowplaying.getNPAltOnBackground
import com.toasterofbread.spmp.ui.layout.nowplaying.getNPBackground
import com.toasterofbread.spmp.ui.layout.nowplaying.maintab.NOW_PLAYING_TOP_BAR_HEIGHT
import com.toasterofbread.spmp.ui.layout.nowplaying.rememberTopBarShouldShowInQueue
import com.toasterofbread.utils.composable.SubtleLoadingIndicator
import com.toasterofbread.utils.getContrasted
import kotlinx.coroutines.delay
import org.burnoutcrew.reorderable.ReorderableLazyListState
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable

@Composable
fun QueueTab(page_height: Dp, modifier: Modifier = Modifier) {
    val player = LocalPlayerState.current
    val expansion = LocalNowPlayingExpansion.current
    val density = LocalDensity.current

    var key_inc by remember { mutableStateOf(0) }
    val radio_info_position: NowPlayingQueueRadioInfoPosition = Settings.getEnum(Settings.KEY_NP_QUEUE_RADIO_INFO_POSITION)
    val multiselect_context: MediaItemMultiSelectContext = remember { MediaItemMultiSelectContext() }

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

    val show_top_bar by rememberTopBarShouldShowInQueue(expansion.top_bar_mode.value)
    val top_bar_height by animateDpAsState(
        if (show_top_bar) NOW_PLAYING_TOP_BAR_HEIGHT.dp else 0.dp
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
                .requiredHeight(page_height + 200.dp)
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

                val wave_border_mode: NowPlayingQueueWaveBorderMode by Settings.KEY_NP_QUEUE_WAVE_BORDER_MODE.rememberMutableEnumState()
                QueueBorder(wave_border_mode, list_padding, queue_list_state)

                CompositionLocalProvider(
                    LocalPlayerState provides remember { player.copy(onClickedOverride = { _, index: Int? ->
                        player.player?.seekToSong(index!!)
                    }) }
                ) {
                    var list_position by remember { mutableStateOf(0.dp) }
                    LazyColumn(
                        state = queue_list_state.listState,
                        contentPadding = PaddingValues(
                            top = list_padding +
                                // Extra space to prevent initial wave border overlap
                                if (wave_border_mode != NowPlayingQueueWaveBorderMode.LINE) 15.dp else 0.dp
                        ),
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
                                NOW_PLAYING_TOP_BAR_HEIGHT.dp + MINIMISED_NOW_PLAYING_HEIGHT.dp + list_position + list_padding
                            ))
                        }
                    }
                }
            }
        }
    }
}

private const val WAVE_BORDER_TIME_SPEED: Float = 0.15f

@Composable
private fun QueueBorder(
    wave_border_mode: NowPlayingQueueWaveBorderMode,
    list_padding: Dp,
    queue_list_state: ReorderableLazyListState
) {
    if (wave_border_mode == NowPlayingQueueWaveBorderMode.LINE) {
        Divider(Modifier.padding(horizontal = list_padding), 1.dp, getNPBackground())
    }
    else {
        val player = LocalPlayerState.current
        var wave_border_offset: Float by remember { mutableStateOf(0f) }

        LaunchedEffect(wave_border_mode) {
            val update_interval: Long = 1000 / 30
            when (wave_border_mode) {
                NowPlayingQueueWaveBorderMode.TIME -> {
                    while (true) {
                        wave_border_offset += update_interval * WAVE_BORDER_TIME_SPEED
                        delay(update_interval)
                    }
                }
                NowPlayingQueueWaveBorderMode.TIME_SYNC -> {
                    while (true) {
                        wave_border_offset = player.status.getPositionMillis() * WAVE_BORDER_TIME_SPEED
                        delay(update_interval)
                    }
                }
                else -> wave_border_offset = 0f
            }
        }

        WaveBorder(
            Modifier.fillMaxWidth().zIndex(1f),
            getColour = { getNPAltOnBackground() },
            getWaveOffset = {
                when (wave_border_mode) {
                    NowPlayingQueueWaveBorderMode.SCROLL -> {
                        ((50.dp.toPx() * queue_list_state.listState.firstVisibleItemIndex) + queue_list_state.listState.firstVisibleItemScrollOffset)
                    }
                    else -> wave_border_offset
                }
            },
            border_thickness = 1.5.dp,
            border_colour = getNPBackground()
        )
    }
}
