package com.toasterofbread.spmp.ui.layout.playlistpage

import LocalPlayerState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemSortType
import com.toasterofbread.spmp.model.mediaitem.isMediaItemHidden
import com.toasterofbread.spmp.model.mediaitem.loader.MediaItemLoader
import com.toasterofbread.spmp.model.mediaitem.loader.loadDataOnChange
import com.toasterofbread.spmp.model.mediaitem.playlist.Playlist
import com.toasterofbread.spmp.model.mediaitem.playlist.PlaylistEditor
import com.toasterofbread.spmp.model.mediaitem.playlist.PlaylistEditor.Companion.getEditorOrNull
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylist
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylistData
import com.toasterofbread.spmp.platform.composable.SwipeRefresh
import com.toasterofbread.spmp.ui.component.MusicTopBar
import com.toasterofbread.spmp.ui.component.WAVE_BORDER_DEFAULT_HEIGHT
import com.toasterofbread.spmp.ui.component.WaveBorder
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.theme.Theme
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.getOrReport
import com.toasterofbread.utils.composable.stickyHeaderWithTopPadding
import com.toasterofbread.utils.copy
import com.toasterofbread.utils.getThemeColour
import com.toasterofbread.utils.thenIf
import kotlinx.coroutines.launch
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable

private enum class LoadType {
    AUTO, REFRESH, CONTINUE
}

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

    val playlist_items: List<MediaItem>? by playlist.Items.observe(db)
    var sorted_items: List<Pair<MediaItem, Int>>? by remember { mutableStateOf(null) }

    var load_type: LoadType by remember { mutableStateOf(LoadType.AUTO) }
    var load_error: Throwable? by remember { mutableStateOf(null) }
    var loaded_data: RemotePlaylistData? by remember { mutableStateOf(null) }
    
    val loading by playlist.loadDataOnChange(
        player.context,
        force = playlist is RemotePlaylist,
        onLoadFailed = { error ->
            load_error = error
        },
        onLoadSucceeded = { data ->
            if (data is RemotePlaylistData) {
                loaded_data = data
            }
        }
    )

    var playlist_editor: PlaylistEditor? by remember { mutableStateOf(null) }
    LaunchedEffect(loading) {
        val playlist_data = loaded_data ?: playlist
        if (playlist_editor?.playlist == playlist_data) {
            return@LaunchedEffect
        }

        val new_editor = playlist_data.getEditorOrNull(player.context).getOrNull()
        if (new_editor != null) {
            playlist_editor?.transferStateTo(new_editor)
        }
        playlist_editor = new_editor
    }

    val apply_item_filter: Boolean by Settings.KEY_FILTER_APPLY_TO_PLAYLIST_ITEMS.rememberMutableState()
    var accent_colour: Color? by remember { mutableStateOf(null) }
    var reorderable: Boolean by remember { mutableStateOf(false) }
    var current_filter: String? by remember { mutableStateOf(null) }

    var playlist_sort_type: MediaItemSortType by remember {
        mutableStateOf(playlist.SortType.get(db) ?: MediaItemSortType.NATIVE)
    }
    fun setPlaylistSortType(sort_type: MediaItemSortType) {
        if (sort_type == playlist_sort_type) {
            return
        }

        playlist_sort_type = sort_type
        coroutine_scope.launch {
            playlist.setSortType(sort_type, player.context)
        }
    }

    LaunchedEffect(playlist.id) {
        accent_colour = null
        playlist_sort_type = playlist.SortType.get(db) ?: MediaItemSortType.NATIVE
    }

    val multiselect_context = remember {
        MediaItemMultiSelectContext() { context ->
            val editor = playlist_editor ?: return@MediaItemMultiSelectContext

            // Remove selected items from playlist
            IconButton({
                coroutine_scope.launch {
                    val selected_items = context.getSelectedItems().sortedByDescending { it.second!! }
                    for (item in selected_items) {
                        editor.removeItem(item.second!!)
                        context.setItemSelected(item.first, false, item.second)
                    }
                    editor.applyItemChanges()
                }
            }) {
                Icon(Icons.Default.PlaylistRemove, null)
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        LaunchedEffect(playlist_items, playlist_sort_type, current_filter, apply_item_filter) {
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

                playlist_sort_type
                    .sortItems(filtered_items, db)
                    .mapIndexed { index, value ->
                        Pair(value, index)
                    }
            }
        }

        val items_above = if (previous_item != null) 4 else 3
        val list_state = rememberReorderableLazyListState(
            onMove = { from, to ->
                check(reorderable)
                check(current_filter == null)
                check(playlist_sort_type == MediaItemSortType.NATIVE)

                if (to.index >= items_above && from.index >= items_above) {
                    sorted_items = sorted_items?.toMutableList()?.apply {
                        add(to.index - items_above, removeAt(from.index - items_above))
                    }
                }
            },
            onDragEnd = { from, to ->
                assert(playlist_editor!!.canMoveItems())
                if (to >= items_above && from >= items_above) {
                    playlist_editor!!.moveItem(from - items_above, to - items_above)
                }
            }
        )

        var editing_info by remember { mutableStateOf(false) }

        var top_bar_showing by remember { mutableStateOf(false) }
        MusicTopBar(
            Settings.KEY_LYRICS_SHOW_IN_SEARCH,
            Modifier.fillMaxWidth().zIndex(2f),
            padding = padding.copy(bottom = 0.dp),
            onShowingChanged = { top_bar_showing = it }
        )

        AnimatedVisibility(top_bar_showing, Modifier.zIndex(1f)) {
            WaveBorder(
                Modifier.thenIf(list_state.listState.firstVisibleItemIndex >= items_above) {
                    alpha(0f)
                }
            )
        }

        val top_padding by animateDpAsState(
            if (top_bar_showing) WAVE_BORDER_DEFAULT_HEIGHT.dp
            else padding.calculateTopPadding()
        )

        val remote_playlist = if (playlist is RemotePlaylist) playlist else null

        SwipeRefresh(
            state = load_type == LoadType.REFRESH && loading && remote_playlist != null,
            onRefresh = {
                remote_playlist?.also { remote ->
                    load_type = LoadType.REFRESH
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
                contentPadding = padding.copy(top = top_padding)
            ) {
                if (previous_item != null) {
                    item {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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
                }

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
                        Modifier.fillMaxWidth(),
                        { editing_info = it }
                    )
                }

                stickyHeaderWithTopPadding(
                    list_state.listState,
                    if (top_bar_showing) 0.dp else top_padding,
                    Modifier.zIndex(1f).padding(bottom = 5.dp),
                    Theme.background_provider
                ) {
                    InteractionBar(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable(remember { MutableInteractionSource() }, indication = null) {},
                        loading = loading && load_type != LoadType.CONTINUE && sorted_items != null,
                        multiselect_context = multiselect_context,
                        list_state = list_state.listState,
                        playlist = playlist,
                        playlist_editor = playlist_editor,
                        reorderable = reorderable,
                        setReorderable = {
                            val editor: PlaylistEditor? = playlist_editor
                            if (editor == null) {
                                reorderable = false
                                return@InteractionBar
                            }

                            reorderable = it

                            if (reorderable) {
                                assert(editor.canMoveItems())
                                setPlaylistSortType(MediaItemSortType.NATIVE)
                                current_filter = null
                            }
                            else {
                                coroutine_scope.launch {
                                    reorderable = false
                                    editor.applyItemChanges().getOrReport("PlaylistPageItemReorder")
                                }
                            }
                        },
                        filter = current_filter,
                        setFilter = {
                            check(!reorderable)
                            current_filter = it
                        },
                        sort_option = playlist_sort_type,
                        setSortOption = { type ->
                            setPlaylistSortType(type)
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
                    playlist_sort_type,
                    player
                )

                item {
                    PlaylistFooter(
                        playlist,
                        sorted_items,
                        loading && load_type != LoadType.REFRESH && sorted_items == null,
                        load_error,
                        Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
