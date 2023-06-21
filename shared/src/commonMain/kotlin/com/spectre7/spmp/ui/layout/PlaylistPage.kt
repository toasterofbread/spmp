@file:OptIn(ExperimentalMaterial3Api::class)

package com.spectre7.spmp.ui.layout

import LocalPlayerState
import SpMp
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.spectre7.spmp.api.durationToString
import com.spectre7.spmp.api.getOrReport
import com.spectre7.spmp.model.*
import com.spectre7.spmp.model.mediaitem.*
import com.spectre7.spmp.platform.LargeDropdownMenu
import com.spectre7.spmp.platform.composable.PlatformAlertDialog
import com.spectre7.spmp.platform.composable.platformClickable
import com.spectre7.spmp.platform.vibrateShort
import com.spectre7.spmp.resources.getString
import com.spectre7.spmp.ui.component.*
import com.spectre7.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.spectre7.spmp.ui.layout.mainpage.PlayerState
import com.spectre7.spmp.ui.theme.Theme
import com.spectre7.utils.*
import com.spectre7.utils.composable.*
import kotlinx.coroutines.*
import org.burnoutcrew.reorderable.*

private enum class SortOption {
    PLAYLIST, ALPHABET, DURATION, PLAY_COUNT;

    fun getReadable(): String = 
        getString(when(this) {
            PLAYLIST ->   "playlist_sort_option_playlist"
            ALPHABET ->   "playlist_sort_option_alphabet"
            DURATION ->   "playlist_sort_option_duration"
            PLAY_COUNT -> "playlist_sort_option_playcount"
        })

    fun sortItems(items: List<MediaItem>, reversed: Boolean = false): List<MediaItem> {
        val selector: (MediaItem) -> Comparable<*> = when (this) {
            PLAYLIST ->
                return if (reversed) items.asReversed()
                else items
            ALPHABET -> {
                { it.title!! }
            }
            DURATION -> {
                { if (it is Song) it.duration ?: 0 else 0 }
            }
            PLAY_COUNT -> {
                { it.registry_entry.getPlayCount(null) }
            }
        }
        return items.sortedWith(if (reversed) compareByDescending(selector) else compareBy(selector))
    }
}

@Composable
fun PlaylistPage(
    pill_menu: PillMenu,
    playlist: Playlist,
    previous_item: MediaItem? = null,
    padding: PaddingValues = PaddingValues(),
    close: () -> Unit
) {
    val player = LocalPlayerState.current
    val coroutine_scope = rememberCoroutineScope()

    val multiselect_context = remember { MediaItemMultiSelectContext() { context ->
        if (playlist.is_editable != true) {
            return@MediaItemMultiSelectContext
        }

        IconButton({ coroutine_scope.launch {
            val items = context.getSelectedItems().sortedByDescending { it.second!! }
            for (item in items) {
                playlist.removeItem(item.second!!)
                context.setItemSelected(item.first, false, item.second)
            }
            playlist.saveItems().getOrReport("PlaylistPageRemoveItemSave")
        } }) {
            Icon(Icons.Default.PlaylistRemove, null)
        }
    } }

    var accent_colour: Color? by remember { mutableStateOf(null) }
    var reorderable: Boolean by remember { mutableStateOf(false) }
    var current_filter: String? by remember { mutableStateOf(null) }
    var current_sort_option: SortOption by remember { mutableStateOf(SortOption.PLAYLIST) }

    LaunchedEffect(playlist) {
        accent_colour = null
        playlist.getFeedLayouts().getOrReport("PlaylistPageLoad")
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (previous_item != null) {
            Row(Modifier.fillMaxWidth().padding(top = padding.calculateTopPadding()), verticalAlignment = Alignment.CenterVertically) {
                IconButton(close) {
                    Icon(Icons.Default.KeyboardArrowLeft, null)
                }

                Spacer(Modifier.fillMaxWidth().weight(1f))
                previous_item.title!!.also { Text(it) }
                Spacer(Modifier.fillMaxWidth().weight(1f))

                IconButton({ player.showLongPressMenu(previous_item) }) {
                    Icon(Icons.Default.MoreVert, null)
                }
            }
        }

        var top_bar_showing: Boolean by remember { mutableStateOf(false) }
        MusicTopBar(
            Settings.KEY_LYRICS_SHOW_IN_PLAYLIST,
            Modifier.fillMaxWidth().padding(top = SpMp.context.getStatusBarHeight())
        ) { showing ->
            top_bar_showing = showing
        }
        
        val thumb_item = playlist.getThumbnailHolder().getHolder()

        LaunchedEffect(thumb_item) {
            if (thumb_item == playlist) {
                accent_colour = playlist.getThemeColour() ?: Theme.current.accent
            }
        }

        val sorted_items: MutableList<Pair<MediaItem, Int>> = remember { mutableStateListOf() }
        LaunchedEffect(playlist.items?.size, current_sort_option, current_filter) {
            sorted_items.clear()
            playlist.items?.let { items ->
                val filtered_items = current_filter.let { filter ->
                    if (filter == null) items
                    else items.filter { it.title!!.contains(filter, true) }
                }

                sorted_items.addAll(
                    current_sort_option
                        .sortItems(filtered_items)
                        .mapIndexed { index, value ->
                            Pair(value, index)
                        }
                )
            }
        }

        OnChangedEffect(reorderable) {
            if (!reorderable) {
                playlist.saveItems().getOrReport("PlaylistPageSaveItems")
            }
        }

        val items_above = 2

        val list_state = rememberReorderableLazyListState(
            onMove = { from, to ->
                check(reorderable)
                check(current_filter == null)
                check(current_sort_option == SortOption.PLAYLIST)

                if (to.index >= items_above && from.index >= items_above) {
                    sorted_items.add(to.index - items_above, sorted_items.removeAt(from.index - items_above))
                }
            },
            onDragEnd = { from, to ->
                if (to >= items_above && from >= items_above) {
                    playlist.moveItem(from - items_above, to - items_above)
                }
            }
        )

        LazyColumn(
            state = list_state.listState,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.reorderable(list_state),
            contentPadding = if (previous_item != null) padding.copy(top = 0.dp) else padding
        ) {
            item {
                PlaylistTopInfo(
                    playlist,
                    accent_colour ?: Theme.current.accent
                ) {
                    accent_colour = thumb_item.item?.getThemeColour()
                }
            }

            playlist.items?.also { items ->
                item {
                    InteractionBar(
                        playlist,
                        items,
                        reorderable,
                        {
                            reorderable = playlist.is_editable == true && it
                            if (reorderable) {
                                current_sort_option = SortOption.PLAYLIST
                                current_filter = null
                            }
                        },
                        current_filter,
                        {
                            check(!reorderable)
                            current_filter = it
                        },
                        current_sort_option,
                        {
                            check(!reorderable)
                            current_sort_option = it
                        },
                        multiselect_context,
                        Modifier.fillMaxWidth()
                    )
                }

                PlaylistItems(
                    playlist,
                    items,
                    list_state,
                    sorted_items,
                    multiselect_context, 
                    reorderable,
                    current_sort_option,
                    player,
                )
            }
        }
    }
}

private fun LazyListScope.PlaylistItems(
    playlist: Playlist,
    items: MutableList<MediaItem>,
    list_state: ReorderableLazyListState,
    sorted_items: List<Pair<MediaItem, Int>>,
    multiselect_context: MediaItemMultiSelectContext,
    reorderable: Boolean,
    sort_option: SortOption,
    player: PlayerState
) {
    if (sorted_items.isEmpty()) {
        item {
            Text(getString("playlist_empty"), Modifier.padding(top = 15.dp))
        }
    }

    items(sorted_items, key = { it.second }) {
        val (item, index) = it
        check(item is Song)

        val long_press_menu_data = remember(item) {
            getSongLongPressMenuData(
                item,
                multiselect_context = multiselect_context,
                multiselect_key = index
            )
        }

        ReorderableItem(list_state, key = index) { dragging ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .mediaItemPreviewInteraction(item, long_press_menu_data),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(Modifier.size(50.dp)) {
                    item.Thumbnail(
                        MediaItemThumbnailProvider.Quality.LOW,
                        Modifier.fillMaxSize().longPressMenuIcon(long_press_menu_data)
                    )
                    multiselect_context.SelectableItemOverlay(item, Modifier.fillMaxSize(), key = index)
                }

                Column(
                    Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        item.title!!,
                        style = MaterialTheme.typography.titleSmall
                    )

                    val duration_text = remember(item.duration) {
                        item.duration?.let { duration -> durationToString(duration, true, hl = SpMp.ui_language) }
                    }
                    duration_text?.also { text ->
                        Text(text, style = MaterialTheme.typography.labelSmall)
                    }
                }

                AnimatedVisibility(reorderable) {
                    Icon(Icons.Default.Reorder, null, Modifier.padding(end = 20.dp).detectReorder(list_state))
                }
            }
        }
    }
}

@Composable
private fun PlaylistInfoText(playlist: Playlist, items: List<MediaItem>) {
    CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.titleMedium) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val item_count = playlist.item_count ?: items.size
            if (item_count > 0) {
                val total_duration_text = remember(playlist.total_duration) {
                    if (playlist.total_duration == null) ""
                    else durationToString(playlist.total_duration!!, hl = SpMp.ui_language)
                }
                if (total_duration_text.isNotBlank()) {
                    Text(total_duration_text)
                    Text("\u2022")
                }

                Text(getString("playlist_x_songs").replace("\$x", item_count.toString()))
            }
        }
    }
}

@Composable
private fun InteractionBar(
    playlist: Playlist,
    items: MutableList<MediaItem>,
    reorderable: Boolean,
    setReorderable: (Boolean) -> Unit,
    filter: String?,
    setFilter: (String?) -> Unit,
    sort_option: SortOption,
    setSortOption: (SortOption) -> Unit,
    multiselect_context: MediaItemMultiSelectContext,
    modifier: Modifier = Modifier
) {
    val player = LocalPlayerState.current

    // 0 -> search, 1 -> sort
    var opened_menu: Int by remember { mutableStateOf(-1) }    

    Crossfade(multiselect_context.is_active) { selecting ->
        if (!selecting) {
            Row(Modifier.fillMaxWidth()) {
                // Filter button
                IconButton(
                    {
                        if (opened_menu == 0) opened_menu = -1
                        else opened_menu = 0
                    },
                    enabled = !reorderable
                ) {
                    Crossfade(opened_menu == 0) { searching ->
                        Icon(if (searching) Icons.Default.Done else Icons.Default.Search, null)
                    }
                }

                // Animate between filter bar and remaining buttons
                Box(Modifier.fillMaxWidth()) {
                    this@Row.AnimatedVisibility(opened_menu != 0) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            // Sort
                            IconButton(
                                {
                                    if (opened_menu == 1) opened_menu = -1
                                    else opened_menu = 1
                                },
                                enabled = !reorderable
                            ) {
                                Icon(Icons.Default.Sort, null)
                            }

                            Spacer(Modifier.fillMaxWidth().weight(1f))

                            if (playlist.is_editable == true) {
                                // Reorder
                                IconButton({ setReorderable(!reorderable) }) {
                                    Crossfade(reorderable) { reordering ->
                                        Icon(if (reordering) Icons.Default.Done else Icons.Default.Reorder, null)
                                    }
                                }
                                // Add
                                IconButton({ TODO() }) {
                                    Icon(Icons.Default.Add, null)
                                }
                            }
                        }
                    }
                    this@Row.AnimatedVisibility(opened_menu == 0) {
                        InteractionBarFilterBox(filter, setFilter, Modifier.fillMaxWidth())
                    }
                }
            }
        }
        else {
            multiselect_context.InfoDisplay()
        }
    }

    // Sort options
    LargeDropdownMenu(
        opened_menu == 1,
        { if (opened_menu == 1) opened_menu = -1 },
        SortOption.values().size,
        sort_option.ordinal,
        { SortOption.values()[it].getReadable() }
    ) {
        setSortOption(SortOption.values()[it])
        opened_menu = -1
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InteractionBarFilterBox(filter: String?, setFilter: (String?) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier) {
        TextField(
            filter ?: "",
            {  setFilter(it.ifEmpty { null }) },
            Modifier.fillMaxWidth().weight(1f),
            singleLine = true
        )

        IconButton(
            { setFilter(null) },
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = Color.Transparent,
                contentColor = LocalContentColor.current
            )
        ) {
            Icon(Icons.Default.Close, null)
        }
    }
}

@Composable
private fun TopInfoEditButtons(playlist: Playlist, accent_colour: Color, modifier: Modifier = Modifier, onFinished: () -> Unit) {
    Row(modifier) {
        IconButton(onFinished) {
            Icon(Icons.Default.Done, null)
        }

        if (playlist.is_editable == true) {
            var show_thumb_selection by remember { mutableStateOf(false) }

            IconButton({ show_thumb_selection = true }) {
                Icon(Icons.Default.Image, null)
            }

            if (show_thumb_selection) {
                PlatformAlertDialog(
                    onDismissRequest = { show_thumb_selection = false },
                    confirmButton = {
                        if (playlist.playlist_reg_entry.image_item_uid != null) {
                            IconButton({
                                playlist.playlist_reg_entry.image_item_uid = null
                                show_thumb_selection = false
                            }) {
                                Icon(Icons.Default.Refresh, null)
                            }
                        }
                    },
                    dismissButton = {
                        Button({ show_thumb_selection = false }) {
                            Text(getString("action_cancel"))
                        }
                    },
                    title = { Text(getString("playlist_select_image"), style = MaterialTheme.typography.headlineSmall) },
                    text = {
                        val player = LocalPlayerState.current
                        val playlist_items = playlist.items ?: emptyList()

                        if (playlist_items.isEmpty()) {
                            Text(getString("playlist_empty"))
                        }
                        else {
                            CompositionLocalProvider(LocalPlayerState provides remember { player.copy(
                                onClickedOverride = { item, _ ->
                                    playlist.playlist_reg_entry.image_item_uid = item.uid
                                    show_thumb_selection = false
                                }
                            ) }) {
                                LazyColumn {
                                    items(playlist_items) { item ->
                                        item.PreviewLong(MediaItemPreviewParams())
                                    }
                                }
                            }
                        }
                    }
                )
            }
        }

        if (playlist is LocalPlaylist) {
            val coroutine_scope = rememberCoroutineScope()
            var converting by remember { mutableStateOf(false) }

            Spacer(Modifier.fillMaxWidth().weight(1f))

            Button(
                {
                    if (converting) {
                        return@Button
                    }
                    coroutine_scope.launch {
                        converting = true
                        playlist.convertToAccountPlaylist().getOrReport("ConvertPlaylistToAccountPlaylist")
                        converting = false
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent_colour,
                    contentColor = accent_colour.getContrasted()
                )
            ) {
                Text(getString("playlist_upload_to_account"))
                Spacer(Modifier.width(10.dp))
                Crossfade(converting) { active ->
                    if (active) {
                        SubtleLoadingIndicator(Modifier.size(24.dp))
                    }
                    else {
                        Icon(Icons.Default.CloudUpload, null)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistTopInfo(playlist: Playlist, accent_colour: Color, modifier: Modifier = Modifier, onThumbLoaded: (ImageBitmap) -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    val min_height = 120.dp

    val player = LocalPlayerState.current
    val density = LocalDensity.current

    var editing_info by remember { mutableStateOf(false) }
    var edited_title: String by remember { mutableStateOf("") }

    var split_position by remember(playlist) { mutableStateOf(playlist.playlist_reg_entry.playlist_page_thumb_width ?: 0f) }
    var width: Dp by remember(playlist) { mutableStateOf(0.dp) }
    var show_image by remember(playlist) { mutableStateOf(true) }

    DisposableEffect(Unit) {
        onDispose {
            if (editing_info) {
                playlist.saveRegistry()
            }
        }
    }

    LaunchedEffect(editing_info) {
        if (editing_info) {
            edited_title = playlist.title!!
        }
        else {
            playlist.saveRegistry()
        }
    }

    Column(modifier.animateContentSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            Modifier
                .height(IntrinsicSize.Max)
                .onSizeChanged {
                    val width_dp = with(density) { it.width.toDp() }
                    if (width == width_dp) {
                        return@onSizeChanged
                    }
                    width = width_dp

                    if (split_position == 0f) {
                        split_position = min_height / width
                    }
                },
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            var image_size by remember { mutableStateOf(IntSize.Zero) }

            // Playlist image
            AnimatedVisibility(show_image) {
                Box(
                    Modifier
                        .heightIn(min = min_height)
                        .fillMaxWidth(split_position)
                ) {
                    playlist.Thumbnail(
                        MediaItemThumbnailProvider.Quality.HIGH,
                        Modifier
                            .fillMaxSize()
                            .aspectRatio(1f)
                            .clip(shape)
                            .onSizeChanged {
                                image_size = it
                            }
                            .platformClickable(
                                onClick = {},
                                onAltClick = {
                                    if (!editing_info) {
                                        editing_info = true
                                        SpMp.context.vibrateShort()
                                    }
                                }
                            ),
                        onLoaded = onThumbLoaded
                    )
                }
            }

            // System insets spacing
            AnimatedVisibility(editing_info && !show_image) {
                SpMp.context.getSystemInsets()?.also { system_insets ->
                    with(LocalDensity.current) {
                        Spacer(Modifier.width(system_insets.getLeft(this, LocalLayoutDirection.current).toDp()))
                    }
                }
            }

            // Split position drag handle
            AnimatedVisibility(editing_info) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .width(12.dp)
                        .border(Dp.Hairline, LocalContentColor.current, shape)
                        .draggable(
                            orientation = Orientation.Horizontal,
                            state = rememberDraggableState { delta ->
                                val delta_dp = with(density) { delta.toDp() }
                                if (!show_image) {
                                    if (delta_dp > 0.dp) {
                                        show_image = true
                                        split_position = min_height / width
                                    }
                                }
                                else {
                                    split_position = (split_position + (delta_dp / width)).coerceIn(0.1f, 0.9f)
                                    if (split_position * width < min_height) {
                                        show_image = false
                                    }
                                }

                                playlist.playlist_reg_entry.playlist_page_thumb_width = split_position
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.MoreVert, null)
                }
            }

            // Main info column
            Column(Modifier.height(with(LocalDensity.current) { image_size.height.toDp().coerceAtLeast(min_height) })) {

                // Title text
                Box(Modifier.fillMaxHeight().weight(1f), contentAlignment = Alignment.CenterStart) {
                    Crossfade(editing_info) { editing ->
                        if (!editing) {
                            Text(
                                playlist.title!!,
                                style = MaterialTheme.typography.headlineSmall,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.platformClickable(
                                    onClick = {},
                                    onAltClick = {
                                        editing_info = true
                                    }
                                )
                            )
                        }
                        else {
                            val colour = LocalContentColor.current
                            val line_padding = with(LocalDensity.current) { 5.dp.toPx() }
                            val line_width = with(LocalDensity.current) { 1.dp.toPx() }

                            BasicTextField(
                                edited_title,
                                {
                                    edited_title = it.replace("\n", "")
                                    playlist.registry_entry.title = edited_title.trim()
                                },
                                Modifier
                                    .fillMaxWidth()
                                    .drawBehind {
                                        drawLine(
                                            colour,
                                            center + Offset(-size.width / 2f, (size.height / 2f) + line_padding),
                                            center + Offset(size.width / 2f, (size.height / 2f) + line_padding),
                                            strokeWidth = line_width
                                        )
                                    },
                                textStyle = LocalTextStyle.current.copy(color = colour),
                                cursorBrush = SolidColor(colour)
                            )
                        }
                    }
                }

                // Play button
                Button(
                    { player.playMediaItem(playlist) },
                    Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent_colour,
                        contentColor = accent_colour.getContrasted()
                    ),
                    shape = shape
                ) {
                    Icon(Icons.Default.PlayArrow, null)
                    Text(getString("playlist_chip_play"))
                }
            }
        }

        Crossfade(editing_info) { editing ->
            if (editing) {
                TopInfoEditButtons(playlist, accent_colour, Modifier.fillMaxWidth()) {
                    editing_info = false
                }
            }
            else {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton({ player.playMediaItem(playlist, true) }) {
                        Icon(Icons.Default.Shuffle, null)
                    }
                    Crossfade(playlist.pinned_to_home) { pinned ->
                        IconButton({ playlist.setPinnedToHome(!pinned) }) {
                            Icon(if (pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin, null)
                        }
                    }
                    playlist.url?.also { url ->
                        if (SpMp.context.canShare()) {
                            IconButton({ SpMp.context.shareText(url, playlist.title!!) }) {
                                Icon(Icons.Default.Share, null)
                            }
                        }
                    }

                    IconButton({ editing_info = true }) {
                        Icon(Icons.Default.Edit, null)
                    }

                    Spacer(Modifier.fillMaxWidth().weight(1f))

                    playlist.items?.also {
                        PlaylistInfoText(playlist, it)
                    }
                }
            }
        }
    }
}
