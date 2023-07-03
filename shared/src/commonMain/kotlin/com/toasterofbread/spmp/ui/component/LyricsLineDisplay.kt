package com.toasterofbread.spmp.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.toasterofbread.spmp.model.SongLyrics
import com.toasterofbread.utils.BasicFuriganaText
import com.toasterofbread.utils.composable.OnChangedEffect
import com.toasterofbread.utils.composable.RecomposeOnInterval

private const val UPDATE_INTERVAL_MS = 100L

private fun getCurrentLine(lyrics: SongLyrics, time: Long, linger: Boolean): Int? {
    var last_before: Int? = null
    for (line in lyrics.lines.withIndex()) {
        val range = line.value.firstOrNull()?.line_range ?: continue
        if (range.contains(time)) {
            return line.index
        }

        if (linger && range.last < time) {
            last_before = line.index
        }
    }
    return last_before
}

@Composable
fun LyricsLineDisplay(
    lyrics: SongLyrics,
    getTime: () -> Long,
    lyrics_linger: Boolean = true,
    show_furigana: Boolean = true,
    modifier: Modifier = Modifier
) {
    require(lyrics.synced)

    RecomposeOnInterval(UPDATE_INTERVAL_MS) { s ->
        s

        var current_line: Int? by remember { mutableStateOf(getCurrentLine(lyrics, getTime(), lyrics_linger)) }
        var line_a: Int? by remember { mutableStateOf(current_line) }
        var line_b: Int? by remember { mutableStateOf(null) }
        var show_line_a: Boolean by remember { mutableStateOf(true) }

        OnChangedEffect(getTime()) {
            val line = getCurrentLine(lyrics, getTime(), lyrics_linger)
            if (lyrics_linger && line == null) {
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
                    BasicFuriganaText(lyrics.lines[it], show_readings = show_furigana)
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
                    BasicFuriganaText(lyrics.lines[it], show_readings = show_furigana)
                }
            }
        }
    }
}