@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.spectre7.spmp.ui.layout.nowplaying

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.tokens.FilledButtonTokens
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import com.spectre7.spmp.PlayerServiceHost
import com.spectre7.spmp.model.MediaItem
import com.spectre7.spmp.model.NowPlayingQueueRadioInfoPosition
import com.spectre7.spmp.model.Settings
import com.spectre7.spmp.model.Song
import com.spectre7.spmp.platform.MediaPlayerRepeatMode
import com.spectre7.spmp.platform.MediaPlayerService
import com.spectre7.spmp.platform.vibrateShort
import com.spectre7.spmp.ui.layout.MINIMISED_NOW_PLAYING_HEIGHT
import com.spectre7.spmp.ui.layout.PlayerViewContext
import com.spectre7.utils.*
import org.burnoutcrew.reorderable.*
import kotlin.math.roundToInt
import com.spectre7.utils.getString

private class QueueTabItem(val song: Song, val key: Int) {

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

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun QueueElement(
        list_state: ReorderableLazyListState,
        index: Int,
        backgroundColourProvider: () -> Color,
        playerProvider: () -> PlayerViewContext,
        requestRemove: () -> Unit
    ) {
        val swipe_state = queueElementSwipeState(requestRemove)
        val max_offset = with(LocalDensity.current) { SpMp.context.getScreenWidth().toPx() }
        val anchors = mapOf(-max_offset to 0, 0f to 1, max_offset to 2)

        Box(
            Modifier
                .offset { IntOffset(swipe_state.offset.value.roundToInt(), 0) }
                .background(RoundedCornerShape(45), backgroundColourProvider)
        ) {
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 10.dp, end = 20.dp)
            ) {
                song.PreviewLong(
                    MediaItem.PreviewParams(
                        remember(index) {
                            {
                                playerProvider().copy(onClickedOverride = {
                                    PlayerServiceHost.player.seekToSong(index)
                                })
                            }
                        },
                        Modifier
                            .weight(1f)
                            .swipeable(
                                swipe_state,
                                anchors,
                                Orientation.Horizontal,
                                thresholds = { _, _ -> FractionalThreshold(0.2f) }
                            ),
                        contentColour = { backgroundColourProvider().getContrasted() },
                    ),
                    queue_index = index
                )

                // Drag handle
                Icon(
                    Icons.Filled.Menu,
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QueueTab(expansionProvider: () -> Float, playerProvider: () -> PlayerViewContext, scroll: (pages: Int) -> Unit) {

    var key_inc by remember { mutableStateOf(0) }
    val radio_info_position: NowPlayingQueueRadioInfoPosition = Settings.getEnum(Settings.KEY_NP_QUEUE_RADIO_INFO_POSITION)

    val song_items: SnapshotStateList<QueueTabItem> = remember { mutableStateListOf<QueueTabItem>().also { list ->
        PlayerServiceHost.player.iterateSongs { _, song: Song ->
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
                song_items.add(to, song_items.removeAt(from))
            }
        }
    }

    var playing_key: Int? by remember { mutableStateOf(null) }
    LaunchedEffect(PlayerServiceHost.status.m_index, song_items.size) {
        val index = PlayerServiceHost.status.index
        if (index in song_items.indices) {
            playing_key = song_items[index].key
        }
        else {
            playing_key = null
        }
    }

    DisposableEffect(Unit) {
        PlayerServiceHost.player.addListener(queue_listener)
        onDispose {
            PlayerServiceHost.player.removeListener(queue_listener)
        }
    }

    val background_colour = getNPBackground(playerProvider)

    val backgroundColourProvider = { getNPBackground(playerProvider) }
    val queueBackgroundColourProvider = { getNPBackground(playerProvider).amplify(0.15f, 0.15f) }

    val shape = RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp)
    Box(
        Modifier
            .fillMaxSize()
            .padding(top = MINIMISED_NOW_PLAYING_HEIGHT.dp + (SpMp.context.getStatusBarHeight() * 0.5f))
            .background(shape, queueBackgroundColourProvider)
            .clip(shape)
    ) {
        val list_padding = 10.dp

        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            val padding = 15.dp
            Row(
                Modifier
                    .padding(top = padding, start = padding, end = padding, bottom = 10.dp)
                    .height(40.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                RepeatButton(backgroundColourProvider, Modifier.fillMaxHeight())
                StopAfterSongButton(backgroundColourProvider, Modifier.fillMaxHeight())

                Button(
                    onClick = {
                        PlayerServiceHost.player.clearQueue(keep_current = PlayerServiceHost.status.queue_size > 1)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = background_colour,
                        contentColor = background_colour.getContrasted()
                    )
                ) {
                    Text(getString("queue_clear"))
                }

                Surface(
                    Modifier.combinedClickable(
                        onClick = {
                            PlayerServiceHost.player.undoableAction {
                                PlayerServiceHost.player.shuffleQueue()
                            }
                        },
                        onLongClick = {
                            SpMp.context.vibrateShort()
                            PlayerServiceHost.player.undoableAction {
                                PlayerServiceHost.player.shuffleQueue(start = 0)
                            }
                        }
                    ),
                    color = background_colour,
                    shape = FilledButtonTokens.ContainerShape.toShape()
                ) {
                    Row(
                        Modifier
                            .defaultMinSize(
                                minWidth = ButtonDefaults.MinWidth,
                                minHeight = ButtonDefaults.MinHeight
                            )
                            .padding(ButtonDefaults.ContentPadding),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = getString("queue_shuffle"),
                            color = background_colour.getContrasted(),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                val undo_background = animateColorAsState(
                    if (PlayerServiceHost.status.m_undo_count != 0) background_colour
                    else background_colour.setAlpha(0.3f)
                ).value

                Box(
                    modifier = Modifier
                        .minimumTouchTargetSize()
                        .background(
                            undo_background,
                            CircleShape
                        )
                        .combinedClickable(
                            enabled = PlayerServiceHost.status.m_undo_count != 0,
                            onClick = { PlayerServiceHost.player.undo() },
                            onLongClick = {
                                SpMp.context.vibrateShort()
                                PlayerServiceHost.player.undoAll()
                            }
                        )
                        .size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Undo, null, tint = undo_background.getContrasted(true))
                }
            }

            if (radio_info_position == NowPlayingQueueRadioInfoPosition.TOP_BAR) {
                CurrentRadioIndicator(queueBackgroundColourProvider, backgroundColourProvider, playerProvider)
            }

            Divider(Modifier.padding(horizontal = list_padding), 1.dp, backgroundColourProvider)

            val items_above_queue = if (radio_info_position == NowPlayingQueueRadioInfoPosition.ABOVE_ITEMS) 1 else 0
            val state = rememberReorderableLazyListState(
                onMove = { from, to ->
                    song_items.add(to.index - items_above_queue, song_items.removeAt(from.index - items_above_queue))
                },
                onDragEnd = { from, to ->
                    if (from != to) {
                        song_items.add(from - items_above_queue, song_items.removeAt(to - items_above_queue))
                        PlayerServiceHost.player.undoableAction {
                            moveSong(from - items_above_queue, to - items_above_queue)
                        }
                        playing_key = null
                    }
                }
            )

            LazyColumn(
                state = state.listState,
                contentPadding = PaddingValues(top = list_padding, bottom = 60.dp),
                modifier = Modifier
                    .reorderable(state)
                    .detectReorderAfterLongPress(state)
                    .padding(horizontal = list_padding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (radio_info_position == NowPlayingQueueRadioInfoPosition.ABOVE_ITEMS) {
                    item {
                        CurrentRadioIndicator(queueBackgroundColourProvider, backgroundColourProvider, playerProvider)
                    }
                }

                items(song_items.size, { song_items[it].key }) { index ->
                    val item = song_items[index]
                    ReorderableItem(state, key = item.key) { is_dragging ->
                        LaunchedEffect(is_dragging) {
                            if (is_dragging) {
                                SpMp.context.vibrateShort()
                                playing_key = song_items[PlayerServiceHost.status.index].key
                            }
                        }

                        Box(Modifier.height(50.dp)) {
                            item.QueueElement(
                                state,
                                index,
                                {
                                    val current = if (playing_key != null) playing_key == item.key else PlayerServiceHost.status.m_index == index
                                    if (current) backgroundColourProvider()
                                    else queueBackgroundColourProvider()
                                },
                                playerProvider
                            ) {
                                PlayerServiceHost.player.undoableAction {
                                    PlayerServiceHost.player.removeFromQueue(index)
                                }
                            }
                        }
                    }
                }

                if (PlayerServiceHost.player.radio_loading) {
                    item {
                        Box(Modifier.height(50.dp), contentAlignment = Alignment.Center) {
                            SubtleLoadingIndicator(colourProvider = { queueBackgroundColourProvider().getContrasted() })
                        }
                    }
                }
            }
        }

        ActionBar(playerProvider, expansionProvider, scroll)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrentRadioIndicator(
    backgroundColourProvider: () -> Color,
    accentColourProvider: () -> Color,
    playerProvider: () -> PlayerViewContext
) {
    Column {
        val radio_item: MediaItem? = PlayerServiceHost.player.radio_item
        if (radio_item != null && radio_item !is Song) {
            radio_item.PreviewLong(MediaItem.PreviewParams(
                playerProvider,
                Modifier.padding(horizontal = 15.dp),
                contentColour = { backgroundColourProvider().getContrasted() }
            ))
        }

        val filters = PlayerServiceHost.player.radio_filters
        val current_filter = PlayerServiceHost.player.radio_current_filter
        if (filters != null) {
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(15.dp)
            ) {
                Spacer(Modifier)

                for (filter in listOf(null) + filters.withIndex()) {
                    FilterChip(
                        current_filter == filter?.index,
                        onClick = {
                            if (PlayerServiceHost.player.radio_current_filter != filter?.index) {
                                PlayerServiceHost.player.radio_current_filter = filter?.index
                            }
                        },
                        label = {
                            Text(
                                filter?.value?.joinToString("|") { it.getReadable() }
                                    ?: getString("radio_filter_all")
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            labelColor = backgroundColourProvider().getContrasted(),
                            selectedContainerColor = accentColourProvider(),
                            selectedLabelColor = accentColourProvider().getContrasted()
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun RepeatButton(backgroundColourProvider: () -> Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .minimumTouchTargetSize()
            .aspectRatio(1f)
            .background(CircleShape, backgroundColourProvider)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    PlayerServiceHost.player.repeat_mode =
                        when (PlayerServiceHost.player.repeat_mode) {
                            MediaPlayerRepeatMode.ALL -> MediaPlayerRepeatMode.ONE
                            MediaPlayerRepeatMode.ONE -> MediaPlayerRepeatMode.OFF
                            else -> MediaPlayerRepeatMode.ALL
                        }
                }
            )
            .crossOut(
                crossed_out = PlayerServiceHost.status.m_repeat_mode == MediaPlayerRepeatMode.OFF,
                colourProvider = { backgroundColourProvider().getContrasted() },
            ) {
                return@crossOut IntSize(
                    (getInnerSquareSizeOfCircle(it.width * 0.5f, 50) * 1.25f).roundToInt(),
                    (getInnerSquareSizeOfCircle(it.height * 0.5f, 50) * 1.25f).roundToInt()
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            when (PlayerServiceHost.status.m_repeat_mode) {
                MediaPlayerRepeatMode.ONE -> Icons.Filled.RepeatOne
                else -> Icons.Filled.Repeat
            },
            null,
            Modifier.size(20.dp),
            tint = backgroundColourProvider().getContrasted()
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StopAfterSongButton(backgroundColourProvider: () -> Color, modifier: Modifier = Modifier) {
    val rotation = remember { Animatable(0f) }
    OnChangedEffect(PlayerServiceHost.player.stop_after_current_song) {
        rotation.animateTo(
            if (PlayerServiceHost.player.stop_after_current_song) 180f else 0f
        )
    }

    Crossfade(PlayerServiceHost.player.stop_after_current_song) { stopping ->
        Box(
            modifier = modifier
                .minimumTouchTargetSize()
                .aspectRatio(1f)
                .background(CircleShape, backgroundColourProvider)
                .rotate(rotation.value)
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { PlayerServiceHost.player.stop_after_current_song = !stopping },
                    onLongClick = {}
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (stopping) Icons.Filled.HourglassBottom else Icons.Filled.HourglassEmpty,
                null,
                tint = backgroundColourProvider().getContrasted()
            )
        }
    }
}

@Composable
private fun BoxScope.ActionBar(playerProvider: () -> PlayerViewContext, expansionProvider: () -> Float, scroll: (pages: Int) -> Unit) {
    val slide_offset: (fullHeight: Int) -> Int = remember { { (it * 0.7).toInt() } }

    Box(
        Modifier
            .align(Alignment.BottomStart)
            .padding(10.dp)) {

        AnimatedVisibility(
            remember { derivedStateOf { expansionProvider() >= 0.975f } }.value,
            enter = slideInVertically(initialOffsetY = slide_offset),
            exit = slideOutVertically(targetOffsetY = slide_offset)
        ) {
            IconButton(
                { scroll(-1) },
                Modifier
                    .background(getNPOnBackground(playerProvider), CircleShape)
                    .size(40.dp)
            ) {
                Icon(Icons.Filled.KeyboardArrowUp, null, tint = getNPBackground(playerProvider))
            }
        }
    }
}
