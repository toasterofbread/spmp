package com.toasterofbread.spmp.ui.layout.playlistpage

import LocalPlayerState
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemThumbnailProvider
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylist
import com.toasterofbread.spmp.model.mediaitem.playlist.Playlist
import com.toasterofbread.spmp.model.mediaitem.playlist.PlaylistEditor.Companion.rememberEditorOrNull
import com.toasterofbread.spmp.model.mediaitem.playlist.downloadAsLocalPlaylist
import com.toasterofbread.spmp.model.mediaitem.playlist.uploadAsAccountPlaylist
import com.toasterofbread.spmp.model.mediaitem.toThumbnailProvider
import com.toasterofbread.spmp.platform.composable.PlatformAlertDialog
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.component.mediaitempreview.MediaItemPreviewLong
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.getOrReport
import com.toasterofbread.utils.composable.SubtleLoadingIndicator
import kotlinx.coroutines.launch

@Composable
internal fun TopInfoEditButtons(playlist: Playlist, accent_colour: Color, modifier: Modifier = Modifier, onFinished: () -> Unit) {
    val player = LocalPlayerState.current
    val db = player.context.database

    var playlist_image_provider: MediaItemThumbnailProvider? by playlist.CustomImageProvider.observe(db)
    val playlist_editor = playlist.rememberEditorOrNull(player.context)
    
    Row(modifier) {
        IconButton(onFinished) {
            Icon(Icons.Default.Done, null)
        }

        if (playlist_editor != null) {
            var show_thumb_selection by remember { mutableStateOf(false) }

            IconButton({ show_thumb_selection = true }) {
                Icon(Icons.Default.Image, null)
            }

            if (show_thumb_selection) {
                PlatformAlertDialog(
                    onDismissRequest = { show_thumb_selection = false },
                    confirmButton = {
                        if (playlist_image_provider != null) {
                            IconButton({
                                playlist_image_provider = null
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
                        val playlist_items: List<MediaItem>? by playlist.Items.observe(db)
                        playlist_items.also { items ->
                            if (items.isNullOrEmpty()) {
                                Text(getString("playlist_empty"))
                            }
                            else {
                                CompositionLocalProvider(LocalPlayerState provides remember {
                                    player.copy(
                                        onClickedOverride = { item, _ ->
                                            playlist_image_provider = db.mediaItemQueries
                                                .thumbnailProviderById(item.id)
                                                .executeAsOneOrNull()
                                                ?.toThumbnailProvider()

                                            show_thumb_selection = false
                                        }
                                    )
                                }) {
                                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        items(items) { item ->
                                            MediaItemPreviewLong(item)
                                        }
                                    }
                                }
                            }
                        }
                    }
                )
            }
        }

        val conversion_coroutine_scope = rememberCoroutineScope()
        val auth_state = player.context.ytapi.user_auth_state

        var conversion_in_progress by remember { mutableStateOf(false) }

        Crossfade(conversion_in_progress, Modifier.fillMaxWidth().weight(1f)) { converting ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (converting) {
                    SubtleLoadingIndicator()
                    return@Crossfade
                }

                if (auth_state != null) {
                    IconButton(
                        {
                            if (conversion_in_progress) {
                                return@IconButton
                            }
                            conversion_coroutine_scope.launch {
                                conversion_in_progress = true

                                val uploaded_playlist: Playlist? = playlist.uploadAsAccountPlaylist(auth_state)
                                    .getOrReport("ConvertPlaylistToAccountPlaylist")

                                if (uploaded_playlist != null) {
                                    player.openMediaItem(uploaded_playlist)
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

                        val local_playlist: Playlist? = playlist.downloadAsLocalPlaylist(player.context)
                            .getOrReport("ConvertPlaylistToLocalPlaylist")

                        if (local_playlist != null) {
                            player.openMediaItem(local_playlist)
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
