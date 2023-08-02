package com.toasterofbread.spmp.ui.layout.playlistpage

import LocalPlayerState
import SpMp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlaylistRemove
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.toasterofbread.Database
import com.toasterofbread.spmp.api.getOrReport
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemThumbnailProvider
import com.toasterofbread.spmp.model.mediaitem.Playlist
import com.toasterofbread.spmp.model.mediaitem.Song
import com.toasterofbread.spmp.model.mediaitem.getMediaItemPlayCount
import com.toasterofbread.spmp.model.mediaitem.isMediaItemHidden
import com.toasterofbread.spmp.model.mediaitem.loader.loadDataOnChange
import com.toasterofbread.spmp.model.mediaitem.mediaItemPreviewInteraction
import com.toasterofbread.spmp.model.mediaitem.playlist.PlaylistEditor.Companion.rememberEditorOrNull
import com.toasterofbread.spmp.platform.getDefaultHorizontalPadding
import com.toasterofbread.spmp.platform.getDefaultVerticalPadding
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.resources.uilocalisation.durationToString
import com.toasterofbread.spmp.ui.component.MultiselectAndMusicTopBar
import com.toasterofbread.spmp.ui.component.PillMenu
import com.toasterofbread.spmp.ui.component.Thumbnail
import com.toasterofbread.spmp.ui.component.longpressmenu.longPressMenuIcon
import com.toasterofbread.spmp.ui.component.mediaitempreview.getSongLongPressMenuData
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.layout.mainpage.PlayerState
import com.toasterofbread.spmp.ui.theme.Theme
import com.toasterofbread.utils.composable.SubtleLoadingIndicator
import com.toasterofbread.utils.composable.stickyHeaderWithTopPadding
import com.toasterofbread.utils.copy
import com.toasterofbread.utils.getThemeColour
import kotlinx.coroutines.launch
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.ReorderableLazyListState
import org.burnoutcrew.reorderable.detectReorder
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable

@Composable
fun PlaylistPage(
    pill_menu: PillMenu,
    playlist: Playlist,
    previous_item: MediaItem? = null,
    padding: PaddingValues = PaddingValues(),
    close: () -> Unit
) {
    val db = SpMp.context.database
    val player = LocalPlayerState.current
    val coroutine_scope = rememberCoroutineScope()

    val loading by playlist.loadDataOnChange(db)
    val playlist_items: List<MediaItem>? by playlist.Items.observe(db)
    val playlist_editor = playlist.rememberEditorOrNull(db)
    
    val apply_item_filter: Boolean by Settings.KEY_FILTER_APPLY_TO_PLAYLIST_ITEMS.rememberMutableState()
    var accent_colour: Color? by remember { mutableStateOf(null) }
    var reorderable: Boolean by remember { mutableStateOf(false) }
    var current_filter: String? by remember { mutableStateOf(null) }
    var current_sort_option: SortOption by remember { mutableStateOf(SortOption.PLAYLIST) }
    val vertical_padding = SpMp.context.getDefaultVerticalPadding()
    val top_padding = padding.calculateTopPadding() + vertical_padding

    LaunchedEffect(playlist) {
        accent_colour = null
    }

    val multiselect_context = remember { MediaItemMultiSelectContext() { context ->
        if (playlist_editor == null) {
            return@MediaItemMultiSelectContext
        }

        // Remove selected items from playlist
        IconButton({ coroutine_scope.launch {
            val selected_items = context.getSelectedItems().sortedByDescending { it.second!! }
            for (item in selected_items) {
                playlist_editor.removeItem(item.second!!)
                context.setItemSelected(item.first, false, item.second)
            }
            playlist_editor.applyItemChanges()
        } }) {
            Icon(Icons.Default.PlaylistRemove, null)
        }
    } }

    Column(Modifier.fillMaxSize()) {
        if (previous_item != null) {
            Row(Modifier.fillMaxWidth().padding(top = top_padding), verticalAlignment = Alignment.CenterVertically) {
                IconButton(close) {
                    Icon(Icons.Default.KeyboardArrowLeft, null)
                }

                Spacer(Modifier.fillMaxWidth().weight(1f))

                val previous_item_title: String? by previous_item.Title.observe(db)
                previous_item_title?.also { Text(it) }

                Spacer(Modifier.fillMaxWidth().weight(1f))

                IconButton({ player.showLongPressMenu(previous_item) }) {
                    Icon(Icons.Default.MoreVert, null)
                }
            }
        }

        // TODO
        // val thumb_item = playlist.getThumbnailHolder().getHolder()

        val sorted_items: MutableList<Pair<MediaItem, Int>> = remember { mutableStateListOf() }
        LaunchedEffect(playlist_items, current_sort_option, current_filter, apply_item_filter) {
            sorted_items.clear()
            playlist_items?.also { items ->
                val filtered_items = current_filter.let { filter ->
                    items.filter { item ->
                        if (filter != null && item.Title.get(db)?.contains(filter, true) != true) {
                            return@filter false
                        }

                        if (apply_item_filter && !isMediaItemHidden(item, db)) {
                            return@filter false
                        }

                        return@filter true
                    }
                }

                sorted_items.addAll(
                    current_sort_option
                        .sortItems(filtered_items, db)
                        .mapIndexed { index, value ->
                            Pair(value, index)
                        }
                )
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
                    playlist_editor!!.moveItem(from - items_above, to - items_above)
                }
            }
        )

        var editing_info by remember { mutableStateOf(false) }
        val horizontal_padding = SpMp.context.getDefaultHorizontalPadding()

        val final_padding = MultiselectAndMusicTopBar(
            multiselect_context,
            Modifier.fillMaxWidth(),
            show_wave_border = false,
            padding = padding.copy(
                top = if (previous_item != null) 0.dp else top_padding,
                start = horizontal_padding,
                end = horizontal_padding,
                bottom = padding.calculateBottomPadding() + vertical_padding
            )
        )

        LazyColumn(
            state = list_state.listState,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.reorderable(list_state),
            contentPadding = final_padding
        ) {
            item {
                PlaylistTopInfo(
                    playlist,
                    accent_colour ?: Theme.accent,
                    editing_info,
                    { editing_info = it }
                ) { image ->
                    accent_colour = image.getThemeColour()
                }
            }

            item {
                PlaylistButtonBar(
                    playlist,
                    accent_colour ?: Theme.accent,
                    editing_info,
                    { editing_info = it }
                )
            }

            stickyHeaderWithTopPadding(
                list_state.listState,
                final_padding.calculateTopPadding(),
                Modifier.zIndex(1f),
                Theme.background_provider
            ) {
                InteractionBar(
                    modifier = Modifier.fillMaxWidth(),
                    playlist = playlist,
                    playlist_editor = playlist_editor,
                    reorderable = reorderable,
                    setReorderable = {
                        reorderable = playlist_editor != null && it
                        if (reorderable) {
                            current_sort_option = SortOption.PLAYLIST
                            current_filter = null
                        }
                        else {
                            coroutine_scope.launch {
                                playlist_editor!!.applyItemChanges().getOrReport("PlaylistPageItemReorder")
                            }
                        }
                    },
                    filter = current_filter,
                    setFilter = {
                        check(!reorderable)
                        current_filter = it
                    },
                    sort_option = current_sort_option,
                    setSortOption = {
                        check(!reorderable)
                        current_sort_option = it
                    }
                )
            }

            PlaylistItems(
                playlist,
                loading,
                list_state,
                sorted_items,
                multiselect_context,
                reorderable,
                current_sort_option,
                player,
                db
            )
        }
    }
}

private fun LazyListScope.PlaylistItems(
    playlist: Playlist,
    loading: Boolean,
    list_state: ReorderableLazyListState,
    sorted_items: List<Pair<MediaItem, Int>>,
    multiselect_context: MediaItemMultiSelectContext,
    reorderable: Boolean,
    sort_option: SortOption,
    player: PlayerState,
    db: Database
) {
    if (sorted_items.isEmpty()) {
        item {
            Crossfade(loading, Modifier.fillMaxWidth()) {
                if (it) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        SubtleLoadingIndicator()
                    }
                }
                else {
                    Text(getString("playlist_empty"), Modifier.padding(top = 15.dp))
                }
            }
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
                    .padding(top = 10.dp)
                    .mediaItemPreviewInteraction(
                        item,
                        long_press_menu_data,
                        onClick = { item, index ->
                            player.playPlaylist(playlist, index!!)
                        }
                    ),
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
                    val item_title: String? by item.Title.observe(db)
                    Text(
                        item_title ?: "",
                        style = MaterialTheme.typography.titleSmall
                    )

                    val item_duration: Long? by item.Duration.observe(db)
                    val duration_text = remember(item_duration) {
                        item_duration?.let { duration ->
                            durationToString(duration, true, hl = SpMp.ui_language)
                        }
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

internal enum class SortOption {
    PLAYLIST, ALPHABET, DURATION, PLAY_COUNT;

    fun getReadable(): String =
        getString(when(this) {
            PLAYLIST ->   "playlist_sort_option_playlist"
            ALPHABET ->   "playlist_sort_option_alphabet"
            DURATION ->   "playlist_sort_option_duration"
            PLAY_COUNT -> "playlist_sort_option_playcount"
        })

    fun sortItems(items: List<MediaItem>, db: Database, reversed: Boolean = false): List<MediaItem> {
        val selector: (MediaItem) -> Comparable<*> = when (this) {
            PLAYLIST ->
                return if (reversed) items.asReversed()
                else items
            ALPHABET -> {
                { it.Title.get(db) ?: "" }
            }
            DURATION -> {
                { if (it is Song) it.Duration.get(db) ?: 0 else 0 }
            }
            PLAY_COUNT -> {
                { SpMp.context.database.getMediaItemPlayCount(it.id) }
            }
        }
        return items.sortedWith(if (reversed) compareByDescending(selector) else compareBy(selector))
    }
}
