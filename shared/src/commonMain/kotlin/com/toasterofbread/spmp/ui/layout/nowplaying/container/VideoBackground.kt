package com.toasterofbread.spmp.ui.layout.nowplaying.container

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.toasterofbread.spmp.platform.isVideoPlaybackSupported
import com.toasterofbread.spmp.platform.SongVideoPlayback
import LocalAppState
import com.toasterofbread.spmp.ui.layout.nowplaying.PlayerExpansionState
import com.toasterofbread.spmp.model.mediaitem.song.Song
import LocalPlayerState
import LocalNowPlayingExpansion

@Composable
internal fun VideoBackground(
    modifier: Modifier = Modifier,
    getAlpha: () -> Float = { 1f }
): Boolean {
    if (!isVideoPlaybackSupported()) {
        return false
    }

    val state: SpMp.State = LocalAppState.current
    val expansion: PlayerExpansionState = LocalNowPlayingExpansion.current
    val current_song: Song? by state.session.status.song_state

    current_song?.id?.also { song_id ->
        return SongVideoPlayback(
            song_id,
            modifier = modifier,
            getPositionMs = { state.session.status.getPositionMs() },
            fill = true,
            getAlpha = {
                getAlpha() * expansion.get().coerceIn(0f..1f)
            }
        )
    }

    return false
}
