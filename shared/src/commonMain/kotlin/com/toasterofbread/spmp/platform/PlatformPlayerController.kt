package com.toasterofbread.spmp.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.toasterofbread.spmp.platform.playerservice.PlayerServiceState

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

expect abstract class PlatformPlayerController() {
    var session_started: Boolean

    val context: PlatformContext
    val service_state: PlayerServiceState

    open fun onCreate()
    open fun onDestroy()

    @Composable
    fun Visualiser(colour: Color, modifier: Modifier = Modifier, opacity: Float = 1f)

    fun redo()
    fun redoAll()
    fun undo()
    fun undoAll()

    open fun play()
    open fun pause()
    open fun playPause()

    open fun seekTo(position_ms: Long)
    open fun seekToSong(index: Int)
    open fun seekToNext()
    open fun seekToPrevious()

    fun getSong(): Song?
    fun getSong(index: Int): Song?

    fun addSong(song: Song)
    fun addSong(song: Song, index: Int)
    fun moveSong(from: Int, to: Int)
    fun removeSong(index: Int)

    fun addListener(listener: Listener)
    fun removeListener(listener: Listener)

    fun triggerSavePersistentQueue()
    fun setStopAfterCurrentSong(value: Boolean)

    companion object {
        fun <C: PlatformPlayerController> connect(
            context: PlatformContext,
            controller_class: Class<C>,
            instance: C? = null,
            onConnected: (service: C) -> Unit,
            onCancelled: () -> Unit
        ): Any
        fun disconnect(context: PlatformContext, connection: Any)
    }
}
