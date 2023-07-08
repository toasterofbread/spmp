package com.toasterofbread.spmp.ui.layout.playlistpage

import LocalPlayerState
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.api.getOrReport
import com.toasterofbread.spmp.model.mediaitem.LocalPlaylist
import com.toasterofbread.spmp.model.mediaitem.MediaItemPreviewParams
import com.toasterofbread.spmp.model.mediaitem.Playlist
import com.toasterofbread.spmp.platform.composable.PlatformAlertDialog
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.utils.composable.SubtleLoadingIndicator
import com.toasterofbread.utils.getContrasted
import kotlinx.coroutines.launch

@Composable
internal fun TopInfoEditButtons(playlist: Playlist, accent_colour: Color, modifier: Modifier = Modifier, onFinished: () -> Unit) {
    Row(modifier) {
        IconButton(onFinished) {
            Icon(Icons.Default.Done, null)
        }

        if (playlist.is_editable == true) {
            var show_thumb_selection by remember { mutableStateOf(false) }

            IconButton({ show_thumb_selection = true }) {
                Icon(Icons.Default.Image, null)
            }

            if (show_thumb_selection) {
                PlatformAlertDialog(
                    onDismissRequest = { show_thumb_selection = false },
                    confirmButton = {
                        if (playlist.playlist_reg_entry.image_item_uid != null) {
                            IconButton({
                                playlist.playlist_reg_entry.image_item_uid = null
                                show_thumb_selection = false
                            }) {
                                Icon(Icons.Default.Refresh, null)
                            }
                        }
                    },
                    dismissButton = {
                        Button({ show_thumb_selection = false }) {
                            Text(getString("action_cancel"))
                        }
                    },
                    title = { Text(getString("playlist_select_image"), style = MaterialTheme.typography.headlineSmall) },
                    text = {
                        val player = LocalPlayerState.current
                        val playlist_items = playlist.items ?: emptyList()

                        if (playlist_items.isEmpty()) {
                            Text(getString("playlist_empty"))
                        } else {
                            CompositionLocalProvider(LocalPlayerState provides remember {
                                player.copy(
                                    onClickedOverride = { item, _ ->
                                        playlist.playlist_reg_entry.image_item_uid = item.uid
                                        show_thumb_selection = false
                                    }
                                )
                            }) {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    items(playlist_items) { item ->
                                        item.PreviewLong(MediaItemPreviewParams())
                                    }
                                }
                            }
                        }
                    }
                )
            }
        }

        if (playlist is LocalPlaylist) {
            val coroutine_scope = rememberCoroutineScope()
            var converting by remember { mutableStateOf(false) }

            Spacer(Modifier.fillMaxWidth().weight(1f))

            Button(
                {
                    if (converting) {
                        return@Button
                    }
                    coroutine_scope.launch {
                        converting = true
                        playlist.convertToAccountPlaylist().getOrReport("ConvertPlaylistToAccountPlaylist")
                        converting = false
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent_colour,
                    contentColor = accent_colour.getContrasted()
                )
            ) {
                Text(getString("playlist_upload_to_account"))
                Spacer(Modifier.width(10.dp))
                Crossfade(converting) { active ->
                    if (active) {
                        SubtleLoadingIndicator(Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Default.CloudUpload, null)
                    }
                }
            }
        }
    }
}
