package com.toasterofbread.spmp.ui.component.longpressmenu.playlist

import LocalPlayerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.playlist.InteractivePlaylistEditor.Companion.rememberEditorOrNull
import com.toasterofbread.spmp.model.mediaitem.playlist.Playlist
import com.toasterofbread.spmp.ui.component.longpressmenu.LongPressMenuActionProvider
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.lpm_action_play
import spmp.shared.generated.resources.lpm_action_play_after_1_song
import spmp.shared.generated.resources.lpm_action_play_after_x_songs
import spmp.shared.generated.resources.lpm_action_shuffle_playlist
import spmp.shared.generated.resources.playlist_delete

@Composable
fun LongPressMenuActionProvider.PlaylistLongPressMenuActions(playlist: MediaItem) {
    require(playlist is Playlist)

    val player = LocalPlayerState.current
    val coroutine_context = rememberCoroutineScope()

    ActionButton(
        Icons.Default.PlayArrow, stringResource(Res.string.lpm_action_play),
        onClick = {
            player.playMediaItem(playlist)
        }
    )

    ActionButton(
        Icons.Default.Shuffle, stringResource(Res.string.lpm_action_shuffle_playlist),
        onClick = {
            player.playMediaItem(playlist, true)
        }
    )

    ActiveQueueIndexAction(
        { distance ->
            stringResource(if (distance == 1) Res.string.lpm_action_play_after_1_song else Res.string.lpm_action_play_after_x_songs).replace("\$x", distance.toString())
        },
        onClick = { active_queue_index ->
            player.playMediaItem(playlist, at_index = active_queue_index + 1)
        },
        onLongClick = { active_queue_index ->
            player.playMediaItem(playlist, at_index = active_queue_index + 1, shuffle = true)
        }
    )

    val playlist_editor by playlist.rememberEditorOrNull(player.context)
    if (playlist_editor != null) {
        ActionButton(
            Icons.Default.Delete,
            stringResource(Res.string.playlist_delete),
            onClick = {
                playlist_editor?.also { editor ->
                    coroutine_context.launch {
                        editor.performDeletion().getOrThrow()
                    }
                }
            }
        )
    }
}
