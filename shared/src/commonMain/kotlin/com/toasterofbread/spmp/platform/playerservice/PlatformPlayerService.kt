package com.toasterofbread.spmp.platform.playerservice

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.platform.PlatformContext
import com.toasterofbread.spmp.platform.PlayerListener
import com.toasterofbread.spmp.youtubeapi.radio.RadioInstance

internal const val AUTO_DOWNLOAD_SOFT_TIMEOUT = 1500 // ms

enum class MediaPlayerState {
    IDLE,
    BUFFERING,
    READY,
    ENDED
}

enum class MediaPlayerRepeatMode {
    NONE,
    ONE,
    ALL
}

expect class PlatformPlayerService() {
    companion object {
        val instance: PlatformPlayerService?

        fun addListener(listener: PlayerListener)
        fun removeListener(listener: PlayerListener)

        inline fun <reified C: PlatformPlayerService> connect(
            context: PlatformContext,
            controller_class: Class<C>,
            instance: C? = null,
            crossinline onConnected: () -> Unit,
            crossinline onCancelled: () -> Unit
        ): Any

        fun disconnect(context: PlatformContext, connection: Any)
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

    @Composable
    fun Visualiser(colour: Color, modifier: Modifier, opacity: Float)
}
