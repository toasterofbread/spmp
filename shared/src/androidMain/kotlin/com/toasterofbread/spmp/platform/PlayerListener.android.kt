package com.toasterofbread.spmp.platform

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.toasterofbread.spmp.model.mediaitem.song.Song
import dev.toastbits.spms.socketapi.shared.SpMsPlayerRepeatMode
import dev.toastbits.spms.socketapi.shared.SpMsPlayerState
import com.toasterofbread.spmp.platform.playerservice.convertState
import com.toasterofbread.spmp.platform.playerservice.toSong

actual abstract class PlayerListener {
    actual open fun onSongTransition(song: Song?, manual: Boolean) {}
    actual open fun onStateChanged(state: SpMsPlayerState) {}
    actual open fun onPlayingChanged(is_playing: Boolean) {}
    actual open fun onRepeatModeChanged(repeat_mode: SpMsPlayerRepeatMode) {}
    actual open fun onVolumeChanged(volume: Float) {}
    actual open fun onDurationChanged(duration_ms: Long) {}
    actual open fun onSeeked(position_ms: Long) {}
    actual open fun onUndoStateChanged() {}

    actual open fun onSongAdded(index: Int, song: Song) {}
    actual open fun onSongRemoved(index: Int, song: Song) {}
    actual open fun onSongMoved(from: Int, to: Int) {}

    actual open fun onEvents() {}

    private val player_listener = object : Player.Listener {
        var current_song: Song? = null
        override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
            val song = item?.toSong()
            if (song?.id == current_song?.id) {
                return
            }
            current_song = song
            onSongTransition(song, reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK)
        }
        override fun onPlaybackStateChanged(state: Int) {
            onStateChanged(convertState(state))
        }
        override fun onIsPlayingChanged(is_playing: Boolean) {
            onPlayingChanged(is_playing)
        }
        override fun onRepeatModeChanged(repeat_mode: Int) {
            onRepeatModeChanged(SpMsPlayerRepeatMode.entries[repeat_mode])
        }
        override fun onVolumeChanged(volume: Float) {
            this@PlayerListener.onVolumeChanged(volume)
        }
        override fun onEvents(player: Player, events: Player.Events) {
            onEvents()
        }
    }

    internal fun addToPlayer(player: Player) {
        player.addListener(player_listener)
    }
    internal fun removeFromPlayer(player: Player) {
        player.removeListener(player_listener)
    }
}
