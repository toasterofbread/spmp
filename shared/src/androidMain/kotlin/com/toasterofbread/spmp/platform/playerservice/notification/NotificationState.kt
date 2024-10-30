package com.toasterofbread.spmp.platform.playerservice.notification

import android.media.session.PlaybackState
import dev.toastbits.ytmkt.model.external.SongLikedStatus

data class NotificationState(
    val playback_state: Int? = PlaybackState.STATE_NONE,
    val paused: Boolean = true,
    val current_liked_status: SongLikedStatus? = null,
    val authenticated: Boolean = false,
    val position_ms: Long? = null
)
