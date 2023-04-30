package com.spectre7.spmp.ui.component

import androidx.compose.animation.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.spectre7.spmp.model.Song
import com.spectre7.utils.RecomposeOnInterval
import com.spectre7.utils.ShortFuriganaText
import com.spectre7.utils.TextData

private const val UPDATE_INTERVAL_MS = 100L

// TODO Load all lyrics first

@Composable
fun LyricsLineDisplay(lyrics: Song.Lyrics, getTime: () -> Long, getColour: () -> Color, modifier: Modifier = Modifier) {
    RecomposeOnInterval(UPDATE_INTERVAL_MS) { s ->
        s

        val current_line = lyrics.getLine(getTime())?.let { lyrics.lines[it] }

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

        val enter = slideInVertically() + fadeIn()
        val exit = slideOutVertically() + fadeOut()

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
    val data = remember(line) {
        val terms: MutableList<TextData> = mutableListOf()
        for (term in line.withIndex()) {
            for (subterm in term.value.subterms.withIndex()) {
                if (term.index + 1 == line.size && subterm.index + 1 == term.value.subterms.size) {
                    terms.add(TextData(subterm.value.text + "\n", subterm.value.furi))
                }
                else {
                    terms.add(TextData(subterm.value.text, subterm.value.furi))
                }
            }
        }
        terms
    }

    ShortFuriganaText(data)
}
