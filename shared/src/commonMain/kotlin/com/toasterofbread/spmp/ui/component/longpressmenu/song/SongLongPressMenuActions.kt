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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.toastbits.composekit.components.platform.composable.BackHandler
import dev.toastbits.composekit.context.vibrateShort
import dev.toastbits.composekit.util.composable.getValue
import dev.toastbits.composekit.components.utils.composable.ShapedIconButton
import com.toasterofbread.spmp.model.mediaitem.MEDIA_ITEM_RELATED_CONTENT_ICON
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.library.MediaItemLibrary
import com.toasterofbread.spmp.model.mediaitem.library.createLocalPlaylist
import com.toasterofbread.spmp.model.mediaitem.playlist.InteractivePlaylistEditor
import com.toasterofbread.spmp.model.mediaitem.playlist.InteractivePlaylistEditor.Companion.getEditorOrNull
import com.toasterofbread.spmp.model.mediaitem.playlist.LocalPlaylistData
import com.toasterofbread.spmp.model.mediaitem.playlist.Playlist
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.platform.download.DownloadStatus
import com.toasterofbread.spmp.platform.download.rememberDownloadStatus
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.component.longpressmenu.LongPressMenuActionProvider
import com.toasterofbread.spmp.ui.layout.PlaylistSelectMenu
import dev.toastbits.composekit.theme.core.onAccent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.playlist_create
import spmp.shared.generated.resources.toast_playlist_added
import spmp.shared.generated.resources.lpm_action_radio
import spmp.shared.generated.resources.song_add_to_playlist
import spmp.shared.generated.resources.lpm_action_delete_local_song_file
import spmp.shared.generated.resources.notif_download_finished
import spmp.shared.generated.resources.notif_download_already_finished
import spmp.shared.generated.resources.notif_download_cancelled
import spmp.shared.generated.resources.notif_download_already_downloading
import spmp.shared.generated.resources.lpm_action_download
import spmp.shared.generated.resources.lpm_action_go_to_artist
import spmp.shared.generated.resources.lpm_action_go_to_album
import spmp.shared.generated.resources.lpm_action_play_after_1_song
import spmp.shared.generated.resources.lpm_action_play_after_x_songs
import spmp.shared.generated.resources.lpm_action_song_related

@Composable
fun LongPressMenuActionProvider.SongLongPressMenuActions(
    item: MediaItem, // Might be a playlist
    spacing: Dp,
    queue_index: Int?,
    withSong: (suspend (Song) -> Unit) -> Unit,
) {
    val player: PlayerState = LocalPlayerState.current
    val density: Density = LocalDensity.current
    val coroutine_scope: CoroutineScope = rememberCoroutineScope()

    var height: Dp? by remember { mutableStateOf(null) }
    var adding_to_playlist: Boolean by remember { mutableStateOf(false) }

    Crossfade(adding_to_playlist) { playlist_interface ->
        if (!playlist_interface) {
            Column(
                Modifier.onSizeChanged { height = with(density) { it.height.toDp() } },
                verticalArrangement = Arrangement.spacedBy(spacing)
            ) {
                LPMActions(item, withSong, queue_index) { adding_to_playlist = true }
            }
        }
        else {
            BackHandler {
                adding_to_playlist = false
            }

            Column(
                Modifier
                    .border(1.dp, LocalContentColor.current, RoundedCornerShape(16.dp))
                    .padding(10.dp)
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
                    containerColor = player.theme.accent,
                    contentColor = player.theme.onAccent
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ShapedIconButton({ adding_to_playlist = false }, colours = button_colours) {
                        Icon(Icons.Default.Close, null)
                    }

                    Spacer(Modifier.fillMaxWidth().weight(1f))

                    Button(
                        {
                            coroutine_scope.launch {
                                val playlist: LocalPlaylistData = MediaItemLibrary.createLocalPlaylist(player.context).getOrNull()
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
                        {
                            if (selected_playlists.isNotEmpty()) {
                                withSong { song ->
                                    coroutine_scope.launch(NonCancellable) {
                                        for (playlist in selected_playlists) {
                                            val editor: InteractivePlaylistEditor =
                                                playlist.getEditorOrNull(player.context).getOrNull() ?: continue
                                            editor.addItem(song, null)
                                            editor.applyChanges()
                                        }

                                        player.context.sendToast(getString(Res.string.toast_playlist_added))
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
    val download: DownloadStatus? by (item as? Song)?.rememberDownloadStatus()
    val coroutine_scope = rememberCoroutineScope()

    ActionButton(
        Icons.Default.Radio, stringResource(Res.string.lpm_action_radio),
        onClick = {
            withSong {
                player.withPlayer {
                    playSong(it)
                }
            }
        },
        onAltClick = queue_index?.let { index -> {
            withSong {
                player.withPlayer {
                    startRadioAtIndex(index + 1, it, index, skip_first = true)
                }
            }
        }}
    )

    val lpm_increment_play_after: Boolean by player.settings.Behaviour.LPM_INCREMENT_PLAY_AFTER.observe()

    ActiveQueueIndexAction(
        { distance ->
            stringResource(if (distance == 1) Res.string.lpm_action_play_after_1_song else Res.string.lpm_action_play_after_x_songs).replace("\$x", distance.toString())
        },
        onClick = { active_queue_index ->
            withSong {
                player.withPlayer {
                    addToQueue(
                        it,
                        active_queue_index + 1,
                        is_active_queue = lpm_increment_play_after,
                        start_radio = false
                    )
                }
            }
        },
        onLongClick = { active_queue_index ->
            withSong {
                player.withPlayer {
                    addToQueue(
                        it,
                        active_queue_index + 1,
                        is_active_queue = lpm_increment_play_after,
                        start_radio = true
                    )
                }
            }
        }
    )

    ActionButton(Icons.Default.PlaylistAdd, stringResource(Res.string.song_add_to_playlist), onClick = openPlaylistInterface, onAction = {})

    if (download?.isCompleted() == true) {
        ActionButton(
            Icons.Default.Delete,
            stringResource(Res.string.lpm_action_delete_local_song_file),
            onClick = {
                val song: Song = download?.song ?: return@ActionButton
                coroutine_scope.launch {
                    player.context.download_manager.deleteSongLocalAudioFile(song)
                }
            }
        )
    }
    else if (download == null || download?.status == DownloadStatus.Status.IDLE) {
        fun downloadCallback(status: DownloadStatus?) {
            coroutine_scope.launch {
                when (status?.status) {
                    null -> {}
                    DownloadStatus.Status.FINISHED -> player.context.sendToast(getString(Res.string.notif_download_finished))
                    DownloadStatus.Status.ALREADY_FINISHED -> player.context.sendToast(getString(Res.string.notif_download_already_finished))
                    DownloadStatus.Status.CANCELLED -> player.context.sendToast(getString(Res.string.notif_download_cancelled))

                    // IDLE, DOWNLOADING, PAUSED
                    else -> {
                        player.context.sendToast(getString(Res.string.notif_download_already_downloading))
                    }
                }
            }
        }

        ActionButton(
            Icons.Default.Download,
            stringResource(Res.string.lpm_action_download),
            onClick = {
                withSong {
                    player.onSongDownloadRequested(it) { status -> downloadCallback(status) }
                }
            },
            onAltClick = {
                withSong {
                    player.onSongDownloadRequested(it, always_show_options = true) { status -> downloadCallback(status) }
                    player.context.vibrateShort()
                }
            }
        )
    }

    if (item is MediaItem.WithArtists) {
        val item_artists: List<Artist>? by item.Artists.observe(player.database)
        item_artists?.firstOrNull()?.also { artist ->
            ActionButton(Icons.Default.Person, stringResource(Res.string.lpm_action_go_to_artist), onClick = {
                player.openMediaItem(artist)
            })
        }
    }

    if (item is Song) {
        val item_album: Playlist? by item.Album.observe(player.database)
        item_album?.also { album ->
            ActionButton(Icons.Default.Album, stringResource(Res.string.lpm_action_go_to_album), onClick = {
                player.openMediaItem(album)
            })
        }
    }

    ActionButton(MEDIA_ITEM_RELATED_CONTENT_ICON, stringResource(Res.string.lpm_action_song_related), onClick = {
        withSong {
            player.openMediaItem(it)
        }
    })
}
