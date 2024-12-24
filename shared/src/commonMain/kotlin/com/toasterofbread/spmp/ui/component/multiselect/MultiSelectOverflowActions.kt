package com.toasterofbread.spmp.ui.component.multiselect_context

import LocalPlayerState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.library.MediaItemLibrary
import com.toasterofbread.spmp.model.mediaitem.library.createLocalPlaylist
import com.toasterofbread.spmp.model.mediaitem.playlist.InteractivePlaylistEditor
import com.toasterofbread.spmp.model.mediaitem.playlist.InteractivePlaylistEditor.Companion.getEditorOrNull
import com.toasterofbread.spmp.model.mediaitem.playlist.LocalPlaylistData
import com.toasterofbread.spmp.model.mediaitem.playlist.Playlist
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.platform.download.DownloadStatus
import com.toasterofbread.spmp.platform.download.rememberSongDownloads
import com.toasterofbread.spmp.platform.getOrNotify
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.layout.PlaylistSelectMenu
import dev.toastbits.composekit.context.vibrateShort
import dev.toastbits.composekit.theme.core.onAccent
import dev.toastbits.composekit.components.utils.composable.PlatformClickableButton
import dev.toastbits.composekit.components.utils.composable.ShapedIconButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.lpm_action_download
import spmp.shared.generated.resources.lpm_action_hide
import spmp.shared.generated.resources.playlist_create
import spmp.shared.generated.resources.song_add_to_playlist
import spmp.shared.generated.resources.toast_playlist_added

@Composable
internal fun ColumnScope.MultiSelectOverflowActions(
    multiselect_context: MediaItemMultiSelectContext,
    additionalSelectedItemActions: (@Composable ColumnScope.(MediaItemMultiSelectContext) -> Unit)?
) {
    val player: PlayerState = LocalPlayerState.current
    val coroutine_scope: CoroutineScope = rememberCoroutineScope()

    val selected_items = multiselect_context.selected_items
    val downloads: List<DownloadStatus> by rememberSongDownloads()

    val any_are_songs: Boolean by remember { derivedStateOf {
        selected_items.any { it.first is Song }
    } }

    val any_are_downloadable: Boolean by remember { derivedStateOf {
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
        AddToPlaylistDialog(multiselect_context, adding, coroutine_scope) { adding_to_playlist = null }
    }

    // Add to playlist
    AnimatedVisibility(any_are_songs) {
        Button({
            adding_to_playlist = multiselect_context.getUniqueSelectedItems().filterIsInstance<Song>()
        }) {
            Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null, Modifier.padding(end = 5.dp))
            Text(stringResource(Res.string.song_add_to_playlist))
        }
    }

    // Hide
    Button({
        player.database.transaction {
            for (item in multiselect_context.getUniqueSelectedItems().filterIsInstance<MediaItem>()) {
                item.Hidden.set(true, player.database)
            }
        }
        multiselect_context.onActionPerformed()
    }) {
        Icon(Icons.Default.VisibilityOff, null, Modifier.padding(end = 5.dp))
        Text(stringResource(Res.string.lpm_action_hide))
    }

    // Download
    AnimatedVisibility(any_are_downloadable) {
        PlatformClickableButton(
            onClick = {
                val songs: List<Song> = multiselect_context.getUniqueSelectedItems().filterIsInstance<Song>()
                player.onSongDownloadRequested(songs)
                multiselect_context.onActionPerformed()
            },
            onAltClick = {
                val songs: List<Song> = multiselect_context.getUniqueSelectedItems().filterIsInstance<Song>()
                player.onSongDownloadRequested(songs, always_show_options = true)
                multiselect_context.onActionPerformed()
                player.context.vibrateShort()
            }
        ) {
            Icon(Icons.Default.Download, null, Modifier.padding(end = 5.dp))
            Text(stringResource(Res.string.lpm_action_download))
        }
    }

    additionalSelectedItemActions?.invoke(this, multiselect_context)
}

@Composable
private fun AddToPlaylistDialog(multiselect_context: MediaItemMultiSelectContext, items: List<Song>, coroutine_scope: CoroutineScope, onFinished: () -> Unit) {
    val player: PlayerState = LocalPlayerState.current

    val selected_playlists: SnapshotStateList<Playlist> = remember { mutableStateListOf() }
    val button_colours = IconButtonDefaults.iconButtonColors(
        containerColor = player.theme.accent,
        disabledContainerColor = player.theme.accent,
        contentColor = player.theme.onAccent,
        disabledContentColor = player.theme.onAccent.copy(alpha = 0.5f)
    )

    fun onPlaylistsSelected() {
        onFinished()

        if (selected_playlists.isNotEmpty()) {
            coroutine_scope.launch(NonCancellable) {
                for (playlist in selected_playlists) {
                    val editor: InteractivePlaylistEditor = playlist.getEditorOrNull(player.context).getOrNull() ?: continue
                    for (item in items) {
                        editor.addItem(item, null)
                    }
                    editor.applyChanges()
                }

                player.context.sendToast(getString(Res.string.toast_playlist_added))
            }
        }

        multiselect_context.onActionPerformed()
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
                            val playlist: LocalPlaylistData =
                                MediaItemLibrary.createLocalPlaylist(player.context).getOrNotify(player.context, "MultiSelectContextCreateLocalPlaylist")
                                ?: return@launch
                            selected_playlists.add(playlist)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = player.theme.accent,
                        contentColor = player.theme.onAccent
                    )
                ) {
                    Text(stringResource(Res.string.playlist_create))
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
            Text(stringResource(Res.string.song_add_to_playlist), style = MaterialTheme.typography.headlineSmall)
        },
        text = {
//                CompositionLocalProvider(LocalContentColor provides context.theme.accent) {
                PlaylistSelectMenu(selected_playlists, player.context.ytapi.user_auth_state, Modifier.height(300.dp))
//                }
        }
    )
}
