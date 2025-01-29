package com.toasterofbread.spmp.ui.util

import LocalPlayerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.toasterofbread.spmp.model.lyrics.SongLyrics
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.youtubeapi.lyrics.LyricsFuriganaTokeniser
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class LyricsLineState(
    val lines: List<List<SongLyrics.Term>>,
    val lyrics: SongLyrics
) {
    private var current_line: Int? by mutableStateOf(null)
    private var show_line_a: Boolean by mutableStateOf(true)

    private var line_a: Int? by mutableStateOf(current_line)
    private var line_b: Int? by mutableStateOf(null)

    private fun shouldShowLineA(): Boolean = line_a != null && show_line_a
    private fun shouldShowLineB(): Boolean = line_b != null && !show_line_a

    fun isLineShowing(): Boolean = shouldShowLineA() || shouldShowLineB()

    fun update(time: Long, linger: Boolean) {
        val line: Int? = getCurrentLine(time, linger)
        if (linger && line == null) {
            return
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

    @Composable
    fun LyricsDisplay(LineDisplay: @Composable (show: Boolean, line: List<SongLyrics.Term>?) -> Unit) {
        LineDisplay(shouldShowLineA(), line_a?.let { lines[it] })
        LineDisplay(shouldShowLineB(), line_b?.let { lines[it] })
    }

    private fun getCurrentLine(time: Long, linger: Boolean): Int? {
        var last_before: Int? = null

        for (line in lines.withIndex()) {
            val range: LongRange = line.value.firstOrNull()?.line_range ?: continue
            if (range.contains(time)) {
                return line.index
            }

            if (linger && range.last < time) {
                last_before = line.index
            }
        }
        return last_before
    }

    companion object {
        @Composable
        fun rememberCurrentLineState(
            lyrics: SongLyrics,
            linger: Boolean,
            update_interval: Duration? = 100.milliseconds,
            getTime: () -> Long
        ): LyricsLineState? {
            val player: PlayerState = LocalPlayerState.current
            val romanise_furigana: Boolean by player.settings.Lyrics.ROMANISE_FURIGANA.observe()

            var state: LyricsLineState? by remember { mutableStateOf(null) }

            LaunchedEffect(lyrics.lines, romanise_furigana) {
                val tokeniser: LyricsFuriganaTokeniser? = LyricsFuriganaTokeniser.getInstance()
                val lines: List<List<SongLyrics.Term>> =
                    if (tokeniser != null) lyrics.lines.map { tokeniser.mergeAndFuriganiseTerms(it, romanise_furigana) }
                    else lyrics.lines

                state = LyricsLineState(lines, lyrics).apply { update(getTime(), linger) }
            }

            LaunchedEffect(lyrics, linger, state, update_interval) {
                val current_state: LyricsLineState = state ?: return@LaunchedEffect
                if (update_interval == null) {
                    current_state.update(getTime(), linger)
                    return@LaunchedEffect
                }

                while (true) {
                    delay(update_interval)
                    current_state.update(getTime(), linger)
                }
            }

            return state
        }

    }
}
