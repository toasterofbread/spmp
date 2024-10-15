package com.toasterofbread.spmp.ui.component.multiselect_context

import LocalPlayerState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.toastbits.composekit.platform.vibrateShort
import dev.toastbits.composekit.utils.composable.PlatformClickableButton
import dev.toastbits.composekit.utils.composable.ShapedIconButton
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
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.layout.PlaylistSelectMenu
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import dev.toastbits.composekit.settings.ui.on_accent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.song_add_to_playlist
import spmp.shared.generated.resources.lpm_action_download
import spmp.shared.generated.resources.toast_playlist_added
import spmp.shared.generated.resources.playlist_create
import spmp.shared.generated.resources.song_add_to_playlist

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
            Icon(Icons.Default.PlaylistAdd, null)
            Text(stringResource(Res.string.song_add_to_playlist))
        }
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
            Icon(Icons.Default.Download, null)
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
        contentColor = player.theme.on_accent,
        disabledContentColor = player.theme.on_accent.copy(alpha = 0.5f)
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
                        contentColor = player.theme.on_accent
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
