package com.toasterofbread.spmp.ui.layout.playlistpage

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlaylistRemove
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import dev.toastbits.composekit.components.platform.composable.SwipeRefresh
import dev.toastbits.composekit.util.getThemeColour
import dev.toastbits.composekit.components.utils.composable.ScrollBarLazyColumnWithHeader
import com.toasterofbread.spmp.db.Database
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemHolder
import com.toasterofbread.spmp.model.mediaitem.MediaItemSortType
import com.toasterofbread.spmp.model.mediaitem.db.isMediaItemHidden
import com.toasterofbread.spmp.model.mediaitem.loader.MediaItemLoader
import com.toasterofbread.spmp.model.mediaitem.loader.MediaItemThumbnailLoader
import com.toasterofbread.spmp.model.mediaitem.loader.loadDataOnChange
import com.toasterofbread.spmp.model.mediaitem.playlist.LocalPlaylistData
import com.toasterofbread.spmp.model.mediaitem.playlist.Playlist
import com.toasterofbread.spmp.model.mediaitem.playlist.PlaylistData
import com.toasterofbread.spmp.model.mediaitem.playlist.InteractivePlaylistEditor
import com.toasterofbread.spmp.model.mediaitem.playlist.InteractivePlaylistEditor.Companion.getEditorOrNull
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylist
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylistData
import com.toasterofbread.spmp.platform.getOrNotify
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.layout.apppage.AppPageState
import com.toasterofbread.spmp.ui.layout.apppage.AppPageWithItem
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.burnoutcrew.reorderable.ReorderableLazyListState
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import LocalPlayerState
import PlatformIO
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.song_remove_from_playlist

private enum class LoadType {
    AUTO, REFRESH, CONTINUE
}

// Dear future me: please organise this
class PlaylistAppPage(
    override val state: AppPageState,
    playlist: Playlist
): AppPageWithItem() {
    override val item: MediaItemHolder = playlist

    private var previous_item: MediaItemHolder? by mutableStateOf(null)
    override fun onOpened(from_item: MediaItemHolder?) {
        super.onOpened(from_item)
        previous_item = from_item
    }

    private var load_type: LoadType by mutableStateOf(LoadType.AUTO)
    private var load_error: Throwable? by mutableStateOf(null)

    private var item_load_state: State<Boolean>? by mutableStateOf(null)
    private var items_reload_key: Boolean by mutableStateOf(false)

    var edit_in_progress: Boolean by mutableStateOf(false)
        private set

    var playlist: Playlist by mutableStateOf(playlist)
        private set
    var loaded_playlist: PlaylistData? by mutableStateOf(null)

    var edited_title: String by mutableStateOf("")
    var edited_image_width: Float? by mutableStateOf(null)
    var edited_image_url: String? by mutableStateOf(null)
        private set

    fun setEditedImageUrl(image_url: String?) {
        coroutine_scope.launch {
            MediaItemThumbnailLoader.invalidateCache(playlist, state.context)
            edited_image_url = image_url
        }
    }

    var reordering: Boolean by mutableStateOf(false)
        private set
    var current_filter: String? by mutableStateOf(null)
        private set
    var sort_type: MediaItemSortType by mutableStateOf(playlist.SortType.get(state.context.database) ?: MediaItemSortType.NATIVE)
        private set

    var accent_colour: Color? by mutableStateOf(null)
    val coroutine_scope: CoroutineScope = CoroutineScope(Job())

    var playlist_editor: InteractivePlaylistEditor? by mutableStateOf(null)
    val multiselect_context = MediaItemMultiSelectContext(state.context) { context ->
        val editor: InteractivePlaylistEditor = playlist_editor ?: return@MediaItemMultiSelectContext

        // Remove selected items from playlist
        Button({
            coroutine_scope.launch {
                val selected_items = context.getSelectedItems().sortedByDescending { it.second!! }
                for (item in selected_items) {
                    editor.removeItem(item.second!!)
                    context.setItemSelected(item.first, false, item.second)
                }
                editor.applyChanges()
                items_reload_key = !items_reload_key
            }
        }) {
            Icon(Icons.Default.PlaylistRemove, null)
            Text(stringResource(Res.string.song_remove_from_playlist))
        }
    }

    override fun canReload(): Boolean = true

    override fun onReload() {
        load_error = null

        val remote_playlist: RemotePlaylist? = playlist as? RemotePlaylist
        if (remote_playlist == null) {
            items_reload_key = !items_reload_key
            return
        }

        load_type = LoadType.REFRESH
        coroutine_scope.launch {
            MediaItemLoader.loadRemotePlaylist(remote_playlist.getEmptyData(), state.context)
                .fold(
                    { loaded_playlist = it },
                    { load_error = it }
                )
        }
    }

    @Composable
    override fun isReloading(): Boolean = item_load_state?.value == true

    fun getAccentColour(): Color =
        accent_colour ?: state.player.theme.accent

    fun beginEdit() {
        edited_title = playlist.getActiveTitle(state.context.database) ?: ""
        edited_image_width = playlist.ImageWidth.get(state.context.database)
        edited_image_url = playlist.CustomImageUrl.get(state.context.database)
        edit_in_progress = true
    }

    fun finishEdit() = coroutine_scope.launch(Dispatchers.PlatformIO) {
        edit_in_progress = false

        var changes_made: Boolean = false

        val editor: InteractivePlaylistEditor? = playlist_editor
        val data: PlaylistData? = if (editor == null) playlist.getEmptyData() else null

        if (edited_title != playlist.getActiveTitle(state.context.database)) {
            if (editor != null) {
                editor.setTitle(edited_title)
            }
            else {
                data!!.setDataActiveTitle(edited_title)
            }
            changes_made = true
        }
        if (edited_image_width != playlist.ImageWidth.get(state.context.database)) {
            if (editor != null) {
                editor.setImageWidth(edited_image_width)
            }
            else {
                data!!.image_width = edited_image_width
            }
            changes_made = true
        }

        if (edited_image_url != playlist.CustomImageUrl.get(state.context.database)) {
            if (editor != null) {
                editor.setImage(edited_image_url)
            }
            else {
                data!!.custom_image_url = edited_image_url
            }
            changes_made = true
        }

        if (!changes_made) {
            return@launch
        }

        if (editor != null) {
            editor.applyChanges(exclude_item_changes = true)
            items_reload_key = !items_reload_key
        }
        else {
            data!!.savePlaylist(state.context)
        }

        // Replace LocalPlaylistData object with a copy to force recomposition, as LocalPlaylistData is not observable
        val local_playlist = playlist as? LocalPlaylistData
        if (local_playlist != null) {
            val replacement = LocalPlaylistData(local_playlist.id)

            local_playlist.populateData(replacement, state.context.database)
            replacement.name = edited_title
            replacement.custom_image_url = edited_image_url
            replacement.image_width = edited_image_width

            playlist = replacement
        }
    }

    fun setReorderable(value: Boolean) {
        val editor: InteractivePlaylistEditor? = playlist_editor
        if (editor == null) {
            reordering = false
            return
        }

        reordering = value

        if (reordering) {
            setSortType(MediaItemSortType.NATIVE)
            current_filter = null
        }
        else {
            coroutine_scope.launch {
                reordering = false
                editor.applyChanges().getOrNotify(state.context, "PlaylistPageItemReorder")
                items_reload_key = !items_reload_key
            }
        }
    }

    fun setCurrentFilter(value: String?) {
        check(!reordering)
        current_filter = value
    }

    fun setSortType(value: MediaItemSortType) {
        if (value == sort_type) {
            return
        }

        sort_type = value
        coroutine_scope.launch {
            playlist.setSortType(sort_type, state.context)
        }
    }

    fun onThumbnailLoaded(image: ImageBitmap?) {
        accent_colour = image?.getThemeColour()
    }

    @Composable
    override fun ColumnScope.Page(
        multiselect_context: MediaItemMultiSelectContext,
        modifier: Modifier,
        content_padding: PaddingValues,
        close: () -> Unit
    ) {
        val player: PlayerState = LocalPlayerState.current
        val db: Database = player.database

        val playlist_items: List<MediaItem>? by playlist.Items.observe(db, key = items_reload_key)
        var sorted_items: List<MediaItem>? by remember { mutableStateOf(null) }

        val load_state: State<Boolean> = playlist.loadDataOnChange(
            player.context,
            onLoadFailed = { error ->
                load_error = error
            },
            onLoadSucceeded = { data ->
                if (data is PlaylistData) {
                    loaded_playlist = data
                }
            }
        )
        item_load_state = load_state

        val loading: Boolean by load_state

        val playlist_data: Playlist = loaded_playlist ?: playlist

        LaunchedEffect(loaded_playlist) {
            if (loaded_playlist == null || playlist_editor?.playlist == loaded_playlist) {
                return@LaunchedEffect
            }

            val new_editor: InteractivePlaylistEditor? =
                loaded_playlist?.getEditorOrNull(player.context)?.fold(
                    { it },
                    {
                        load_error = it
                        it.printStackTrace()
                        return@LaunchedEffect
                    }
                )

            if (new_editor != null) {
                playlist_editor?.transferStateTo(new_editor)
            }
            playlist_editor = new_editor
        }

        val apply_item_filter: Boolean by player.settings.Filter.APPLY_TO_PLAYLIST_ITEMS.observe()

        LaunchedEffect(playlist_items, sort_type, current_filter, apply_item_filter) {
            sorted_items = playlist_items?.let { items ->
                val filtered_items = current_filter.let { filter ->
                    items.filter { item ->
                        if (filter != null && item.getActiveTitle(db)?.contains(filter, true) != true) {
                            return@filter false
                        }

                        if (apply_item_filter && !isMediaItemHidden(item, player.context)) {
                            return@filter false
                        }

                        return@filter true
                    }
                }

                sort_type.sortItems(filtered_items, db)
            }
        }

        Column(modifier) {
            val items_above: Int = if (previous_item != null) 4 else 3
            val list_state: ReorderableLazyListState = rememberReorderableLazyListState(
                onMove = { from, to ->
                    check(reordering)
                    check(current_filter == null)
                    check(sort_type == MediaItemSortType.NATIVE)

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

            val remote_playlist: RemotePlaylist? = playlist as? RemotePlaylist

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
                indicator_padding = PaddingValues(top = content_padding.calculateTopPadding()),
                modifier = Modifier.fillMaxSize()
            ) {
                ScrollBarLazyColumnWithHeader(
                    header_index = if (previous_item == null) 2 else 3,
                    getHeaderBackgroundColour = { player.theme.background },
                    state = list_state.listState,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.reorderable(list_state),
                    contentPadding = content_padding,
                    headerContent = {
                        PlaylistInteractionBar(
                            sorted_items,
                            loading = loading && load_type != LoadType.CONTINUE && sorted_items != null,
                            list_state = list_state.listState,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable(remember { MutableInteractionSource() }, indication = null) {}
                        )
                    }
                ) { headerContent ->
                    previous_item?.item?.also { prev ->
                        item {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(close) {
                                    Icon(Icons.AutoMirrored.Default.KeyboardArrowLeft, null)
                                }

                                Spacer(Modifier.fillMaxWidth().weight(1f))

                                val previous_item_title: String? by prev.observeActiveTitle()
                                previous_item_title?.also { Text(it) }

                                Spacer(Modifier.fillMaxWidth().weight(1f))

                                IconButton({ player.showLongPressMenu(prev) }) {
                                    Icon(Icons.Default.MoreVert, null)
                                }
                            }
                        }
                    }

                    item {
                        PlaylistTopInfo(sorted_items)
                    }

                    item {
                        PlaylistButtonBar(Modifier.fillMaxWidth())
                    }

                    item {
                        headerContent()
                    }

                    PlaylistItems(
                        playlist,
                        this,
                        list_state,
                        sorted_items
                    )

                    item {
                        PlaylistFooter(
                            sorted_items,
                            getAccentColour(),
                            loading && (sorted_items == null || load_type != LoadType.REFRESH),
                            load_error,
                            Modifier.fillMaxWidth().padding(top = 15.dp),
                            onRetry =
                                remote_playlist?.let { remote ->
                                    {
                                        load_type = LoadType.REFRESH
                                        load_error = null
                                        coroutine_scope.launch {
                                            MediaItemLoader.loadRemotePlaylist(remote.getEmptyData(), player.context)
                                        }
                                    }
                                },
                            onContinue =
                                (playlist_data as? RemotePlaylistData)?.let { remote_data ->
                                    { continuation ->
                                        load_type = LoadType.CONTINUE
                                        load_error = null
                                        coroutine_scope.launch {
                                            MediaItemLoader.loadRemotePlaylist(remote_data, player.context, continuation = continuation)
                                        }
                                    }
                                }
                        )
                    }
                }
            }
        }
    }
}
