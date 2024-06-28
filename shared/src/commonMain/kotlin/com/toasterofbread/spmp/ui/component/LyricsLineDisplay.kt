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
import dev.toastbits.composekit.platform.Platform
import dev.toastbits.composekit.utils.composable.AlignableCrossfade
import dev.toastbits.composekit.utils.composable.NullableValueAnimatedVisibility
import com.toasterofbread.spmp.model.lyrics.SongLyrics
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.youtubeapi.lyrics.LyricsFuriganaTokeniser
import com.toasterofbread.spmp.youtubeapi.lyrics.createFuriganaTokeniser
import kotlinx.coroutines.delay
import LocalPlayerState

private const val UPDATE_INTERVAL_MS = 100L

@Composable
fun HorizontalLyricsLineDisplay(
    lyrics: SongLyrics,
    getTime: () -> Long,
    modifier: Modifier = Modifier,
    text_colour: Color = LocalContentColor.current,
    text_align: TextAlign = TextAlign.Center,
    lyrics_linger: Boolean = false,
    show_furigana: Boolean? = null,
    // max_lines: Int = 1,
    // preallocate_max_space: Boolean = false,
    emptyContent: (@Composable () -> Unit)? = null
) {
    require(lyrics.synced)

    val show_furigana_option: Boolean by LocalPlayerState.current.settings.lyrics.DEFAULT_FURIGANA.observe()
    val current_line_state: CurrentLineState = rememberCurrentLineState(lyrics, lyrics_linger, getTime)

    val lyrics_text_style: TextStyle =
        LocalTextStyle.current.copy(
            fontSize = when (Platform.current) {
                Platform.ANDROID -> 16.sp
                Platform.DESKTOP -> 20.sp
            },
            color = text_colour,
            textAlign = text_align
        )

    Box(
        modifier.height(IntrinsicSize.Min),
        contentAlignment = when (text_align) {
            TextAlign.Start -> Alignment.CenterStart
            TextAlign.End -> Alignment.CenterEnd
            else -> Alignment.Center
        }
    ) {
        current_line_state.LyricsDisplay { show, line ->
            NullableValueAnimatedVisibility(
                line?.takeIf { show },
                Modifier.height(IntrinsicSize.Min).width(IntrinsicSize.Max),
                enter = slideInVertically { it },
                exit = slideOutVertically { -it } + fadeOut()
            ) { current_line ->
                if (current_line == null) {
                    return@NullableValueAnimatedVisibility
                }

                HorizontalFuriganaText(
                    current_line,
                    show_readings = show_furigana ?: show_furigana_option,
                    style = lyrics_text_style,
                    // max_lines = max_lines,
                    // preallocate_needed_space = preallocate_max_space
                )
            }
        }

        AlignableCrossfade(
            if (current_line_state.isLineShowing()) null else emptyContent,
            Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) { content ->
            if (content != null) {
                Box(
                    Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
fun VerticalLyricsLineDisplay(
    lyrics: SongLyrics,
    getTime: () -> Long,
    modifier: Modifier = Modifier,
    text_colour: Color = LocalContentColor.current,
    lyrics_linger: Boolean = false,
    show_furigana: Boolean = false
) {
    require(lyrics.synced)

    val lyrics_text_style: TextStyle =
        LocalTextStyle.current.copy(
            fontSize = when (Platform.current) {
                Platform.ANDROID -> 16.sp
                Platform.DESKTOP -> 20.sp
            },
            color = text_colour
        )

    val current_line_state: CurrentLineState = rememberCurrentLineState(lyrics, lyrics_linger, getTime)

    Box(
        modifier,
        contentAlignment = Alignment.Center
    ) {
        current_line_state.LyricsDisplay { show, line ->
            NullableValueAnimatedVisibility(
                line?.takeIf { show },
                Modifier.height(IntrinsicSize.Min).width(IntrinsicSize.Max),
                enter = slideInHorizontally { -it },
                exit = slideOutHorizontally { it } + fadeOut()
            ) { current_line ->
                if (current_line == null) {
                    return@NullableValueAnimatedVisibility
                }

                VerticalFuriganaText(
                    current_line,
                    show_readings = show_furigana,
                    style = lyrics_text_style
                )
            }
        }
    }
}

private class CurrentLineState(val lines: List<List<SongLyrics.Term>>) {
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
}

@Composable
private fun rememberCurrentLineState(
    lyrics: SongLyrics,
    linger: Boolean,
    getTime: () -> Long
): CurrentLineState {
    val player: PlayerState = LocalPlayerState.current
    val romanise_furigana: Boolean by player.settings.lyrics.ROMANISE_FURIGANA.observe()

    val state: CurrentLineState = remember(romanise_furigana) {
        val tokeniser: LyricsFuriganaTokeniser = createFuriganaTokeniser(romanise_furigana)
        val lines: List<List<SongLyrics.Term>> = lyrics.lines.map { tokeniser.mergeAndFuriganiseTerms(it) }
        CurrentLineState(lines).apply { update(getTime(), linger) }
    }

    LaunchedEffect(linger) {
        while (true) {
            delay(UPDATE_INTERVAL_MS)
            state.update(getTime(), linger)
        }
    }

    return state
}
