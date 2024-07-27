package com.toasterofbread.spmp.platform

import com.toasterofbread.spmp.model.mediaitem.song.Song
import dev.toastbits.spms.socketapi.shared.SpMsPlayerRepeatMode
import dev.toastbits.spms.socketapi.shared.SpMsPlayerState

actual abstract class PlayerListener actual constructor() {
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
}
