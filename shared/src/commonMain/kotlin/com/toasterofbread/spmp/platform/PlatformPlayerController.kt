package com.toasterofbread.spmp.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.platform.playerservice.PlayerServiceState
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

expect class PlatformPlayerController() {
    val context: PlatformContext
    val service_state: PlayerServiceState

    fun onCreate()
    fun onDestroy()

    @Composable
    fun Visualiser(colour: Color, modifier: Modifier = Modifier, opacity: Float = 1f)

    val session_started: Boolean
    val state: MediaPlayerState
    val is_playing: Boolean
    val song_count: Int
    val current_song_index: Int
    val current_position_ms: Long
    val duration_ms: Long
    val undo_count: Int
    val redo_count: Int
    val radio_state: RadioInstance.RadioState
    val active_queue_index: Int

    var repeat_mode: MediaPlayerRepeatMode
    var volume: Float

    val has_focus: Boolean

    fun undo()
    fun redo()
    fun undoAll()
    fun redoAll()

    open fun play()
    open fun pause()
    open fun playPause()

    open fun seekTo(position_ms: Long)
    open fun seekBy(delta_ms: Long)
    open fun seekToSong(index: Int)
    open fun seekToNext()
    open fun seekToPrevious()

    fun getSong(): Song?
    fun getSong(index: Int): Song?

    fun continueRadio(is_retry: Boolean)
    fun dismissRadioLoadError()
    fun setRadioFilter(filter_index: Int?)

    fun startRadioAtIndex(
        index: Int,
        item: MediaItem? = null,
        item_index: Int? = null,
        add_item: Boolean = false,
        skip_first: Boolean = false,
        shuffle: Boolean = false,
        on_load_seek_index: Int? = null
    )

    fun cancelSession()

    fun addSong(song: Song)
    fun addSong(song: Song, index: Int, is_active_queue: Boolean = false, start_radio: Boolean = false)
    fun addMultipleSongs(
        songs: List<Song>,
        index: Int = 0,
        skip_first: Boolean = false,
        is_active_queue: Boolean = false,
        skip_existing: Boolean = false,
        clear: Boolean = false
    )

    fun moveSong(from: Int, to: Int)
    fun removeSong(index: Int)
    fun removeMultipleSongs(indices: List<Int>)

    fun clearQueue(from: Int = 0, keep_current: Boolean = false, cancel_radio: Boolean = true)
    fun shuffleQueue(start: Int = 0, end: Int = -1)
    fun shuffleQueueIndices(indices: List<Int>)

    fun triggerSavePersistentQueue()
    fun setStopAfterCurrentSong(value: Boolean)

    fun updateActiveQueueIndex(delta: Int = 0)
    fun setActiveQueueIndex(value: Int)

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

inline fun PlatformPlayerController.iterateSongs(action: (i: Int, song: Song) -> Unit) {
    for (i in 0 until song_count) {
        action(i, getSong(i)!!)
    }
}
