package com.toasterofbread.spmp.model.appaction

import kotlinx.serialization.Serializable
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.song.SongLikedStatus
import com.toasterofbread.spmp.model.mediaitem.song.updateLiked
import com.toasterofbread.spmp.model.mediaitem.playlist.Playlist
import com.toasterofbread.spmp.platform.download.DownloadStatus
import com.toasterofbread.composekit.utils.composable.LargeDropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.toasterofbread.spmp.model.mediaitem.db.togglePinned
import com.toasterofbread.spmp.model.mediaitem.loader.SongLikedLoader
import com.toasterofbread.spmp.model.appaction.AppAction
import com.toasterofbread.spmp.youtubeapi.endpoint.SetSongLikedEndpoint
import com.toasterofbread.spmp.youtubeapi.endpoint.SongLikedEndpoint

@Serializable
data class SongAppAction(
    val action: Action = Action.DEFAULT
): AppAction {
    override fun getType(): AppAction.Type = AppAction.Type.SONG
    override fun getIcon(): ImageVector = action.getIcon()
    override suspend fun executeAction(player: PlayerState) {
        val song: Song = player.status.song ?: return
        val index: Int = player.status.index
        if (index < 0) {
            return
        }
        action.execute(song, index, player)
    }

    @Composable
    override fun Preview(modifier: Modifier) {
        Row(
            modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(action.getIcon(), null)
            Text(action.getName(), softWrap = false)
        }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    override fun ConfigurationItems(item_modifier: Modifier, onModification: (AppAction) -> Unit) {
        var show_action_selector: Boolean by remember { mutableStateOf(false) }

        LargeDropdownMenu(
            expanded = show_action_selector,
            onDismissRequest = { show_action_selector = false },
            item_count = Action.entries.size,
            selected = action.ordinal,
            itemContent = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val action: Action = Action.entries[it]
                    Icon(action.getIcon(), null)
                    Text(action.getName())
                }
            },
            onSelected = {
                onModification(copy(action = Action.entries[it]))
                show_action_selector = false
            }
        )

        FlowRow(
            item_modifier,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                getString("appaction_config_song_action"),
                Modifier.align(Alignment.CenterVertically),
                softWrap = false
            )

            Button({ show_action_selector = !show_action_selector }) {
                Preview(Modifier)
            }
        }
    }

    enum class Action {
        TOGGLE_LIKE,
        TOGGLE_PIN,
        HIDE,
        START_RADIO,
        DOWNLOAD,
        OPEN_ALBUM,
        OPEN_RELATED,
        REMOVE_FROM_QUEUE;

        companion object {
            val DEFAULT: Action = TOGGLE_LIKE
        }

        fun getName(): String =
            when (this) {
                TOGGLE_LIKE -> getString("appaction_song_action_toggle_like")
                TOGGLE_PIN -> getString("appaction_song_action_toggle_pin")
                HIDE -> getString("appaction_song_action_hide")
                START_RADIO -> getString("appaction_song_action_start_radio")
                DOWNLOAD -> getString("appaction_song_action_download")
                OPEN_ALBUM -> getString("appaction_song_action_open_album")
                OPEN_RELATED -> getString("appaction_song_action_open_related")
                REMOVE_FROM_QUEUE -> getString("appaction_song_action_remove_from_queue")
            }

        fun getIcon(): ImageVector =
            when (this) {
                TOGGLE_LIKE -> Icons.Default.Favorite
                TOGGLE_PIN -> Icons.Default.PushPin
                HIDE -> Icons.Default.VisibilityOff
                START_RADIO -> Icons.Default.Radio
                DOWNLOAD -> Icons.Default.Download
                OPEN_ALBUM -> Icons.Default.Album
                OPEN_RELATED -> Icons.Default.GridView
                REMOVE_FROM_QUEUE -> Icons.Default.Close
            }

        suspend fun execute(song: Song, queue_index: Int, player: PlayerState) {
            when (this) {
                TOGGLE_LIKE -> {
                    val set_liked_endpoint: SetSongLikedEndpoint? = player.context.ytapi.user_auth_state?.SetSongLiked
                    val get_liked_endpoint: SongLikedEndpoint? = player.context.ytapi.user_auth_state?.SongLiked
                    val liked: SongLikedStatus? =
                        SongLikedLoader.loadSongLiked(song.id, player.context, get_liked_endpoint).getOrNull()
                        ?: song.Liked.get(player.database)

                    song.updateLiked(
                        when (liked) {
                            SongLikedStatus.LIKED, SongLikedStatus.DISLIKED -> SongLikedStatus.NEUTRAL
                            SongLikedStatus.NEUTRAL, null -> SongLikedStatus.LIKED
                        },
                        set_liked_endpoint,
                        player.context
                    )
                }
                TOGGLE_PIN -> {
                    song.togglePinned(player.context)
                }
                HIDE -> {
                    song.Hidden.set(true, player.database)
                    player.withPlayer {
                        removeFromQueue(queue_index)
                    }
                }
                START_RADIO -> {
                    player.withPlayer {
                        playSong(song)
                    }
                }
                DOWNLOAD -> {
                    player.onSongDownloadRequested(song) { status ->
                        when (status?.status) {
                            null -> {}
                            DownloadStatus.Status.FINISHED -> player.context.sendToast(getString("notif_download_finished"))
                            DownloadStatus.Status.ALREADY_FINISHED -> player.context.sendToast(getString("notif_download_already_finished"))
                            DownloadStatus.Status.CANCELLED -> player.context.sendToast(getString("notif_download_cancelled"))

                            // IDLE, DOWNLOADING, PAUSED
                            else -> {
                                player.context.sendToast(getString("notif_download_already_downloading"))
                            }
                        }
                    }
                }
                OPEN_ALBUM -> {
                    val album: Playlist = song.Album.get(player.database) ?: return
                    player.openMediaItem(album)
                }
                OPEN_RELATED -> {
                    player.openMediaItem(song)
                }
                REMOVE_FROM_QUEUE -> {
                    player.withPlayer {
                        removeFromQueue(queue_index)
                    }
                }
            }
        }
    }
}
