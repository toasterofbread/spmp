@file:OptIn(ExperimentalMaterial3Api::class)

package com.spectre7.spmp.ui.layout

import LocalPlayerState
import SpMp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.zIndex
import com.spectre7.spmp.api.durationToString
import com.spectre7.spmp.model.*
import com.spectre7.spmp.platform.LargeDropdownMenu
import com.spectre7.spmp.resources.getString
import com.spectre7.spmp.resources.getStringTODO
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
        getStringTODO(when(this) {
            PLAYLIST -> "Playlist order"
            ALPHABET -> "Name"
            DURATION -> "Duration"
            PLAY_COUNT -> "Listen count"
        })

    fun sortItems(items: List<MediaItem>, reversed: Boolean = false): List<MediaItem> = when (this) {
        PLAYLIST -> 
            if (reversed) items.asReversed() 
            else items
        ALPHABET -> 
            if (reversed) items.sortedByDescending { it.title!! } 
            else items.sortedBy { it.title!! }
        DURATION ->
            if (reversed) items.sortedByDescending { if (it is Song) it.duration ?: 0 else 0 }
            else items.sortedBy { if (it is Song) it.duration ?: 0 else 0 }
        PLAY_COUNT -> 
            if (reversed) items.sortedByDescending { it.registry_entry.play_count } 
            else items.sortedBy { it.registry_entry.play_count }
    }
}

@Composable
fun PlaylistPage(
    pill_menu: PillMenu,
    playlist: Playlist,
    previous_item: MediaItem? = null,
    close: () -> Unit
) {
    val status_bar_height = SpMp.context.getStatusBarHeight()
    var accent_colour: Color? by remember { mutableStateOf(null) }
    val player = LocalPlayerState.current
    val multiselect_context = remember { MediaItemMultiSelectContext() {} }

    var reorderable: Boolean by remember { mutableStateOf(false) }
    var current_filter: String? by remember { mutableStateOf(null) }
    var current_sort_option: SortOption by remember { mutableStateOf(SortOption.PLAYLIST) }

    LaunchedEffect(playlist) {
        accent_colour = null

        if (playlist.feed_layouts == null) {
            launch {
                val result = playlist.loadData()
                result.fold(
                    { playlist ->
                        if (playlist == null) {
                            SpMp.error_manager.onError("PlaylistPageLoad", Exception("loadData result is null"))
                        }
                    },
                    { error ->
                        SpMp.error_manager.onError("PlaylistPageLoad", error)
                    }
                )
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 10.dp).padding(top = status_bar_height), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (previous_item != null) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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

        val layout = playlist.layout

        val sorted_items: MutableList<Pair<MediaItem, Int>> = remember { mutableStateListOf() }
        LaunchedEffect(layout?.items?.size, current_sort_option, current_filter) {
            sorted_items.clear()
            layout?.let { layout ->
                sorted_items.addAll(
                    current_sort_option.sortItems(
                        layout.items.filter {
                            current_filter?.let { filter -> it.title!!.contains(filter, true) }
                                ?: true
                        }
                    ).withIndex().map { Pair(it.value, it.index) }
                )
            }
        }

        LaunchedEffect(reorderable) {
            if (reorderable) {
                return@LaunchedEffect
            }

            layout?.items?.also { items ->
                var update: Boolean = false
                for (i in 0 until items.size) {
                    val item = sorted_items[i].first
                    if (items[i] != item) {
                        items[i] = item
                        update = true
                    }
                }

                if (update) {
                    playlist.saveItems()
                }
            }
        }

        val items_above = 2

        val list_state = rememberReorderableLazyListState(
            onMove = { from, to ->
                if (to.index >= items_above) {
                    sorted_items.add(to.index - items_above, sorted_items.removeAt(from.index - items_above))
                }
            }
        )

        LazyColumn(
            state = list_state.listState,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.reorderable(list_state)
        ) {
            item {
                PlaylistTopInfo(playlist, accent_colour) {
                    if (accent_colour == null) {
                        accent_colour = playlist.getDefaultThemeColour() ?: Theme.current.accent
                    }
                }
            }

            layout?.also { layout ->
                item {
                    InteractionBar(
                        playlist,
                        layout,
                        accent_colour,
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
                        Modifier.fillMaxWidth().padding(top = 15.dp)
                    )
                }

                PlaylistItems(
                    playlist,
                    layout,
                    list_state,
                    sorted_items ?: emptyList(),
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
    layout: MediaItemLayout,
    list_state: ReorderableLazyListState,
    sorted_items: List<Pair<MediaItem, Int>>,
    multiselect_context: MediaItemMultiSelectContext,
    reorderable: Boolean,
    sort_option: SortOption,
    player: PlayerState
) {
    items(sorted_items, key = { it.second }) {
        val (item, index) = it
        check(item is Song)

        val long_press_menu_data = remember(item) {
            getSongLongPressMenuData(
                item,
                multiselect_context = multiselect_context
            )
        }

        ReorderableItem(list_state, key = index) { dragging ->
            Row(
                Modifier.fillMaxWidth().mediaItemPreviewInteraction(item, long_press_menu_data),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(Modifier.size(50.dp)) {
                    item.Thumbnail(
                        MediaItemThumbnailProvider.Quality.LOW,
                        Modifier.fillMaxSize().longPressMenuIcon(long_press_menu_data)
                    )
                    multiselect_context.SelectableItemOverlay(item, Modifier.fillMaxSize())
                }

                Column(
                    Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        item.title!!,
                        style = MaterialTheme.typography.titleSmall
                    )

                    val duration_text = remember(item.duration!!) {
                        durationToString(item.duration!!, SpMp.ui_language, true)
                    }
                    Text(duration_text, style = MaterialTheme.typography.labelSmall)
                }

                AnimatedVisibility(reorderable) {
                    Icon(Icons.Default.Reorder, null, Modifier.detectReorder(list_state))
                }
            }
        }
    }
}

@Composable
private fun PlaylistInfoText(playlist: Playlist, layout: MediaItemLayout) {
    CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.titleMedium) {
        Row {
            val total_duration_text = remember(playlist.total_duration) {
                if (playlist.total_duration == null) ""
                else durationToString(playlist.total_duration!!, SpMp.ui_language, false)
            }
            Text(total_duration_text)
            Text("\u2022")
            Text(getString("playlist_x_songs").replace("\$x", (playlist.item_count ?: layout.items.size).toString()))
        }
    }
}

@Composable
private fun InteractionBar(
    playlist: Playlist,
    layout: MediaItemLayout,
    accent_colour: Color?, 
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

    Column(modifier.animateContentSize()) {

        if (playlist.is_editable == true) {
            Row(Modifier.fillMaxWidth()) {
                PlaylistInfoText(playlist, layout)

                Spacer(Modifier.width(50.dp))
                Spacer(Modifier.fillMaxWidth().weight(1f))

                playlist.artist?.title?.also { artist ->
                    Marquee(arrangement = Arrangement.End) {
                        Text(
                            artist,
                            Modifier.clickable { player.onMediaItemClicked(playlist.artist!!) },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth()) {
            CompositionLocalProvider(LocalContentColor provides (accent_colour ?: Theme.current.accent)) {
                
                // Filter button
                IconButton({
                    if (opened_menu == 0) opened_menu = -1
                    else opened_menu = 0
                }) {
                    Crossfade(opened_menu == 0) { searching ->
                        Icon(if (searching) Icons.Default.Done else Icons.Default.Search, null)
                    }
                }

                // Animate between filter bar and remaining buttons
                Box(Modifier.fillMaxWidth()) {
                    this@Row.AnimatedVisibility(opened_menu != 0) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            // Sort
                            IconButton({
                                if (opened_menu == 1) opened_menu = -1
                                else opened_menu = 1
                            }) {
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
                            else {
                                PlaylistInfoText(playlist, layout)
                            }
                        }
                    }
                    this@Row.AnimatedVisibility(opened_menu == 0) {
                        InteractionBarFilterBox(filter, setFilter, Modifier.fillMaxWidth())
                    }
                }
            }
        }

        AnimatedVisibility(multiselect_context.is_active) {
            multiselect_context.InfoDisplay()
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
}

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
private fun PlaylistTopInfo(playlist: Playlist, accent_colour: Color?, onThumbLoaded: (ImageBitmap) -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    val player = LocalPlayerState.current

    Row(Modifier.height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(10.dp)) {

        var thumb_size by remember { mutableStateOf(IntSize.Zero) }
        playlist.Thumbnail(
            MediaItemThumbnailProvider.Quality.HIGH,
            Modifier.fillMaxWidth(0.5f).aspectRatio(1f).clip(shape).onSizeChanged {
                thumb_size = it
            },
            onLoaded = onThumbLoaded
        )

        Column(Modifier.height(with(LocalDensity.current) { thumb_size.height.toDp() })) {
            Box(Modifier.fillMaxHeight().weight(1f), contentAlignment = Alignment.CenterStart) {
                Text(
                    playlist.title!!,
                    style = MaterialTheme.typography.headlineSmall,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row {
                IconButton({ TODO() }) {
                    Icon(Icons.Default.Radio, null)
                }
                IconButton({ TODO() }) {
                    Icon(Icons.Default.Shuffle, null)
                }
                Crossfade(playlist.pinned_to_home) { pinned ->
                    IconButton({ playlist.setPinnedToHome(!pinned) }) {
                        Icon(if (pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin, null)
                    }
                }
                if (SpMp.context.canShare()) {
                    IconButton({ SpMp.context.shareText(playlist.url, playlist.title!!) }) {
                        Icon(Icons.Default.Share, null)
                    }
                }
            }

            Button(
                { player.playMediaItem(playlist) },
                Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent_colour ?: Theme.current.accent,
                    contentColor = accent_colour?.getContrasted() ?: Theme.current.on_accent
                ),
                shape = shape
            ) {
                Icon(Icons.Default.PlayArrow, null)
                Text(getString("playlist_chip_play"))
            }
        }
    }
}
