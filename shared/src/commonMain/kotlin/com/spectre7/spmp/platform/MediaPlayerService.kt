package com.spectre7.spmp.platform

import com.spectre7.spmp.model.Song

internal const val AUTO_DOWNLOAD_SOFT_TIMEOUT = 1500 // ms

enum class MediaPlayerState {
    STATE_IDLE,
    STATE_BUFFERING,
    STATE_READY,
    STATE_ENDED
}

enum class MediaPlayerRepeatMode {
    REPEAT_MODE_OFF, REPEAT_MODE_ONE, REPEAT_MODE_ALL
}

expect open class MediaPlayerService(): PlatformService {
    open class Listener() {
        open fun onMediaItemTransition(song: Song?)
        open fun onStateChanged(state: MediaPlayerState)
        open fun onPlayingChanged(is_playing: Boolean)
        open fun onShuffleEnabledChanged(shuffle_enabled: Boolean)
        open fun onRepeatModeChanged(repeat_mode: MediaPlayerRepeatMode)

        open fun onEvents()
    }

    fun release()

    val is_playing: Boolean
    val song_count: Int
    val current_song_index: Int
    val current_position_ms: Long
    val duration_ms: Long
    val shuffle_enabled: Boolean
    val has_next_song: Boolean
    val has_prev_song: Boolean
    val state: MediaPlayerState

    var repeat_mode: MediaPlayerRepeatMode
    var volume: Float

    val has_focus: Boolean

    open fun play()
    open fun pause()
    open fun playPause()

    open fun seekTo(position_ms: Long)
    open fun seekTo(index: Int, position_ms: Long = 0)
    open fun seekToNext()
    open fun seekToPrevious()

    fun getSong(): Song?
    fun getSong(index: Int): Song?
    fun moveSong(from: Int, to: Int)

    fun addSong(song: Song)
    fun addSong(song: Song, index: Int)
    fun removeSong(index: Int)

    fun addListener(listener: Listener)
    fun removeListener(listener: Listener)
}