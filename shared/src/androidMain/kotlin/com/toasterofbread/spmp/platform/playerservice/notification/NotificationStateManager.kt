package com.toasterofbread.spmp.platform.playerservice.notification

import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.SystemClock
import androidx.media3.common.Player
import com.toasterofbread.spmp.platform.playerservice.PlayerServiceNotificationCustomAction
import com.toasterofbread.spmp.shared.R
import dev.toastbits.composekit.utils.common.launchSingle
import dev.toastbits.ytmkt.model.external.SongLikedStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext

class NotificationStateManager(
    private val media_session: MediaSession,
    private val player: Player
) {
    var current: NotificationState = NotificationState()
        private set

    private val coroutine_scope: CoroutineScope = CoroutineScope(Job())

    fun update(
        playback_state: Int? = current.playback_state,
        paused: Boolean = current.paused,
        current_liked_status: SongLikedStatus? = current.current_liked_status,
        authenticated: Boolean = current.authenticated
    ) {
        val new_state: NotificationState =
            NotificationState(
                playback_state,
                paused,
                current_liked_status,
                authenticated
            )

        if (new_state == current) {
            return
        }

        current = new_state

        coroutine_scope.launchSingle {
            media_session.setPlaybackState(new_state.build())
        }
    }

    fun release() {
        coroutine_scope.cancel()
    }

    private suspend fun NotificationState.build(): PlaybackState? = withContext(Dispatchers.Main) {
        val state_builder: PlaybackState.Builder = PlaybackState.Builder()

        state_builder.setState(
            playback_state
                ?: if (paused) PlaybackState.STATE_PAUSED else PlaybackState.STATE_PLAYING,
            player.currentPosition,
            if (paused) 0f else 1f,
            SystemClock.elapsedRealtime()
        )
        state_builder.setActions(
            PlaybackState.ACTION_SEEK_TO or PlaybackState.ACTION_PLAY_PAUSE or PlaybackState.ACTION_SKIP_TO_NEXT or PlaybackState.ACTION_SKIP_TO_PREVIOUS
        )

        val like_action: PlayerServiceNotificationCustomAction =
            when (current_liked_status) {
                SongLikedStatus.NEUTRAL, null -> PlayerServiceNotificationCustomAction.LIKE
                SongLikedStatus.DISLIKED,
                SongLikedStatus.LIKED -> PlayerServiceNotificationCustomAction.UNLIKE
            }

        state_builder.addCustomAction(
            PlaybackState.CustomAction.Builder(
                like_action.name,
                like_action.name,
                if (authenticated)
                    when (current_liked_status) {
                        null,
                        SongLikedStatus.NEUTRAL -> R.drawable.ic_thumb_up_off
                        SongLikedStatus.LIKED -> R.drawable.ic_thumb_up
                        SongLikedStatus.DISLIKED -> R.drawable.ic_thumb_down
                    }
                else
                    when (current_liked_status) {
                        null,
                        SongLikedStatus.DISLIKED,
                        SongLikedStatus.NEUTRAL -> R.drawable.ic_heart_off
                        SongLikedStatus.LIKED -> R.drawable.ic_heart
                    }
            ).build()
        )

        return@withContext state_builder.build()
    }
}
