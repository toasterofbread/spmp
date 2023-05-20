package com.spectre7.spmp.ui.component

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.spectre7.spmp.PlayerServiceHost
import com.spectre7.spmp.model.MusicTopBarMode
import com.spectre7.spmp.model.Song
import com.spectre7.spmp.platform.composable.platformClickable
import com.spectre7.utils.composable.rememberSongUpdateLyrics

private fun getModeState(mode: MusicTopBarMode, song: Song?): Any? {
    return when (mode) {
        MusicTopBarMode.LYRICS -> song?.lyrics?.lyrics?.let {  lyrics ->
            if (lyrics.sync_type != Song.Lyrics.SyncType.NONE) lyrics else null
        }
        MusicTopBarMode.VISUALISER -> if (PlayerServiceHost.status.m_playing) mode else null
    }
}

@Composable
fun MusicTopBar(
    song: Song?,
    default_mode: MusicTopBarMode,
    modifier: Modifier = Modifier
) {
    var target_mode: MusicTopBarMode by remember(default_mode) { mutableStateOf(default_mode) }
    val song_state by rememberSongUpdateLyrics(song, target_mode == MusicTopBarMode.LYRICS)

    val current_state by remember {
        derivedStateOf {
            for (mode in target_mode.ordinal downTo 0) {
                val state = getModeState(MusicTopBarMode.values()[mode], song_state)
                if (state != null) {
                    return@derivedStateOf state
                }
            }
            return@derivedStateOf null
        }
    }

    Crossfade(
        current_state,
        modifier.platformClickable(
            onClick = {
                if (target_mode.ordinal + 1 == MusicTopBarMode.values().size) {
                    target_mode = MusicTopBarMode.values()[0]
                }
                else {
                    target_mode = MusicTopBarMode.values()[target_mode.ordinal + 1]
                }
            },
            onAltClick = {
                if (current_state is Song.Lyrics) {
                    TODO()
                }
            }
        )
    ) { s ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when (s) {
                is Song.Lyrics ->
                    LyricsLineDisplay(
                        s,
                        { PlayerServiceHost.status.position_ms + 500 }
                    )
                MusicTopBarMode.VISUALISER ->
                    PlayerServiceHost.player.Visualiser(
                        LocalContentColor.current,
                        Modifier.fillMaxSize().padding(vertical = 10.dp),
                        opacity = 0.5f
                    )
            }
        }
    }
}
