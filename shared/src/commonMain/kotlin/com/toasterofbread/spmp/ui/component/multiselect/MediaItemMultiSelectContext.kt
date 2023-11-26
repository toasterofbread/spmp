package com.toasterofbread.spmp.ui.component.multiselect

import LocalPlayerState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toasterofbread.composekit.platform.composable.BackHandler
import com.toasterofbread.composekit.utils.common.getContrasted
import com.toasterofbread.composekit.utils.common.lazyAssert
import com.toasterofbread.composekit.utils.composable.ScrollabilityIndicatorRow
import com.toasterofbread.composekit.utils.composable.ShapedIconButton
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemHolder
import com.toasterofbread.spmp.model.mediaitem.db.observePinnedToHome
import com.toasterofbread.spmp.model.mediaitem.db.setPinned
import com.toasterofbread.spmp.model.mediaitem.library.MediaItemLibrary
import com.toasterofbread.spmp.model.mediaitem.library.createLocalPlaylist
import com.toasterofbread.spmp.model.mediaitem.playlist.LocalPlaylist
import com.toasterofbread.spmp.model.mediaitem.playlist.Playlist
import com.toasterofbread.spmp.model.mediaitem.playlist.PlaylistEditor.Companion.getEditorOrNull
import com.toasterofbread.spmp.model.mediaitem.playlist.PlaylistEditor.Companion.isPlaylistEditable
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.settings.category.BehaviourSettings
import com.toasterofbread.spmp.platform.download.DownloadStatus
import com.toasterofbread.spmp.platform.download.PlayerDownloadManager
import com.toasterofbread.spmp.platform.download.rememberSongDownloads
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.component.multiselect_context.MultiSelectSelectedItemActions
import com.toasterofbread.spmp.ui.layout.PlaylistSelectMenu
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.getOrReport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

class MediaItemMultiSelectContext(
    val additionalSelectedItemActions: (@Composable RowScope.(MediaItemMultiSelectContext) -> Unit)? = null
) {
    private val selected_items: MutableList<Pair<MediaItem, Int?>> = mutableStateListOf()
    var is_active: Boolean by mutableStateOf(false)
        private set

    @Composable
    fun getActiveHintBorder(): BorderStroke? = if (is_active) BorderStroke(hint_path_thickness, LocalContentColor.current) else null
    private val hint_path_thickness = 0.5.dp

    fun setActive(value: Boolean) {
        if (value) {
            selected_items.clear()
        }
        is_active = value
    }

    fun isItemSelected(item: MediaItem, key: Int? = null): Boolean {
        if (!is_active) {
            return false
        }
        return selected_items.any { it.first == item && it.second == key }
    }

    fun onActionPerformed() {
        if (BehaviourSettings.Key.MULTISELECT_CANCEL_ON_ACTION.get()) {
            setActive(false)
        }
    }

    private fun onSelectedItemsChanged() {
        if (selected_items.isEmpty() && BehaviourSettings.Key.MULTISELECT_CANCEL_ON_NONE_SELECTED.get()) {
            setActive(false)
        }
    }

    fun getSelectedItems(): List<Pair<MediaItem, Int?>> = selected_items
    fun getUniqueSelectedItems(): Set<MediaItem> = selected_items.map { it.first }.toSet()

    fun updateKey(index: Int, key: Int?) {
        selected_items[index] = selected_items[index].copy(second = key)
        lazyAssert(condition = ::areItemsValid)
    }

    fun toggleItem(item: MediaItem, key: Int? = null) {
        val iterator = selected_items.iterator()
        while (iterator.hasNext()) {
            iterator.next().apply {
                if (first == item && second == key) {
                    iterator.remove()
                    onSelectedItemsChanged()
                    return
                }
            }
        }

        selected_items.add(Pair(item, key))
        onSelectedItemsChanged()
    }

    fun setItemSelected(item: MediaItem, selected: Boolean, key: Int? = null): Boolean {
        if (selected) {
            if (!is_active) {
                setActive(true)
            }

            if (selected_items.any { it.first == item && it.second == key }) {
                return false
            }
            selected_items.add(Pair(item, key))
            onSelectedItemsChanged()
            return true
        }
        else if (selected_items.removeIf { it.first == item && it.second == key }) {
            onSelectedItemsChanged()
            return true
        }
        else {
            return false
        }
    }

    @Composable
    fun SelectableItemOverlay(item: MediaItem, modifier: Modifier = Modifier, key: Int? = null) {
        val selected by remember(item, key) { derivedStateOf { isItemSelected(item, key) } }
        val background_colour = LocalContentColor.current.getContrasted().copy(alpha = 0.5f)

        AnimatedVisibility(selected, modifier, enter = fadeIn(), exit = fadeOut()) {
            Box(Modifier.background(background_colour), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Check, null)
            }
        }
    }

    @Composable
    fun InfoDisplay(modifier: Modifier = Modifier, getAllSelectableItems: (() -> List<Pair<MediaItem, Int?>>)? = null) {
        DisposableEffect(Unit) {
            onDispose {
                if (!is_active) {
                    selected_items.clear()
                }
            }
        }

        BackHandler(is_active) {
            setActive(false)
        }

        Column(modifier.fillMaxWidth().animateContentSize()) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    getString("multiselect_x_items_selected").replace("\$x", selected_items.size.toString()), 
                    style = MaterialTheme.typography.labelLarge
                )

                getAllSelectableItems?.also { getSelectable ->
                    IconButton(
                        {
                            var changed = false
                            for (item in getSelectable()) {
                                if (setItemSelected(item.first, true, item.second)) {
                                    changed = true
                                }
                            }

                            if (!changed) {
                                selected_items.clear()
                            }
                        },
                        Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.SelectAll, null)
                    }
                }
            }

            Divider(Modifier.padding(top = 5.dp), color = LocalContentColor.current, thickness = hint_path_thickness)

            val scroll_state = rememberScrollState()
            ScrollabilityIndicatorRow(scroll_state, Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                Row(Modifier.fillMaxWidth().horizontalScroll(scroll_state), verticalAlignment = Alignment.CenterVertically) {
                    GeneralSelectedItemActions()

                    AnimatedVisibility(selected_items.isNotEmpty()) {
                        Row(Modifier.fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
                            MultiSelectSelectedItemActions(this@MediaItemMultiSelectContext, additionalSelectedItemActions)
                        }
                    }
                    Spacer(Modifier.fillMaxWidth().weight(1f))

                    IconButton({ selected_items.clear() }) {
                        Icon(Icons.Default.Refresh, null)
                    }
                    IconButton({ setActive(false) }) {
                        Icon(Icons.Default.Close, null)
                    }
                }
            }

            MultiSelectNextRowActions(this@MediaItemMultiSelectContext)
        }
    }

    @Composable
    private fun AddToPlaylistDialog(items: List<Song>, coroutine_scope: CoroutineScope, onFinished: () -> Unit) {
        val player = LocalPlayerState.current

        val selected_playlists = remember { mutableStateListOf<Playlist>() }
        val button_colours = IconButtonDefaults.iconButtonColors(
            containerColor = player.theme.accent,
            disabledContainerColor = player.theme.accent,
            contentColor = player.theme.on_accent,
            disabledContentColor = player.theme.on_accent.copy(alpha = 0.5f)
        )

        fun onPlaylistsSelected() {
            onFinished()
            
            if (selected_playlists.isNotEmpty()) {
                coroutine_scope.launch(NonCancellable) {
                    for (playlist in selected_playlists) {
                        val editor = playlist.getEditorOrNull(player.context).getOrNull() ?: continue
                        for (item in items) {
                            editor.addItem(item, null)
                        }
                        editor.applyChanges()
                    }

                    player.context.sendToast(getString("toast_playlist_added"))
                }
            }

            onActionPerformed()
        }

        AlertDialog(
            onDismissRequest = onFinished,
            confirmButton = {
                Row {
                    ShapedIconButton(onFinished, colours = button_colours) {
                        Icon(Icons.Default.Close, null)
                    }

                    Button(
                        {
                            coroutine_scope.launch {
                                val playlist = MediaItemLibrary.createLocalPlaylist(player.context).getOrReport(player.context, "MultiSelectContextCreateLocalPlaylist")
                                    ?: return@launch
                                selected_playlists.add(playlist)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = player.theme.accent,
                            contentColor = player.theme.on_accent
                        )
                    ) {
                        Text(getString("playlist_create"))
                    }

                    ShapedIconButton(
                        { onPlaylistsSelected() },
                        colours = button_colours,
                        enabled = selected_playlists.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Done, null)
                    }
                }
            },
            title = {
                Text(getString("song_add_to_playlist"), style = MaterialTheme.typography.headlineSmall)
            },
            text = {
//                CompositionLocalProvider(LocalContentColor provides context.theme.accent) {
                    PlaylistSelectMenu(selected_playlists, player.context.ytapi.user_auth_state, Modifier.height(300.dp))
//                }
            }
        )
    }

    @Composable
    private fun RowScope.GeneralSelectedItemActions() {
        val player = LocalPlayerState.current
        val coroutine_scope = rememberCoroutineScope()
        val downloads: List<DownloadStatus> by rememberSongDownloads()

        val all_are_pinned: Boolean =
            if (selected_items.isEmpty()) false
            else selected_items.all { item ->
                item.first.observePinnedToHome().value
            }

        val any_are_songs: Boolean by remember { derivedStateOf {
            selected_items.any { it.first is Song }
        } }

        val all_are_editable_playlists: Boolean by remember { derivedStateOf {
            selected_items.isNotEmpty()
            && selected_items.all { item ->
                item.first is Playlist && (item.first as Playlist).isPlaylistEditable(player.context)
            }
        } }

        val all_are_deletable by remember { derivedStateOf {
            if (all_are_editable_playlists) {
                return@derivedStateOf true
            }

            return@derivedStateOf (
                selected_items.isNotEmpty()
                && selected_items.all { item ->
                    when (item.first) {
                        is LocalPlaylist -> true
                        is Playlist -> (item.first as Playlist).isPlaylistEditable(player.context)
                        is Song -> downloads.firstOrNull { it.song.id == item.first.id }?.isCompleted() == true
                        else -> false
                    }
                }
            )
        } }

        val any_are_downloadable by remember { derivedStateOf {
            selected_items.any { item ->
                if (item.first !is Song) {
                    return@any false
                }

                val download: DownloadStatus? = downloads.firstOrNull { it.song.id == item.first.id }
                return@any download?.isCompleted() != true
            }
        } }

        var adding_to_playlist: List<Song>? by remember { mutableStateOf(null) }
        adding_to_playlist?.also { adding ->
            AddToPlaylistDialog(adding, coroutine_scope) { adding_to_playlist = null }
        }

        // Pin
        AnimatedVisibility(selected_items.isNotEmpty()) {
            IconButton({
                all_are_pinned.also { pinned ->
                    player.database.transaction {
                        for (item in getUniqueSelectedItems()) {
                            item.setPinned(!pinned, player.context)
                        }
                    }
                }
                onActionPerformed()
            }) {
                Icon(if (all_are_pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin, null)
            }
        }

        // Download
        AnimatedVisibility(any_are_downloadable) {
            IconButton({
                val songs: List<Song> = getUniqueSelectedItems().filterIsInstance<Song>()
                player.onSongDownloadRequested(songs)
                onActionPerformed()
            }) {
                Icon(Icons.Default.Download, null)
            }
        }

        // Add to playlist
        AnimatedVisibility(any_are_songs) {
            IconButton({
                adding_to_playlist = getUniqueSelectedItems().filterIsInstance<Song>()
            }) {
                Icon(Icons.Default.PlaylistAdd, null)
            }
        }

        // Delete playlist / local song
        AnimatedVisibility(all_are_deletable) {
            IconButton({
                coroutine_scope.launch {
                    getUniqueSelectedItems().mapNotNull { item ->
                        if (item is Playlist) {
                            launch {
                                val editor = item.getEditorOrNull(player.context).getOrNull()
                                editor?.deletePlaylist()?.getOrReport(player.context, "deletePlaylist")
                            }
                        }
                        else if (item is Song) {
                            launch {
                                player.context.download_manager.deleteSongLocalAudioFile(item)
                            }
                        }
                        else null
                    }.joinAll()
                    onActionPerformed()
                }
            }) {
                Icon(Icons.Default.Delete, null)
            }
        }
    }

    @Composable
    fun CollectionToggleButton(items: List<MediaItemHolder>) {
        AnimatedVisibility(is_active) {
            val all_selected by remember { derivedStateOf {
                items.all {
                    it.item?.let { item -> isItemSelected(item) } ?: false
                }
            } }
            Crossfade(all_selected) { selected ->
                IconButton({
                    for (item in items) {
                        item.item?.also {
                            setItemSelected(it, !selected)
                        }
                    }
                }) {
                    Icon(if (selected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked, null)
                }
            }
        }
    }

    private fun areItemsValid(): Boolean {
        val keys = mutableMapOf<MediaItem, MutableList<Int?>>()
        for (item in selected_items) {
            val item_keys = keys[item.first]
            if (item_keys == null) {
                keys[item.first] = mutableListOf(item.second)
                continue
            }

            if (item_keys.contains(item.second)) {
                return false
            }
            item_keys.add(item.second)
        }
        return true
    }
}
