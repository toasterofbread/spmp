package com.toasterofbread.spmp.model.appaction

import LocalPlayerState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.model.mediaitem.db.observePinnedToHome
import com.toasterofbread.spmp.model.mediaitem.db.togglePinned
import com.toasterofbread.spmp.model.mediaitem.loader.SongLikedLoader
import com.toasterofbread.spmp.model.mediaitem.playlist.Playlist
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.song.updateLiked
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.download.DownloadStatus
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.component.LikeDislikeButton
import com.toasterofbread.spmp.util.getToggleTarget
import dev.toastbits.composekit.context.vibrateShort
import dev.toastbits.composekit.components.utils.composable.LargeDropdownMenu
import dev.toastbits.ytmkt.endpoint.SetSongLikedEndpoint
import dev.toastbits.ytmkt.endpoint.SongLikedEndpoint
import dev.toastbits.ytmkt.model.external.SongLikedStatus
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.appaction_config_song_action
import spmp.shared.generated.resources.appaction_song_action_copy_url
import spmp.shared.generated.resources.appaction_song_action_download
import spmp.shared.generated.resources.appaction_song_action_hide
import spmp.shared.generated.resources.appaction_song_action_open_album
import spmp.shared.generated.resources.appaction_song_action_open_externally
import spmp.shared.generated.resources.appaction_song_action_open_related
import spmp.shared.generated.resources.appaction_song_action_remove_from_queue
import spmp.shared.generated.resources.appaction_song_action_share
import spmp.shared.generated.resources.appaction_song_action_start_radio
import spmp.shared.generated.resources.appaction_song_action_toggle_like
import spmp.shared.generated.resources.appaction_song_action_toggle_pin
import spmp.shared.generated.resources.notif_copied_to_clipboard
import spmp.shared.generated.resources.notif_download_already_downloading
import spmp.shared.generated.resources.notif_download_already_finished
import spmp.shared.generated.resources.notif_download_cancelled
import spmp.shared.generated.resources.notif_download_finished

@Serializable
data class SongAppAction(
    val action: Action = Action.DEFAULT
): AppAction {
    override fun getType(): AppAction.Type = AppAction.Type.SONG
    override fun getIcon(): ImageVector = action.getIcon()

    override fun hasCustomContent() = action.hasCustomContent()

    @Composable
    override fun CustomContent(onClick: (() -> Unit)?, modifier: Modifier) {
        val song: Song = LocalPlayerState.current.status.song ?: return
        action.CustomContent(onClick, song, modifier)
    }

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

    @Composable
    override fun ConfigurationItems(item_modifier: Modifier, onModification: (AppAction) -> Unit) {
        val player: PlayerState = LocalPlayerState.current

        var show_action_selector: Boolean by remember { mutableStateOf(false) }
        val available_actions: List<Action> = remember { Action.getAvailable(player.context) }

        LargeDropdownMenu(
            title = stringResource(Res.string.appaction_config_song_action),
            isOpen = show_action_selector,
            onDismissRequest = { show_action_selector = false },
            items = available_actions,
            selectedItem = action,
            itemContent = { action ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(action.getIcon(), null)
                    Text(action.getName())
                }
            },
            onSelected = { _, action ->
                onModification(copy(action = action))
                show_action_selector = false
            }
        )

        FlowRow(
            item_modifier,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                stringResource(Res.string.appaction_config_song_action),
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
        REMOVE_FROM_QUEUE,
        SHARE,
        OPEN_EXTERNALLY,
        COPY_URL;

        companion object {
            val DEFAULT: Action = TOGGLE_LIKE

            fun getAvailable(context: AppContext): List<Action> =
                entries.filter { it.isAvailable(context) }
        }

        fun hasCustomContent(): Boolean =
            this == TOGGLE_LIKE || this == TOGGLE_PIN

        @Composable
        fun CustomContent(onClick: (() -> Unit)?, song: Song, modifier: Modifier = Modifier) {
            when (this) {
                TOGGLE_LIKE -> LikeDislikeButton(
                    song,
                    LocalPlayerState.current.context.ytapi.user_auth_state,
                    modifier,
                    onClick = onClick
                )
                TOGGLE_PIN -> {
                    var pinned: Boolean by song.observePinnedToHome()
                    IconButton(
                        {
                            if (onClick != null) {
                                onClick()
                            }
                            else {
                                pinned = !pinned
                            }
                        }
                    ) {
                        Icon(if (pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin, null)
                    }
                }
                else -> throw IllegalStateException(this.toString())
            }
        }

        @Composable
        fun getName(): String =
            when (this) {
                TOGGLE_LIKE -> stringResource(Res.string.appaction_song_action_toggle_like)
                TOGGLE_PIN -> stringResource(Res.string.appaction_song_action_toggle_pin)
                HIDE -> stringResource(Res.string.appaction_song_action_hide)
                START_RADIO -> stringResource(Res.string.appaction_song_action_start_radio)
                DOWNLOAD -> stringResource(Res.string.appaction_song_action_download)
                OPEN_ALBUM -> stringResource(Res.string.appaction_song_action_open_album)
                OPEN_RELATED -> stringResource(Res.string.appaction_song_action_open_related)
                REMOVE_FROM_QUEUE -> stringResource(Res.string.appaction_song_action_remove_from_queue)
                SHARE -> stringResource(Res.string.appaction_song_action_share)
                OPEN_EXTERNALLY -> stringResource(Res.string.appaction_song_action_open_externally)
                COPY_URL -> stringResource(Res.string.appaction_song_action_copy_url)
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
                SHARE -> Icons.Default.Share
                OPEN_EXTERNALLY -> Icons.Default.OpenInNew
                COPY_URL -> Icons.Default.ContentCopy
            }

        fun isAvailable(context: AppContext): Boolean =
            when (this) {
                SHARE -> context.canShare()
                OPEN_EXTERNALLY -> context.canOpenUrl()
                COPY_URL -> context.canCopyText()
                else -> true
            }

        private suspend fun Song.getCurrentUrl(queue_index: Int, player: PlayerState): String {
            var url: String = getUrl(player.context)
            if (queue_index == player.status.index && player.settings.Behaviour.INCLUDE_PLAYBACK_POSITION_IN_SHARE_URL.get()) {
                val position_ms: Long = player.status.getPositionMs()
                if (position_ms >= 0) {
                    url += "&t=${position_ms / 1000}"
                }
            }
            return url
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
                        liked.getToggleTarget(),
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
                        player.coroutine_scope.launch {
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
                SHARE -> {
                    if (player.context.canShare()) {
                        player.context.shareText(
                            song.getCurrentUrl(queue_index, player),
                            song.Title.get(player.database)
                        )
                    }
                }
                OPEN_EXTERNALLY -> {
                    if (player.context.canOpenUrl()) {
                        player.context.openUrl(song.getCurrentUrl(queue_index, player))
                        player.context.vibrateShort()
                    }
                }
                COPY_URL -> {
                    if (player.context.canCopyText()) {
                        player.context.copyText(song.getCurrentUrl(queue_index, player))
                        player.context.vibrateShort()
                        player.context.sendToast(getString(Res.string.notif_copied_to_clipboard))
                    }
                }
            }
        }
    }
}
