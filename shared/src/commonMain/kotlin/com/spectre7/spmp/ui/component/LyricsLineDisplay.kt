package com.spectre7.spmp.ui.component

private const val UPDATE_INTERVAL_MS = 100L

@Composable
fun LyricsLineDisplay(lyrics: Lyrics, getTime: () -> Long, getColour: () -> Color, modifier: Modifier = Modifier) {
    RecomposeOnInterval(UPDATE_INTERVAL_MS) { s ->
        s

        val current_line by remember { derivedStateOf {
            val line = lyrics.getLine(getTime())
            if (line == null)
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
        AnimatedVisibility(line_b && b, enter = enter, exit = exit) {
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
