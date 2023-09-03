package com.toasterofbread.spmp.ui.component.multiselect

import LocalPlayerState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material.icons.outlined.PushPin
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
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemHolder
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.db.observePinnedToHome
import com.toasterofbread.spmp.model.mediaitem.db.setPinned
import com.toasterofbread.spmp.model.mediaitem.library.MediaItemLibrary
import com.toasterofbread.spmp.model.mediaitem.library.createLocalPlaylist
import com.toasterofbread.spmp.model.mediaitem.playlist.Playlist
import com.toasterofbread.spmp.model.mediaitem.playlist.PlaylistEditor.Companion.getEditorOrNull
import com.toasterofbread.spmp.model.mediaitem.playlist.PlaylistEditor.Companion.isPlaylistEditable
import com.toasterofbread.spmp.platform.composable.PlatformAlertDialog
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.PlaylistSelectMenu
import com.toasterofbread.spmp.ui.theme.Theme
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.getOrReport
import com.toasterofbread.utils.composable.ShapedIconButton
import com.toasterofbread.utils.getContrasted
import com.toasterofbread.utils.lazyAssert
import com.toasterofbread.utils.setAlpha
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

class MediaItemMultiSelectContext(
    val nextRowSelectedItemActions: (@Composable ColumnScope.(MediaItemMultiSelectContext) -> Unit)? = null,
    val selectedItemActions: (@Composable RowScope.(MediaItemMultiSelectContext) -> Unit)? = null
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
        if (Settings.KEY_MULTISELECT_CANCEL_ON_ACTION.get()) {
            setActive(false)
        }
    }

    private fun onSelectedItemsChanged() {
        if (selected_items.isEmpty() && Settings.KEY_MULTISELECT_CANCEL_WHEN_NONE_SELECTED.get()) {
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

    fun setItemSelected(item: MediaItem, selected: Boolean, key: Int? = null) {
        if (selected) {
            if (selected_items.any { it.first == item && it.second == key }) {
                return
            }
            selected_items.add(Pair(item, key))
            onSelectedItemsChanged()
        }
        else if (selected_items.removeIf { it.first == item && it.second == key }) {
            onSelectedItemsChanged()
        }
    }

    @Composable
    fun SelectableItemOverlay(item: MediaItem, modifier: Modifier = Modifier, key: Int? = null) {
        val selected by remember(item, key) { derivedStateOf { isItemSelected(item, key) } }
        val background_colour = LocalContentColor.current.getContrasted().setAlpha(0.5f)

        AnimatedVisibility(selected, modifier, enter = fadeIn(), exit = fadeOut()) {
            Box(Modifier.background(background_colour), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Check, null)
            }
        }
    }

    @Composable
    fun InfoDisplay(modifier: Modifier = Modifier) {
        DisposableEffect(Unit) {
            onDispose {
                if (!is_active) {
                    selected_items.clear()
                }
            }
        }

        Column(modifier.fillMaxWidth().animateContentSize()) {
            Text(
                getString("multiselect_x_items_selected").replace("\$x", selected_items.size.toString()), 
                style = MaterialTheme.typography.labelLarge
            )
            Divider(Modifier.padding(top = 5.dp), color = LocalContentColor.current, thickness = hint_path_thickness)

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                GeneralSelectedItemActions()

                AnimatedVisibility(selected_items.isNotEmpty()) {
                    selectedItemActions?.invoke(this@Row, this@MediaItemMultiSelectContext)
                }
                Spacer(Modifier.fillMaxWidth().weight(1f))

                IconButton({ selected_items.clear() }) {
                    Icon(Icons.Default.Refresh, null)
                }
                IconButton({ setActive(false) }) {
                    Icon(Icons.Default.Close, null)
                }
            }

            nextRowSelectedItemActions?.invoke(this, this@MediaItemMultiSelectContext)
        }
    }

    @Composable
    private fun AddToPlaylistDialog(items: List<Song>, coroutine_scope: CoroutineScope, onFinished: () -> Unit) {
        val player = LocalPlayerState.current

        val selected_playlists = remember { mutableStateListOf<Playlist>() }
        val button_colours = IconButtonDefaults.iconButtonColors(
            containerColor = Theme.accent,
            disabledContainerColor = Theme.accent,
            contentColor = Theme.on_accent,
            disabledContentColor = Theme.on_accent.setAlpha(0.5f)
        )

        fun onPlaylistsSelected() {
            onFinished()
            
            if (selected_playlists.isNotEmpty()) {
                coroutine_scope.launch {
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

        PlatformAlertDialog(
            onDismissRequest = onFinished,
            confirmButton = {
                Row {
                    ShapedIconButton(onFinished, colours = button_colours) {
                        Icon(Icons.Default.Close, null)
                    }

                    Button(
                        {
                            coroutine_scope.launch {
                                val playlist = MediaItemLibrary.createLocalPlaylist(player.context).getOrReport("MultiSelectContextCreateLocalPlaylist")
                                    ?: return@launch
                                selected_playlists.add(playlist)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Theme.accent,
                            contentColor = Theme.on_accent
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
//                CompositionLocalProvider(LocalContentColor provides Theme.accent) {
                    PlaylistSelectMenu(selected_playlists, player.context.ytapi.user_auth_state, Modifier.height(300.dp))
//                }
            }
        )
    }

    @Composable
    private fun RowScope.GeneralSelectedItemActions() {
        val player = LocalPlayerState.current
        val coroutine_scope = rememberCoroutineScope()

        val all_are_pinned =
            if (selected_items.isEmpty()) false
            else selected_items.all { item ->
                item.first.observePinnedToHome(player.context).value
            }

        val any_are_songs by remember { derivedStateOf {
            selected_items.any { it.first is Song }
        } }

        val all_are_editable_playlists by remember { derivedStateOf {
            selected_items.isNotEmpty()
            && selected_items.all {
                it.first is Playlist && (it.first as Playlist).isPlaylistEditable(player.context)
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
        AnimatedVisibility(any_are_songs && selected_items.isNotEmpty()) {
            IconButton({
                for (item in getUniqueSelectedItems()) {
                    if (item is Song) {
                        player.context.download_manager.startDownload(item.id)
                    }
                }
                onActionPerformed()
            }) {
                Icon(Icons.Default.Download, null)
            }
        }

        // Add to playlist
        AnimatedVisibility(any_are_songs && selected_items.isNotEmpty()) {
            IconButton({
                adding_to_playlist = getUniqueSelectedItems().filterIsInstance<Song>()
            }) {
                Icon(Icons.Default.PlaylistAdd, null)
            }
        }

        // Delete playlist
        AnimatedVisibility(all_are_editable_playlists && selected_items.isNotEmpty()) {
            IconButton({
                coroutine_scope.launch {
                   getUniqueSelectedItems().mapNotNull { playlist ->
                        if (playlist !is Playlist) null
                        else launch {
                            val editor = playlist.getEditorOrNull(player.context).getOrNull()
                            editor?.deletePlaylist()?.getOrReport("deletePlaylist")
                        }
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
