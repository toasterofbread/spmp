package com.spectre7.spmp.platform

import com.spectre7.spmp.model.Song


actual open class MediaPlayerService actual constructor() : PlatformService() {
    actual open class Listener actual constructor() {
        actual open fun onMediaItemTransition(song: Song?) {
        }

        actual open fun onStateChanged(state: MediaPlayerState) {
        }

        actual open fun onPlayingChanged(is_playing: Boolean) {
        }

        actual open fun onShuffleEnabledChanged(shuffle_enabled: Boolean) {
        }

        actual open fun onRepeatModeChanged(repeat_mode: MediaPlayerRepeatMode) {
        }

        actual open fun onEvents() {
        }
    }

    actual fun release() {
    }

    actual val is_playing: Boolean
        get() = TODO("Not yet implemented")
    actual val song_count: Int
        get() = TODO("Not yet implemented")
    actual val current_song_index: Int
        get() = TODO("Not yet implemented")
    actual val current_position_ms: Long
        get() = TODO("Not yet implemented")
    actual val duration_ms: Long
        get() = TODO("Not yet implemented")
    actual val shuffle_enabled: Boolean
        get() = TODO("Not yet implemented")
    actual val has_next_song: Boolean
        get() = TODO("Not yet implemented")
    actual val has_prev_song: Boolean
        get() = TODO("Not yet implemented")
    actual val state: MediaPlayerState
        get() = TODO("Not yet implemented")
    actual var repeat_mode: MediaPlayerRepeatMode
        get() = TODO("Not yet implemented")
        set(value) {}
    actual var volume: Float
        get() = TODO("Not yet implemented")
        set(value) {}
    actual val has_focus: Boolean
        get() = TODO("Not yet implemented")

    actual open fun play() {
    }

    actual open fun pause() {
    }

    actual open fun playPause() {
    }

    actual open fun seekTo(position_ms: Long) {
    }

    actual open fun seekTo(index: Int, position_ms: Long) {
    }

    actual open fun seekToNext() {
    }

    actual open fun seekToPrevious() {
    }

    actual fun getSong(): Song? {
        TODO("Not yet implemented")
    }

    actual fun getSong(index: Int): Song? {
        TODO("Not yet implemented")
    }

    actual fun moveSong(from: Int, to: Int) {
    }

    actual fun addSong(song: Song) {
    }

    actual fun addSong(song: Song, index: Int) {
    }

    actual fun removeSong(index: Int) {
    }

    actual fun addListener(listener: Listener) {
    }

    actual fun removeListener(listener: Listener) {
    }

}