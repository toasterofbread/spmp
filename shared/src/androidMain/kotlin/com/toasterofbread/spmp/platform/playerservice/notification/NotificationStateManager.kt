package com.toasterofbread.spmp.platform.playerservice.notification

import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.SystemClock
import androidx.media3.common.Player
import com.toasterofbread.spmp.platform.playerservice.PlayerServiceNotificationCustomAction
import com.toasterofbread.spmp.ui.getAndroidIcon
import dev.toastbits.composekit.util.platform.launchSingle
import dev.toastbits.ytmkt.model.external.SongLikedStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext

class NotificationStateManager(private val media_session: MediaSession) {
    var current: NotificationState = NotificationState()
        private set

    private val coroutine_scope: CoroutineScope = CoroutineScope(Job())

    fun update(
        playback_state: Int? = current.playback_state,
        paused: Boolean = current.paused,
        current_liked_status: SongLikedStatus? = current.current_liked_status,
        authenticated: Boolean = current.authenticated,
        position_ms: Long? = current.position_ms
    ) {
        val new_state: NotificationState =
            NotificationState(
                playback_state,
                paused,
                current_liked_status,
                authenticated,
                position_ms
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
            position_ms ?: 0,
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
                current_liked_status.getAndroidIcon(authenticated)
            ).build()
        )

        return@withContext state_builder.build()
    }
}
