package com.spectre7.spmp.ui.component

import androidx.compose.animation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.spectre7.spmp.model.Song
import com.spectre7.utils.BasicFuriganaText
import com.spectre7.utils.RecomposeOnInterval

private const val UPDATE_INTERVAL_MS = 100L

@Composable
fun LyricsLineDisplay(lyrics: Song.Lyrics, getTime: () -> Long, getColour: () -> Color, modifier: Modifier = Modifier) {
    RecomposeOnInterval(UPDATE_INTERVAL_MS) { s ->
        s

        var current_line: Int? by remember { mutableStateOf(null) }
        LaunchedEffect(getTime()) {
            val time = getTime()
            for (line in lyrics.lines.withIndex()) {
                if (line.value.firstOrNull()?.line_range?.contains(time) == true) {
                    current_line = line.index
                }
            }
        }

        var line_a: Int? by remember { mutableStateOf(null) }
        var line_b: Int? by remember { mutableStateOf(null) }
        var a: Boolean by remember { mutableStateOf(true) }

        LaunchedEffect(current_line) {
            if (current_line == null) {
                return@LaunchedEffect
            }

            if (a) {
                line_b = current_line
            }
            else {
                line_a = current_line
            }
            a = !a
        }

        val enter = slideInVertically { it }
        val exit = slideOutVertically { -it } + fadeOut()

        Box(modifier, contentAlignment = Alignment.Center) {
            AnimatedVisibility(line_a != null && a, enter = enter, exit = exit) {
                var line by remember { mutableStateOf(line_a) }
                LaunchedEffect(line_a) {
                    if (line_a != null) {
                        line = line_a
                    }
                }

                line?.also {
                    BasicFuriganaText(lyrics.lines[it], text_colour = getColour())
                }
            }
            AnimatedVisibility(line_b != null && !a, enter = enter, exit = exit) {
                var line by remember { mutableStateOf(line_b) }
                LaunchedEffect(line_b) {
                    if (line_a != null) {
                        line = line_b
                    }
                }

                line?.also {
                    BasicFuriganaText(lyrics.lines[it], text_colour = getColour())
                }
            }
        }
    }
}

//@Composable
//private fun LyricsLine(line: List<Song.Lyrics.Term>) {
//    val data = remember(line) {
//        val terms: MutableList<TextData> = mutableListOf()
//        for (term in line.withIndex()) {
//            for (subterm in term.value.subterms.withIndex()) {
//                if (term.index + 1 == line.size && subterm.index + 1 == term.value.subterms.size) {
//                    terms.add(TextData(subterm.value.text + "\n", subterm.value.furi))
//                }
//                else {
//                    terms.add(TextData(subterm.value.text, subterm.value.furi))
//                }
//            }
//        }
//        terms
//    }
//
////    ShortFuriganaText(data)
//}
