package com.toasterofbread.spmp.platform

import com.toasterofbread.spmp.model.mediaitem.song.Song

expect abstract class PlayerListener() {
    open fun onSongTransition(song: Song?, manual: Boolean)
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
