package com.spectre7.spmp.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.spectre7.spmp.model.Song
import com.spectre7.utils.LongFuriganaText
import com.spectre7.utils.RecomposeOnInterval
import com.spectre7.utils.TextData

private const val UPDATE_INTERVAL_MS = 100L

@Composable
fun LyricsLineDisplay(lyrics: Song.Lyrics, getTime: () -> Long, getColour: () -> Color, modifier: Modifier = Modifier) {
    RecomposeOnInterval(UPDATE_INTERVAL_MS) { s ->
        s

        val current_line by remember { derivedStateOf {
            lyrics.getLine(getTime())?.let { lyrics.lines[it] }
        } }

        var line_a: List<Song.Lyrics.Term>? by remember { mutableStateOf(null) }
        var line_b: List<Song.Lyrics.Term>? by remember { mutableStateOf(null) } 
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

        val enter = slideInVertically()
        val exit = slideOutVertically()

        AnimatedVisibility(line_a != null && a, enter = enter, exit = exit) {
            var line by remember { mutableStateOf(line_a) }
            LaunchedEffect(line_a) {
                if (line_a != null) {
                    line = line_a
                }
            }

            line?.also { LyricsLine(it) }
        }
        AnimatedVisibility(line_b != null && !a, enter = enter, exit = exit) {
            var line by remember { mutableStateOf(line_b) }
            LaunchedEffect(line_b) {
                if (line_a != null) {
                    line = line_b
                }
            }

            line?.also { LyricsLine(it) }
        }
    }
}

@Composable
private fun LyricsLine(line: List<Song.Lyrics.Term>) {
    val data = remember(line) { line.flatMap { term ->
        term.subterms.map { subterm ->
            TextData(subterm.text, subterm.furi)
        }
    } }
    
    LongFuriganaText(data)
}
