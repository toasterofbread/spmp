package com.toasterofbread.spmp.ui.component

import androidx.compose.animation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import com.toasterofbread.composekit.platform.Platform
import com.toasterofbread.composekit.utils.composable.AlignableCrossfade
import com.toasterofbread.spmp.model.lyrics.SongLyrics
import com.toasterofbread.spmp.model.settings.category.TopBarSettings
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
    modifier: Modifier = Modifier,
    text_colour: Color = LocalContentColor.current,
    text_align: TextAlign = TextAlign.Center,
    emptyContent: (@Composable () -> Unit)? = null
) {
    require(lyrics.synced)

    val lyrics_linger: Boolean by TopBarSettings.Key.LYRICS_LINGER.rememberMutableState()
    val show_furigana: Boolean by TopBarSettings.Key.LYRICS_SHOW_FURIGANA.rememberMutableState()
    val max_lines: Int by TopBarSettings.Key.LYRICS_MAX_LINES.rememberMutableState()
    val preallocate_max_space: Boolean by TopBarSettings.Key.LYRICS_PREAPPLY_MAX_LINES.rememberMutableState()

    var current_line: Int? by remember { mutableStateOf(getCurrentLine(lyrics, getTime(), lyrics_linger)) }
    var line_a: Int? by remember { mutableStateOf(current_line) }
    var line_b: Int? by remember { mutableStateOf(null) }
    var show_line_a: Boolean by remember { mutableStateOf(true) }

    val lyrics_text_style: TextStyle =
        LocalTextStyle.current.copy(
            fontSize = when (Platform.current) {
                Platform.ANDROID -> 16.sp
                Platform.DESKTOP -> 20.sp
            },
            color = text_colour,
            textAlign = text_align
        )

    LaunchedEffect(Unit) {
        while (true) {
            delay(UPDATE_INTERVAL_MS)

            val line: Int? = getCurrentLine(lyrics, getTime(), lyrics_linger)
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

    val enter: EnterTransition = slideInVertically { it }
    val exit: ExitTransition = slideOutVertically { -it } + fadeOut()

    Box(modifier.height(IntrinsicSize.Min), contentAlignment = Alignment.Center) {
        val show_a: Boolean = line_a != null && show_line_a
        val show_b: Boolean = line_b != null && !show_line_a

        @Composable
        fun phase(show: Boolean, index: Int?) {
            AnimatedVisibility(show, Modifier.height(IntrinsicSize.Min).width(IntrinsicSize.Max), enter = enter, exit = exit) {
                var line: Int? by remember { mutableStateOf(index) }
                LaunchedEffect(index) {
                    if (index != null) {
                        line = index
                    }
                }

                line?.also {
                    BasicFuriganaText(
                        lyrics.lines[it],
                        show_readings = show_furigana,
                        style = lyrics_text_style,
                        max_lines = max_lines,
                        preallocate_needed_space = preallocate_max_space
                    )
                }
            }
        }

        phase(show_a, line_a)
        phase(show_b, line_b)

        AlignableCrossfade(
            if (show_a || show_b) null else emptyContent,
            Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) { content ->
            if (content != null) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    content()
                }
            }
        }
    }
}
