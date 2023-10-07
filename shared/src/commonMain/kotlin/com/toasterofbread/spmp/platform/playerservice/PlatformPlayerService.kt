package com.toasterofbread.spmp.platform.playerservice

import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.platform.MediaPlayerRepeatMode
import com.toasterofbread.spmp.platform.MediaPlayerState
import com.toasterofbread.spmp.platform.PlatformContext
import com.toasterofbread.spmp.platform.PlayerListener
import com.toasterofbread.spmp.youtubeapi.radio.RadioInstance

data class PlayerServiceState(
    val stop_after_current_song: Boolean = false,
    val radio_state: RadioInstance.RadioState = RadioInstance.RadioState(),
    val undo_count: Int = 0,
    val redo_count: Int = 0,
    val active_queue_index: Int = 0,
    val session_started: Boolean = false
) {
    companion object
}

expect class PlatformPlayerService() {
    companion object {
        val instance: PlatformPlayerService

        fun addListener(listener: PlayerListener)
        fun removeListener(listener: PlayerListener)
    }

    val context: PlatformContext
    val service_player: PlayerServicePlayer

    fun onCreate()
    fun onDestroy()

    val state: MediaPlayerState
    val is_playing: Boolean
    val song_count: Int
    val current_song_index: Int
    val current_position_ms: Long
    val duration_ms: Long
    val has_focus: Boolean

    val radio_state: RadioInstance.RadioState

    var repeat_mode: MediaPlayerRepeatMode
    var volume: Float

    fun play()
    fun pause()
    fun playPause()

    fun seekTo(position_ms: Long)
    fun seekToSong(index: Int)
    fun seekToNext()
    fun seekToPrevious()

    fun getSong(): Song?
    fun getSong(index: Int): Song?

    fun addSong(song: Song, index: Int)
    fun moveSong(from: Int, to: Int)
    fun removeSong(index: Int)

    fun addListener(listener: PlayerListener)
    fun removeListener(listener: PlayerListener)

    var service_state: PlayerServiceState
}
