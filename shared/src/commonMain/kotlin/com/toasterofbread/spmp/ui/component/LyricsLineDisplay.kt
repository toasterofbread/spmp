package com.toasterofbread.spmp.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.model.SongLyrics
import com.toasterofbread.utils.common.BasicFuriganaText
import kotlinx.coroutines.delay

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
    modifier: Modifier = Modifier,
    text_colour: Color = LocalContentColor.current,
    emptyContent: (@Composable () -> Unit)? = null
) {
    require(lyrics.synced)

    var current_line: Int? by remember { mutableStateOf(getCurrentLine(lyrics, getTime(), lyrics_linger)) }
    var line_a: Int? by remember { mutableStateOf(current_line) }
    var line_b: Int? by remember { mutableStateOf(null) }
    var show_line_a: Boolean by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(UPDATE_INTERVAL_MS)

            val line = getCurrentLine(lyrics, getTime(), lyrics_linger)
            if (lyrics_linger && line == null) {
                continue
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
    }

    val enter = slideInVertically { it }
    val exit = slideOutVertically { -it } + fadeOut()

    val max_lines: Int by Settings.KEY_LYRICS_TOP_BAR_MAX_LINES.rememberMutableState()

    Box(modifier.height(IntrinsicSize.Min), contentAlignment = Alignment.Center) {
        val show_a = line_a != null && show_line_a
        val show_b = line_b != null && !show_line_a

        @Composable
        fun phase(show: Boolean, index: Int?) {
            AnimatedVisibility(show, Modifier.height(IntrinsicSize.Min), enter = enter, exit = exit) {
                var line by remember { mutableStateOf(index) }
                LaunchedEffect(index) {
                    if (index != null) {
                        line = index
                    }
                }

                line?.also {
                    BasicFuriganaText(lyrics.lines[it], show_readings = show_furigana, text_colour = text_colour, max_lines = max_lines)
                }
            }
        }

        phase(show_a, line_a)
        phase(show_b, line_b)

        Crossfade(if (show_a || show_b) null else emptyContent, Modifier.fillMaxWidth()) { content ->
            if (content != null) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    content()
                }
            }
        }
    }
}
