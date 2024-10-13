package com.toasterofbread.spmp.widget

import LocalPlayerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.toasterofbread.spmp.model.lyrics.SongLyrics
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.youtubeapi.lyrics.LyricsFuriganaTokeniser
import com.toasterofbread.spmp.youtubeapi.lyrics.createFuriganaTokeniser
import kotlinx.coroutines.delay

private const val UPDATE_INTERVAL_MS = 100L

class SpMpWidgetReceiver: GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SpMpMainWidget()
}

//@Composable
//private fun LyricsDisplay(
//    lyrics: SongLyrics,
//    getTimeMs: () -> Long,
//    colour: Color
//) {
//    val state: CurrentLineState = rememberCurrentLineState(lyrics, true, getTimeMs)
//    Text(state.liiine.toString(), style = TextStyle(color = ColorProvider(colour)))
//}
//
//private class CurrentLineState(val lines: List<List<SongLyrics.Term>>) {
//    private var current_line: Int? by mutableStateOf(null)
//    private var show_line_a: Boolean by mutableStateOf(true)
//
//    private var line_a: Int? by mutableStateOf(current_line)
//    private var line_b: Int? by mutableStateOf(null)
//
//    private fun shouldShowLineA(): Boolean = line_a != null && show_line_a
//    private fun shouldShowLineB(): Boolean = line_b != null && !show_line_a
//
//    val liiine: List<SongLyrics.Term> get() =
//        (if (shouldShowLineA()) line_a else line_b)?.let { lines[it] }.orEmpty()
//
//    fun isLineShowing(): Boolean = shouldShowLineA() || shouldShowLineB()
//
//    fun update(time: Long, linger: Boolean) {
//        val line: Int? = getCurrentLine(time, linger)
//        if (linger && line == null) {
//            return
//        }
//
//        if (line != current_line) {
//            if (show_line_a) {
//                line_b = line
//            }
//            else {
//                line_a = line
//            }
//            current_line = line
//            show_line_a = !show_line_a
//        }
//    }
//
//    @Composable
//    fun LyricsDisplay(LineDisplay: @Composable (show: Boolean, line: List<SongLyrics.Term>?) -> Unit) {
//        LineDisplay(shouldShowLineA(), line_a?.let { lines[it] })
//        LineDisplay(shouldShowLineB(), line_b?.let { lines[it] })
//    }
//
//    private fun getCurrentLine(time: Long, linger: Boolean): Int? {
//        var last_before: Int? = null
//
//        for (line in lines.withIndex()) {
//            val range: LongRange = line.value.firstOrNull()?.line_range ?: continue
//            if (range.contains(time)) {
//                return line.index
//            }
//
//            if (linger && range.last < time) {
//                last_before = line.index
//            }
//        }
//        return last_before
//    }
//}
//
//@Composable
//private fun rememberCurrentLineState(
//    lyrics: SongLyrics,
//    linger: Boolean,
//    getTime: () -> Long
//): CurrentLineState {
//    val player: PlayerState = LocalPlayerState.current
//    val romanise_furigana: Boolean by player.settings.lyrics.ROMANISE_FURIGANA.observe()
//
//    val state: CurrentLineState = remember(romanise_furigana) {
//        val tokeniser: LyricsFuriganaTokeniser = createFuriganaTokeniser(romanise_furigana)
//        val lines: List<List<SongLyrics.Term>> = lyrics.lines.map { tokeniser.mergeAndFuriganiseTerms(it) }
//        CurrentLineState(lines).apply { update(getTime(), linger) }
//    }
//
//    LaunchedEffect(linger) {
//        while (true) {
//            delay(UPDATE_INTERVAL_MS)
//            state.update(getTime(), linger)
//        }
//    }
//
//    return state
//}
