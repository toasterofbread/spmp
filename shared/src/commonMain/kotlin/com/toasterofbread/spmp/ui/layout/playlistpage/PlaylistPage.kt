package com.toasterofbread.spmp.ui.layout.playlistpage

import LocalPlayerState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlaylistRemove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemSortOption
import com.toasterofbread.spmp.model.mediaitem.isMediaItemHidden
import com.toasterofbread.spmp.model.mediaitem.loader.MediaItemLoader
import com.toasterofbread.spmp.model.mediaitem.loader.loadDataOnChange
import com.toasterofbread.spmp.model.mediaitem.playlist.Playlist
import com.toasterofbread.spmp.model.mediaitem.playlist.PlaylistEditor.Companion.rememberEditorOrNull
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylist
import com.toasterofbread.spmp.platform.composable.SwipeRefresh
import com.toasterofbread.spmp.platform.getDefaultHorizontalPadding
import com.toasterofbread.spmp.platform.getDefaultVerticalPadding
import com.toasterofbread.spmp.ui.component.MultiselectAndMusicTopBar
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.theme.Theme
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.getOrReport
import com.toasterofbread.utils.composable.stickyHeaderWithTopPadding
import com.toasterofbread.utils.copy
import com.toasterofbread.utils.getThemeColour
import kotlinx.coroutines.launch
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable

@Composable
fun PlaylistPage(
    playlist: Playlist,
    previous_item: MediaItem? = null,
    padding: PaddingValues = PaddingValues(),
    close: () -> Unit
) {
    val player = LocalPlayerState.current
    val db = player.context.database
    val coroutine_scope = rememberCoroutineScope()

    var load_error: Throwable? by remember { mutableStateOf(null) }
    val loading by playlist.loadDataOnChange(player.context) { load_error = it }
    var refreshed by remember { mutableStateOf(false) }

    val playlist_items: List<MediaItem>? by playlist.Items.observe(db)
    var sorted_items: List<Pair<MediaItem, Int>>? by remember { mutableStateOf(null) }
    val playlist_editor = playlist.rememberEditorOrNull(player.context)

    val apply_item_filter: Boolean by Settings.KEY_FILTER_APPLY_TO_PLAYLIST_ITEMS.rememberMutableState()
    var accent_colour: Color? by remember { mutableStateOf(null) }
    var reorderable: Boolean by remember { mutableStateOf(false) }
    var current_filter: String? by remember { mutableStateOf(null) }
    var current_sort_option: MediaItemSortOption by remember { mutableStateOf(MediaItemSortOption.NATIVE) }
    val vertical_padding = player.getDefaultVerticalPadding()
    val top_padding = padding.calculateTopPadding() + vertical_padding

    LaunchedEffect(playlist) {
        accent_colour = null
    }

    val multiselect_context = remember {
        MediaItemMultiSelectContext() { context ->
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
        }
    }

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

        LaunchedEffect(playlist_items, current_sort_option, current_filter, apply_item_filter) {
            sorted_items = playlist_items?.let { items ->
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

                current_sort_option
                    .sortItems(filtered_items, db)
                    .mapIndexed { index, value ->
                        Pair(value, index)
                    }
            }
        }

        val items_above = 3
        val list_state = rememberReorderableLazyListState(
            onMove = { from, to ->
                check(reorderable)
                check(current_filter == null)
                check(current_sort_option == MediaItemSortOption.NATIVE)

                if (to.index >= items_above && from.index >= items_above) {
                    sorted_items = sorted_items?.toMutableList()?.apply {
                        add(to.index - items_above, removeAt(from.index - items_above))
                    }
                }
            },
            onDragEnd = { from, to ->
                if (to >= items_above && from >= items_above) {
                    playlist_editor!!.moveItem(from - items_above, to - items_above)
                }
            }
        )

        var editing_info by remember { mutableStateOf(false) }
        val horizontal_padding = player.getDefaultHorizontalPadding()

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

        val remote_playlist = if (playlist is RemotePlaylist) playlist else null

        SwipeRefresh(
            state = refreshed && loading && remote_playlist != null,
            onRefresh = {
                remote_playlist?.also { remote ->
                    refreshed = true
                    load_error = null
                    coroutine_scope.launch {
                        MediaItemLoader.loadRemotePlaylist(remote.getEmptyData(), player.context)
                    }
                }
            },
            swipe_enabled = !loading,
            modifier = Modifier.fillMaxSize()
        ) {
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
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable(remember { MutableInteractionSource() }, indication = null) {},
                        list_state = list_state.listState,
                        playlist = playlist,
                        playlist_editor = playlist_editor,
                        reorderable = reorderable,
                        setReorderable = {
                            reorderable = playlist_editor != null && it
                            if (reorderable) {
                                current_sort_option = MediaItemSortOption.NATIVE
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
                    player
                )

                item {
                    PlaylistFooter(playlist, sorted_items, loading && !refreshed, load_error, Modifier.fillMaxWidth())
                }
            }
        }
    }
}
