package com.toasterofbread.spmp.model.appaction

import LocalAppContext
import kotlinx.serialization.Serializable
import LocalAppState
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.song.updateLiked
import com.toasterofbread.spmp.model.mediaitem.playlist.Playlist
import com.toasterofbread.spmp.platform.download.DownloadStatus
import com.toasterofbread.spmp.platform.AppContext
import dev.toastbits.composekit.utils.composable.LargeDropdownMenu
import dev.toastbits.composekit.platform.vibrateShort
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PushPin
import com.toasterofbread.spmp.model.mediaitem.db.togglePinned
import com.toasterofbread.spmp.model.mediaitem.db.observePinnedToHome
import com.toasterofbread.spmp.model.mediaitem.loader.SongLikedLoader
import com.toasterofbread.spmp.ui.component.LikeDislikeButton
import LocalPlayerState
import LocalSessionState
import dev.toastbits.ytmkt.model.external.SongLikedStatus
import dev.toastbits.ytmkt.endpoint.SongLikedEndpoint
import dev.toastbits.ytmkt.endpoint.SetSongLikedEndpoint
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.appaction_config_song_action
import spmp.shared.generated.resources.appaction_song_action_toggle_like
import spmp.shared.generated.resources.appaction_song_action_toggle_pin
import spmp.shared.generated.resources.appaction_song_action_hide
import spmp.shared.generated.resources.appaction_song_action_start_radio
import spmp.shared.generated.resources.appaction_song_action_download
import spmp.shared.generated.resources.appaction_song_action_open_album
import spmp.shared.generated.resources.appaction_song_action_open_related
import spmp.shared.generated.resources.appaction_song_action_remove_from_queue
import spmp.shared.generated.resources.appaction_song_action_share
import spmp.shared.generated.resources.appaction_song_action_open_externally
import spmp.shared.generated.resources.appaction_song_action_copy_url
import spmp.shared.generated.resources.notif_download_finished
import spmp.shared.generated.resources.notif_download_already_finished
import spmp.shared.generated.resources.notif_download_cancelled
import spmp.shared.generated.resources.notif_download_already_downloading
import spmp.shared.generated.resources.notif_copied_to_clipboard

@Serializable
data class SongAppAction(
    val action: Action = Action.DEFAULT
): AppAction {
    override fun getType(): AppAction.Type = AppAction.Type.SONG
    override fun getIcon(): ImageVector = action.getIcon()

    override fun hasCustomContent() = action.hasCustomContent()

    @Composable
    override fun CustomContent(onClick: (() -> Unit)?, modifier: Modifier) {
        val song: Song = LocalSessionState.current.status.song ?: return
        action.CustomContent(onClick, song, modifier)
    }

    override suspend fun executeAction(state: SpMp.State) {
        val song: Song = state.session.status.song ?: return
        val index: Int = state.session.status.index
        if (index < 0) {
            return
        }
        action.execute(song, index, state)
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
        val context: AppContext = LocalAppContext.current

        var show_action_selector: Boolean by remember { mutableStateOf(false) }
        val available_actions: List<Action> = remember { Action.getAvailable(context) }

        LargeDropdownMenu(
            expanded = show_action_selector,
            onDismissRequest = { show_action_selector = false },
            item_count = available_actions.size,
            selected = action.ordinal,
            itemContent = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val action: Action = available_actions[it]
                    Icon(action.getIcon(), null)
                    Text(action.getName())
                }
            },
            onSelected = {
                onModification(copy(action = available_actions[it]))
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
                    LocalAppContext.current.ytapi.user_auth_state,
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

        suspend fun execute(song: Song, queue_index: Int, state: SpMp.State) {
            when (this) {
                TOGGLE_LIKE -> {
                    val set_liked_endpoint: SetSongLikedEndpoint? = state.context.ytapi.user_auth_state?.SetSongLiked
                    val get_liked_endpoint: SongLikedEndpoint? = state.context.ytapi.user_auth_state?.SongLiked
                    val liked: SongLikedStatus? =
                        SongLikedLoader.loadSongLiked(song.id, state.context, get_liked_endpoint).getOrNull()
                        ?: song.Liked.get(state.database)

                    song.updateLiked(
                        when (liked) {
                            SongLikedStatus.LIKED, SongLikedStatus.DISLIKED -> SongLikedStatus.NEUTRAL
                            SongLikedStatus.NEUTRAL, null -> SongLikedStatus.LIKED
                        },
                        set_liked_endpoint,
                        state.context
                    )
                }
                TOGGLE_PIN -> {
                    song.togglePinned(state.context)
                }
                HIDE -> {
                    song.Hidden.set(true, state.database)
                    state.session.withPlayer {
                        removeFromQueue(queue_index)
                    }
                }
                START_RADIO -> {
                    state.session.withPlayer {
                        playSong(song)
                    }
                }
                DOWNLOAD -> {
                    state.ui.onSongDownloadRequested(listOf(song)) { status ->
                        state.context.coroutine_scope.launch {
                            when (status?.status) {
                                null -> {}
                                DownloadStatus.Status.FINISHED -> state.context.sendToast(getString(Res.string.notif_download_finished))
                                DownloadStatus.Status.ALREADY_FINISHED -> state.context.sendToast(getString(Res.string.notif_download_already_finished))
                                DownloadStatus.Status.CANCELLED -> state.context.sendToast(getString(Res.string.notif_download_cancelled))

                                // IDLE, DOWNLOADING, PAUSED
                                else -> {
                                    state.context.sendToast(getString(Res.string.notif_download_already_downloading))
                                }
                            }
                        }
                    }
                }
                OPEN_ALBUM -> {
                    val album: Playlist = song.Album.get(state.database) ?: return
                    state.ui.openMediaItem(album)
                }
                OPEN_RELATED -> {
                    state.ui.openMediaItem(song)
                }
                REMOVE_FROM_QUEUE -> {
                    state.session.withPlayer {
                        removeFromQueue(queue_index)
                    }
                }
                SHARE -> {
                    if (state.context.canShare()) {
                        state.context.shareText(song.getUrl(state.context), song.Title.get(state.database))
                    }
                }
                OPEN_EXTERNALLY -> {
                    if (state.context.canOpenUrl()) {
                        state.context.openUrl(song.getUrl(state.context))
                        state.context.vibrateShort()
                    }
                }
                COPY_URL -> {
                    if (state.context.canCopyText()) {
                        state.context.copyText(song.getUrl(state.context))
                        state.context.vibrateShort()
                        state.context.sendToast(getString(Res.string.notif_copied_to_clipboard))
                    }
                }
            }
        }
    }
}
