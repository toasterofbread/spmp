package com.spectre7.spmp.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.spectre7.spmp.model.Song
import kotlinx.coroutines.CoroutineScope

internal const val AUTO_DOWNLOAD_SOFT_TIMEOUT = 1500 // ms

enum class MediaPlayerState {
    IDLE,
    BUFFERING,
    READY,
    ENDED
}

enum class MediaPlayerRepeatMode {
    OFF, 
    ONE, 
    ALL
}

expect open class MediaPlayerService(): PlatformService {
    interface UndoRedoAction {
        fun undo()
        fun redo()
    }

    open class Listener() {
        open fun onSongTransition(song: Song?)
        open fun onStateChanged(state: MediaPlayerState)
        open fun onPlayingChanged(is_playing: Boolean)
        open fun onRepeatModeChanged(repeat_mode: MediaPlayerRepeatMode)
        open fun onVolumeChanged(volume: Float)
        open fun onDurationChanged(duration_ms: Long)
        open fun onSeeked(position_ms: Long)
        open fun onUndoStateChanged()

        open fun onSongAdded(index: Int, song: Song)
        open fun onSongRemoved(index: Int)
        open fun onSongMoved(from: Int, to: Int)

        open fun onEvents()
    }

    var session_started: Boolean
        private set

    val state: MediaPlayerState
    val is_playing: Boolean
    val song_count: Int
    val current_song_index: Int
    val current_position_ms: Long
    val duration_ms: Long
    val undo_count: Int
    val redo_count: Int

    var repeat_mode: MediaPlayerRepeatMode
    var volume: Float

    val has_focus: Boolean
    val supports_waveform: Boolean

    @Composable
    fun Visualiser(colour: Color, modifier: Modifier = Modifier, opacity: Float = 1f)

    fun undoableAction(action: MediaPlayerService.() -> Unit)
    fun undoableActionWithCustom(action: MediaPlayerService.() -> UndoRedoAction?)

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

    companion object {
        fun CoroutineScope.playerLaunch(action: CoroutineScope.() -> Unit)
    }
}