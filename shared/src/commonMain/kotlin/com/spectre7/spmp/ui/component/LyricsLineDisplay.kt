package com.spectre7.spmp.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.spectre7.spmp.model.Settings
import com.spectre7.spmp.model.Song
import com.spectre7.utils.BasicFuriganaText
import com.spectre7.utils.composable.OnChangedEffect
import com.spectre7.utils.composable.RecomposeOnInterval

private const val UPDATE_INTERVAL_MS = 100L

private fun getCurrentLine(lyrics: Song.Lyrics, time: Long): Int? {
    for (line in lyrics.lines.withIndex()) {
        if (line.value.firstOrNull()?.line_range?.contains(time) == true) {
            return line.index
        }
    }
    return null
}

@Composable
fun LyricsLineDisplay(lyrics: Song.Lyrics, getTime: () -> Long, modifier: Modifier = Modifier) {
    require(lyrics.sync_type != Song.Lyrics.SyncType.NONE)

    val lyrics_linger: Boolean by Settings.KEY_TOPBAR_LYRICS_LINGER.rememberMutableState()

    RecomposeOnInterval(UPDATE_INTERVAL_MS) { s ->
        s

        var current_line: Int? by remember { mutableStateOf(getCurrentLine(lyrics, getTime())) }
        var line_a: Int? by remember { mutableStateOf(current_line) }
        var line_b: Int? by remember { mutableStateOf(null) }
        var show_line_a: Boolean by remember { mutableStateOf(true) }

        OnChangedEffect(getTime()) {
            val line = getCurrentLine(lyrics, getTime())
            if (line == null && lyrics_linger) {
                return@OnChangedEffect
            }

            if (line != current_line) {
                if (show_line_a) {
                    line_b = line
                }
                else {
                    line_a = line
                }
                current_line = line
                show_line_a = !show_line_a
            }
        }

        val enter = slideInVertically { it }
        val exit = slideOutVertically { -it } + fadeOut()

        Box(modifier, contentAlignment = Alignment.Center) {
            AnimatedVisibility(line_a != null && show_line_a, enter = enter, exit = exit) {
                var line by remember { mutableStateOf(line_a) }
                LaunchedEffect(line_a) {
                    if (line_a != null) {
                        line = line_a
                    }
                }

                line?.also {
                    BasicFuriganaText(lyrics.lines[it])
                }
            }
            AnimatedVisibility(line_b != null && !show_line_a, enter = enter, exit = exit) {
                var line by remember { mutableStateOf(line_b) }
                LaunchedEffect(line_b) {
                    if (line_a != null) {
                        line = line_b
                    }
                }

                line?.also {
                    BasicFuriganaText(lyrics.lines[it])
                }
            }
        }
    }
}
