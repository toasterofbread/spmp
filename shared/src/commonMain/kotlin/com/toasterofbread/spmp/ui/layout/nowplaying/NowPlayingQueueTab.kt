@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.toasterofbread.spmp.ui.layout.nowplaying

import LocalPlayerState
import SpMp
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
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import com.toasterofbread.spmp.api.RadioModifier
import com.toasterofbread.spmp.resources.uilocalisation.durationToString
import com.toasterofbread.spmp.model.MusicTopBarMode
import com.toasterofbread.spmp.model.NowPlayingQueueRadioInfoPosition
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemPreviewParams
import com.toasterofbread.spmp.model.mediaitem.Song
import com.toasterofbread.spmp.platform.MediaPlayerRepeatMode
import com.toasterofbread.spmp.platform.MediaPlayerService
import com.toasterofbread.spmp.platform.vibrateShort
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.layout.mainpage.MINIMISED_NOW_PLAYING_HEIGHT
import com.toasterofbread.spmp.ui.layout.mainpage.MINIMISED_NOW_PLAYING_V_PADDING
import com.toasterofbread.utils.*
import com.toasterofbread.utils.composable.Divider
import com.toasterofbread.utils.composable.OnChangedEffect
import com.toasterofbread.utils.composable.SubtleLoadingIndicator
import com.toasterofbread.utils.composable.crossOut
import com.toasterofbread.utils.modifier.background
import org.burnoutcrew.reorderable.*
import kotlin.math.roundToInt

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
            ).replace("\$x", durationToString(delta, true) )
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QueueTab(page_height: Dp, modifier: Modifier = Modifier) {
    var key_inc by remember { mutableStateOf(0) }
    val radio_info_position: NowPlayingQueueRadioInfoPosition = Settings.getEnum(Settings.KEY_NP_QUEUE_RADIO_INFO_POSITION)
    val multiselect_context: MediaItemMultiSelectContext = remember { MediaItemMultiSelectContext() { multiselect -> } }
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

    val background_colour = getNPBackground()
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
                            player.player?.undoableAction {
                                if (multiselect_context.is_active) {
                                    for (item in multiselect_context.getSelectedItems().sortedByDescending { it.second!! }) {
                                        player.player?.removeFromQueue(item.second!!)
                                    }
                                    multiselect_context.onActionPerformed()
                                }
                                else {
                                    player.player?.clearQueue(keep_current = player.status.m_song_count > 1)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = background_colour,
                            contentColor = background_colour.getContrasted()
                        ),
                        border = multiselect_context.getActiveHintBorder()
                    ) {
                        Text(getString("queue_clear"))
                    }

                    Surface(
                        Modifier.combinedClickable(
                            onClick = {
                                if (multiselect_context.is_active) {
                                    player.player?.undoableAction {
                                        player.player?.shuffleQueueAndIndices(multiselect_context.getSelectedItems().map { it.second!! })
                                    }
                                    multiselect_context.onActionPerformed()
                                }
                                else {
                                    player.player?.undoableAction {
                                        player.player?.shuffleQueue()
                                    }
                                }
                            },
                            onLongClick = if (multiselect_context.is_active) null else ({
                                player.player?.undoableAction {
                                    if (!multiselect_context.is_active) {
                                        SpMp.context.vibrateShort()
                                        player.player?.shuffleQueue(start = 0)
                                    }
                                }
                            })
                        ),
                        color = background_colour,
                        shape = FilledButtonTokens.ContainerShape.toShape(),
                        border = multiselect_context.getActiveHintBorder()
                    ) {
                        Box(
                            Modifier
                                .defaultMinSize(
                                    minWidth = ButtonDefaults.MinWidth,
                                    minHeight = ButtonDefaults.MinHeight
                                )
                                .padding(ButtonDefaults.ContentPadding),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = getString("queue_shuffle"),
//                                color = background_colour.getContrasted(),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    val undo_background = animateColorAsState(
                        if (player.status.m_undo_count != 0) LocalContentColor.current
                        else LocalContentColor.current.setAlpha(0.3f)
                    ).value

                    Box(
                        modifier = Modifier
                            .minimumTouchTargetSize()
                            .background(
                                undo_background,
                                CircleShape
                            )
                            .combinedClickable(
                                enabled = player.status.m_undo_count != 0 || player.status.m_redo_count != 0,
                                onClick = { 
                                    player.player?.undo() 
                                    SpMp.context.vibrateShort()
                                },
                                onLongClick = {
                                    player.player?.redo()
                                    SpMp.context.vibrateShort()
                                }
                            )
                            .size(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Undo, null, tint = undo_background.getContrasted(true))
                    }
                }

                if (radio_info_position == NowPlayingQueueRadioInfoPosition.TOP_BAR) {
                    CurrentRadioIndicator(backgroundColourProvider, multiselect_context)
                }

                Divider(Modifier.padding(horizontal = list_padding), 1.dp, backgroundColourProvider)

                CompositionLocalProvider(
                    LocalPlayerState provides remember { player.copy(onClickedOverride = { _, index: Int? ->
                        player.player?.seekToSong(index!!)
                    }) }
                ) {
                    LazyColumn(
                        state = queue_list_state.listState,
                        contentPadding = PaddingValues(top = list_padding, bottom = 60.dp),
                        modifier = Modifier
                            .reorderable(queue_list_state)
                            .padding(horizontal = list_padding),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (radio_info_position == NowPlayingQueueRadioInfoPosition.ABOVE_ITEMS) {
                            item {
                                CurrentRadioIndicator(backgroundColourProvider, multiselect_context)
                            }
                        }

                        items(song_items.size, { song_items[it].key }) { index ->
                            val item = song_items[index]
                            ReorderableItem(queue_list_state, key = item.key) { is_dragging ->
                                LaunchedEffect(is_dragging) {
                                    if (is_dragging) {
                                        SpMp.context.vibrateShort()
                                        playing_key = song_items[player.status.m_index].key
                                    }
                                }

                                Box(Modifier.height(50.dp)) {
                                    item.QueueElement(
                                        queue_list_state,
                                        index,
                                        {
                                            val current = if (playing_key != null) playing_key == item.key else player.status.m_index == index
                                            if (current) backgroundColourProvider()
                                            else queue_background_colour
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

                        if (player.player?.radio_loading == true) {
                            item {
                                Box(Modifier.height(50.dp), contentAlignment = Alignment.Center) {
                                    SubtleLoadingIndicator()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrentRadioIndicator(
    accentColourProvider: () -> Color,
    multiselect_context: MediaItemMultiSelectContext
) {
    val player = LocalPlayerState.current
    val horizontal_padding = 15.dp
    
    Row(Modifier.animateContentSize()) {

        val filters = player.player?.radio_filters
        var show_radio_info: Boolean by remember { mutableStateOf(false) }
        val radio_item: MediaItem? = player.player?.radio_item.takeIf { item ->
            item !is Song || player.player?.radio_item_index == null
        }

        LaunchedEffect(radio_item) {
            if (radio_item == null) {
                show_radio_info = false
            }
        }

        AnimatedVisibility(radio_item != null && filters != null) {
            IconButton(
                { show_radio_info = !show_radio_info },
                Modifier.padding(start = horizontal_padding)
            ) {
                Box {
                    Icon(Icons.Default.Radio, null)
                    val content_colour = LocalContentColor.current
                    Icon(
                        Icons.Default.Info, null,
                        Modifier
                            .align(Alignment.BottomEnd)
                            .offset(5.dp, 5.dp)
                            .size(18.dp)
                            // Fill gap in info icon
                            .drawBehind {
                                drawCircle(content_colour, size.width / 4)
                            },
                        tint = accentColourProvider()
                    )
                }
            }
        }

        Crossfade(if (show_radio_info) radio_item else if (multiselect_context.is_active) true else filters ?: radio_item) { state ->
            when (state) {
                is MediaItem ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = horizontal_padding)
                            .background(RoundedCornerShape(45), accentColourProvider)
                    ) {
                        state.PreviewLong(MediaItemPreviewParams(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 5.dp, vertical = 3.dp)
                        ))
                    }
                true ->
                    multiselect_context.InfoDisplay(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = horizontal_padding)
                    )
                is List<*> ->
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(15.dp)
                    ) {
                        Spacer(Modifier)

                        val current_filter = player.player?.radio_current_filter
                        for (filter in listOf(null) + state.withIndex()) {
                            FilterChip(
                                current_filter == filter?.index,
                                onClick = {
                                    if (player.player?.radio_current_filter != filter?.index) {
                                        player.player?.radio_current_filter = filter?.index
                                    }
                                },
                                label = {
                                    Text(
                                        (filter?.value as List<RadioModifier>?)?.joinToString("|") { it.getReadable() }
                                            ?: getString("radio_filter_all")
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    labelColor = LocalContentColor.current,
                                    selectedContainerColor = accentColourProvider(),
                                    selectedLabelColor = accentColourProvider().getContrasted()
                                )
                            )
                        }
                    }
            }
        }
    }
}

@Composable
private fun RepeatButton(backgroundColourProvider: () -> Color, modifier: Modifier = Modifier) {
    val player = LocalPlayerState.current
    Box(
        modifier = modifier
            .minimumTouchTargetSize()
            .aspectRatio(1f)
            .background(CircleShape, backgroundColourProvider)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    player.player?.repeat_mode =
                        when (player.player?.repeat_mode) {
                            MediaPlayerRepeatMode.ALL -> MediaPlayerRepeatMode.ONE
                            MediaPlayerRepeatMode.ONE -> MediaPlayerRepeatMode.OFF
                            else -> MediaPlayerRepeatMode.ALL
                        }
                }
            )
            .crossOut(
                crossed_out = player.status.m_repeat_mode == MediaPlayerRepeatMode.OFF,
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
            when (player.status.m_repeat_mode) {
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
    val player = LocalPlayerState.current
    val rotation by animateFloatAsState(
        if (player.player?.stop_after_current_song == true) 180f else 0f
    )

    Crossfade(player.player?.stop_after_current_song == true) { stopping ->
        Box(
            modifier = modifier
                .minimumTouchTargetSize()
                .aspectRatio(1f)
                .background(CircleShape, backgroundColourProvider)
                .rotate(rotation)
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { player.player?.stop_after_current_song = !stopping },
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

