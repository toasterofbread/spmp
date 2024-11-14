package com.toasterofbread.spmp.platform.playerservice

import android.content.Intent
import android.content.Intent.EXTRA_KEY_EVENT
import android.media.session.MediaSession
import android.os.Bundle
import android.view.KeyEvent
import com.toasterofbread.spmp.platform.AppContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class PlayerSessionCallback(
    private val player: PlayerService,
    private val context: AppContext,
    private val coroutine_scope: CoroutineScope
): MediaSession.Callback() {
    override fun onMediaButtonEvent(mediaButtonIntent: Intent): Boolean {
        val key_event: KeyEvent? = mediaButtonIntent.extras?.getParcelable(EXTRA_KEY_EVENT)
        if (key_event?.action != KeyEvent.ACTION_UP) {
            return false
        }

        when (key_event.keyCode) {
            KeyEvent.KEYCODE_MEDIA_PLAY -> player.play()
            KeyEvent.KEYCODE_MEDIA_PAUSE -> player.pause()
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE ->
                if (player.is_playing) player.pause()
                else player.play()
            KeyEvent.KEYCODE_MEDIA_NEXT -> player.seekToNext()
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> player.seekToPreviousOrRepeat()
            else -> {
                println("PlayerSessionCallback: Received unhandled media button event: $key_event")
                return false
            }
        }

        return true
    }

    override fun onSkipToNext() {
        player.seekToNext()
    }

    override fun onSkipToPrevious() {
        player.seekToPreviousOrRepeat()
    }

    override fun onPlay() {
        player.play()
    }

    override fun onPause() {
        player.pause()
    }

    override fun onSeekTo(pos: Long) {
        player.seekToTime(pos)
    }

    override fun onCustomAction(action: String, extras: Bundle?) {
        val custom_action: PlayerServiceNotificationCustomAction? =
            PlayerServiceNotificationCustomAction.entries.firstOrNull { it.name == action }

        if (custom_action == null) {
            println("PlayerSessionCallback: Received unknown custom notification action: '$action'")
            return
        }

        coroutine_scope.launch {
            try {
                custom_action.execute(player, context)
            }
            catch (e: Throwable) {
                RuntimeException("PlayerSessionCallback: Ignoring exception when executing custom notification action $custom_action", e).printStackTrace()
            }
        }
    }
}
