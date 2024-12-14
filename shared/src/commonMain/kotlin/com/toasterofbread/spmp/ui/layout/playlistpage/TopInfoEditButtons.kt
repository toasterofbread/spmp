package com.toasterofbread.spmp.ui.layout.playlistpage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.toastbits.composekit.util.composable.AlignableCrossfade
import dev.toastbits.composekit.components.utils.composable.SubtleLoadingIndicator
import com.toasterofbread.spmp.model.mediaitem.playlist.LocalPlaylistData
import com.toasterofbread.spmp.model.mediaitem.playlist.Playlist
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylist
import com.toasterofbread.spmp.model.mediaitem.playlist.downloadAsLocalPlaylist
import com.toasterofbread.spmp.model.mediaitem.playlist.uploadAsAccountPlaylist
import com.toasterofbread.spmp.platform.getOrNotify
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import kotlinx.coroutines.launch
import LocalPlayerState

@Composable
internal fun PlaylistAppPage.PlaylistTopInfoEditButtons(modifier: Modifier = Modifier) {
    val player: PlayerState = LocalPlayerState.current

    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        IconButton({
            finishEdit()
        }) {
            Icon(Icons.Default.Done, null)
        }

        if (playlist_editor != null) {
            var show_thumb_selection by remember { mutableStateOf(false) }

            IconButton({ show_thumb_selection = true }) {
                Icon(Icons.Default.Image, null)
            }

            if (show_thumb_selection) {
                ThumbnailSelectionDialog {
                    show_thumb_selection = false
                }
            }
        }

        val conversion_coroutine_scope = rememberCoroutineScope()
        val auth_state = player.context.ytapi.user_auth_state

        var conversion_in_progress by remember { mutableStateOf(false) }

        AlignableCrossfade(conversion_in_progress, Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { converting ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                if (converting) {
                    SubtleLoadingIndicator(Modifier.padding(end = 5.dp))
                    return@AlignableCrossfade
                }

                if (auth_state != null) {
                    IconButton(
                        {
                            if (conversion_in_progress) {
                                return@IconButton
                            }
                            conversion_coroutine_scope.launch {
                                conversion_in_progress = true

                                val uploaded_playlist: Playlist? = playlist.uploadAsAccountPlaylist(player.context, auth_state)
                                    .getOrNotify(player.context, "ConvertPlaylistToAccountPlaylist")

                                if (uploaded_playlist != null) {
                                    player.openMediaItem(uploaded_playlist, replace_current = true)
                                }

                                conversion_in_progress = false
                            }
                        }
                    ) {
                        Icon(Icons.Default.CloudUpload, null)
                    }
                }

                IconButton({
                    if (conversion_in_progress) {
                        return@IconButton
                    }
                    conversion_coroutine_scope.launch {
                        conversion_in_progress = true

                        val local_playlist: LocalPlaylistData? = playlist.downloadAsLocalPlaylist(player.context)
                            .getOrNotify(player.context, "ConvertPlaylistToLocalPlaylist")

                        if (local_playlist != null) {
                            player.openMediaItem(local_playlist, replace_current = true)
                        }

                        conversion_in_progress = false
                    }
                }) {
                    Icon(if (playlist is RemotePlaylist) Icons.Default.Download else Icons.Default.ContentCopy, null)
                }
            }
        }
    }
}
