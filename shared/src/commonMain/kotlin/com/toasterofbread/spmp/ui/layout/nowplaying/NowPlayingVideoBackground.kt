package com.toasterofbread.spmp.ui.layout.nowplaying

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.toasterofbread.spmp.platform.isVideoPlaybackSupported
import com.toasterofbread.spmp.platform.SongVideoPlayback
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingExpansionState
import com.toasterofbread.spmp.model.mediaitem.song.Song
import LocalPlayerState
import LocalNowPlayingExpansion

@Composable
fun NowPlayingVideoBackground(
    modifier: Modifier = Modifier,
    getAlpha: () -> Float = { 1f }
): Boolean {
    if (!isVideoPlaybackSupported()) {
        return false
    }

    val player: PlayerState = LocalPlayerState.current
    val expansion: NowPlayingExpansionState = LocalNowPlayingExpansion.current
    val current_song: Song? by player.status.song_state

    current_song?.id?.also { song_id ->
        return SongVideoPlayback(
            song_id,
            modifier = modifier,
            getPositionMs = { player.status.getPositionMs() },
            fill = true,
            getAlpha = {
                getAlpha() * expansion.get().coerceIn(0f..1f)
            }
        )
    }

    return false
}
