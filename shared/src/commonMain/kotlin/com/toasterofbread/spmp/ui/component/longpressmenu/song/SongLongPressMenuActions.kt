package com.toasterofbread.spmp.ui.component.longpressmenu.song

import LocalPlayerState
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.model.mediaitem.MEDIA_ITEM_RELATED_CONTENT_ICON
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.library.MediaItemLibrary
import com.toasterofbread.spmp.model.mediaitem.library.createLocalPlaylist
import com.toasterofbread.spmp.model.mediaitem.playlist.Playlist
import com.toasterofbread.spmp.model.mediaitem.playlist.PlaylistEditor.Companion.getEditorOrNull
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.platform.PlayerDownloadManager
import com.toasterofbread.spmp.platform.composable.BackHandler
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.component.longpressmenu.LongPressMenuActionProvider
import com.toasterofbread.spmp.ui.layout.PlaylistSelectMenu
import com.toasterofbread.spmp.ui.theme.Theme
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.getOrReport
import com.toasterofbread.utils.composable.ShapedIconButton
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch

@Composable
fun LongPressMenuActionProvider.SongLongPressMenuActions(
    item: MediaItem,
    spacing: Dp,
    queue_index: Int?,
    withSong: (suspend (Song) -> Unit) -> Unit,
) {
    val player = LocalPlayerState.current
    val density = LocalDensity.current
    val coroutine_scope = rememberCoroutineScope()

    var height: Dp? by remember { mutableStateOf(null) }
    var adding_to_playlist by remember { mutableStateOf(false) }

    Crossfade(adding_to_playlist) { playlist_interface ->
        if (!playlist_interface) {
            Column(
                Modifier.onSizeChanged { height = with(density) { it.height.toDp() } },
                verticalArrangement = Arrangement.spacedBy(spacing)
            ) {
                LPMActions(item, withSong, queue_index) { adding_to_playlist = true }
            }
        } else {
            BackHandler {
                adding_to_playlist = false
            }

            Column(
                Modifier
                    .border(1.dp, LocalContentColor.current, RoundedCornerShape(16.dp))
                    .fillMaxWidth()
                    .then(
                        height?.let { Modifier.height(it) } ?: Modifier
                    )
            ) {
                val selected_playlists = remember { mutableStateListOf<Playlist>() }
                PlaylistSelectMenu(
                    selected_playlists,
                    player.context.ytapi.user_auth_state,
                    Modifier.fillMaxHeight().weight(1f)
                )

                val button_colours = IconButtonDefaults.iconButtonColors(
                    containerColor = Theme.accent,
                    contentColor = Theme.on_accent
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    ShapedIconButton({ adding_to_playlist = false }, colours = button_colours) {
                        Icon(Icons.Default.Close, null)
                    }

                    Spacer(Modifier.fillMaxWidth().weight(1f))

                    Button(
                        {
                            coroutine_scope.launch {
                                val playlist =
                                    MediaItemLibrary.createLocalPlaylist(player.context).getOrReport("SongLongPressMenuActionsCreateLocalPlaylist")
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
                        {
                            if (selected_playlists.isNotEmpty()) {
                                withSong { song ->
                                    coroutine_scope.launch(NonCancellable) {
                                        for (playlist in selected_playlists) {
                                            val editor = playlist.getEditorOrNull(player.context).getOrNull() ?: continue
                                            editor.addItem(song, null)
                                            editor.applyChanges()
                                        }

                                        player.context.sendToast(getString("toast_playlist_added"))
                                    }
                                }

                                onAction()
                            }

                            adding_to_playlist = false
                        },
                        colours = button_colours
                    ) {
                        Icon(Icons.Default.Done, null)
                    }
                }
            }
        }
    }
}

@Composable
private fun LongPressMenuActionProvider.LPMActions(
    item: MediaItem,
    withSong: (suspend (Song) -> Unit) -> Unit,
    queue_index: Int?,
    openPlaylistInterface: () -> Unit
) {
    val player = LocalPlayerState.current

    ActionButton(
        Icons.Default.Radio, getString("lpm_action_radio"),
        onClick = {
            withSong {
                player.player?.playSong(it)
            }
        },
        onLongClick = queue_index?.let { index -> {
            withSong {
                player.player?.startRadioAtIndex(index + 1, it, index, skip_first = true)
            }
        }}
    )

    ActiveQueueIndexAction(
        { distance ->
            getString(if (distance == 1) "lpm_action_play_after_1_song" else "lpm_action_play_after_x_songs").replace("\$x", distance.toString())
        },
        onClick = { active_queue_index ->
            withSong {
                player.player?.addToQueue(
                    it,
                    active_queue_index + 1,
                    is_active_queue = Settings.KEY_LPM_INCREMENT_PLAY_AFTER.get(),
                    start_radio = false
                )
            }
        },
        onLongClick = { active_queue_index ->
            withSong {
                player.player?.addToQueue(
                    it,
                    active_queue_index + 1,
                    is_active_queue = Settings.KEY_LPM_INCREMENT_PLAY_AFTER.get(),
                    start_radio = true
                )
            }
        }
    )

    ActionButton(Icons.Default.PlaylistAdd, getString("song_add_to_playlist"), onClick = openPlaylistInterface, onAction = {})

    ActionButton(Icons.Default.Download, getString("lpm_action_download"), onClick = {
        withSong {
            player.context.download_manager.startDownload(it.id) { status: PlayerDownloadManager.DownloadStatus ->
                when (status.status) {
                    PlayerDownloadManager.DownloadStatus.Status.FINISHED -> player.context.sendToast(getString("notif_download_finished"))
                    PlayerDownloadManager.DownloadStatus.Status.ALREADY_FINISHED -> player.context.sendToast(getString("notif_download_already_finished"))
                    PlayerDownloadManager.DownloadStatus.Status.CANCELLED -> player.context.sendToast(getString("notif_download_cancelled"))

                    // IDLE, DOWNLOADING, PAUSED
                    else -> {
                        player.context.sendToast(getString("notif_download_already_downloading"))
                    }
                }
            }
        }
    })

    if (item is MediaItem.WithArtist) {
        val item_artist: Artist? by item.Artist.observe(player.database)
        item_artist?.also { artist ->
            ActionButton(Icons.Default.Person, getString("lpm_action_go_to_artist"), onClick = {
                player.openMediaItem(artist,)
            })
        }
    }

    ActionButton(MEDIA_ITEM_RELATED_CONTENT_ICON, getString("lpm_action_song_related"), onClick = {
        withSong {
            player.openMediaItem(it,)
        }
    })
}
