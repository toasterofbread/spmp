package com.spectre7.spmp.ui.component

import LocalPlayerState
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.spectre7.spmp.model.MusicTopBarMode
import com.spectre7.spmp.model.Settings
import com.spectre7.spmp.model.mediaitem.Song
import com.spectre7.spmp.model.SongLyrics
import com.spectre7.spmp.platform.composable.platformClickable
import com.spectre7.utils.composable.rememberSongUpdateLyrics

private fun getModeState(mode: MusicTopBarMode, song: Song?): Any? {
    return when (mode) {
        MusicTopBarMode.LYRICS -> song?.lyrics?.lyrics?.let {  lyrics ->
            if (lyrics.synced) lyrics else null
        }
        MusicTopBarMode.VISUALISER -> mode
        MusicTopBarMode.NONE -> mode
    }
}

@Composable
fun MusicTopBar(
    song: Song?,
    default_mode: MusicTopBarMode,
    modifier: Modifier = Modifier,
    target_mode_state: MutableState<MusicTopBarMode> = remember { mutableStateOf(default_mode) },
    mode_state: MutableState<MusicTopBarMode> = remember { mutableStateOf(MusicTopBarMode.NONE) }
) {
    var target_mode by target_mode_state
    val song_state by rememberSongUpdateLyrics(song, target_mode == MusicTopBarMode.LYRICS)
    val player = LocalPlayerState.current

    val visualiser_width: Float by Settings.KEY_TOPBAR_VISUALISER_WIDTH.rememberMutableState()
    check(visualiser_width in 0f .. 1f)

    val current_state by remember {
        derivedStateOf {
            for (mode_i in target_mode.ordinal downTo 0) {
                val mode = MusicTopBarMode.values()[mode_i]
                val state = getModeState(mode, song_state)
                if (state != null) {
                    mode_state.value = mode
                    return@derivedStateOf state
                }
            }
            throw NotImplementedError(target_mode.toString())
        }
    }

    Box(modifier, contentAlignment = Alignment.Center) {
        val mode_toast_alpha = remember { Animatable(0f) }
        val coroutine_scope = rememberCoroutineScope()

        LaunchedEffect(target_mode) {
            coroutine_scope.launchSingle {
                toast_alpha.animateTo(1f)
                delay(1000)
                toast_alpha.animateTo(0f)
            }
        }

        Crossfade(Pair(target_mode, mode_state.value)) { state ->
            val (target, current) = state

            Row(
                Modifier
                    .background(LocalContentColor.current, RoundedCornerShape(10.dp))
                    .graphicsLayer{ alpha = mode_toast_alpha.value * 0.8f }
                    .clickable {
                        if (mode_toast_alpha.value == 1f) {
                            coroutine_scope.launchSingle {
                                toast_alpha.animateTo(0f)
                            }
                        }
                    },
                verticalAlignment = Alignment.CenterVertically, 
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    target.getIcon(),
                    null,
                    tint = LocalContentColor.current.getContrasted()
                )

                if (target != current) {
                    Text(getString("topbar_mode_unavailable"))
                }
            }
        }

        Crossfade(
            current_state,
            modifier.platformClickable(
                onClick = {
                    if (target_mode.ordinal == 0) {
                        target_mode = MusicTopBarMode.values().last()
                    }
                    else {
                        target_mode = MusicTopBarMode.values()[target_mode.ordinal - 1]
                    }
                },
                onAltClick = {
                    if (current_state is SongLyrics) {
                        TODO()
                    }
                }
            )
        ) { s ->
            when (s) {
                is SongLyrics -> {
                    val linger: Boolean by Settings.KEY_TOPBAR_LYRICS_LINGER.rememberMutableState()
                    val show_furigana: Boolean by Settings.KEY_TOPBAR_LYRICS_SHOW_FURIGANA.rememberMutableState()

                    LyricsLineDisplay(
                        s,
                        { player.player.current_position_ms + (song?.song_reg_entry?.getLyricsSyncOffset() ?: 0) },
                        linger,
                        show_furigana
                    )
                }
                MusicTopBarMode.VISUALISER -> {
                    player.player.Visualiser(
                        LocalContentColor.current,
                        Modifier.fillMaxHeight().fillMaxWidth(visualiser_width).padding(vertical = 10.dp),
                        opacity = 0.5f
                    )
                }
                else -> {
                    // TOOD State indicator
                }
            }
        }
    }

}
