package com.toasterofbread.spmp.ui.component

import LocalPlayerState
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.toasterofbread.spmp.model.lyrics.SongLyrics
import com.toasterofbread.spmp.ui.util.LyricsLineState
import dev.toastbits.composekit.util.platform.Platform
import dev.toastbits.composekit.util.composable.AlignableCrossfade
import dev.toastbits.composekit.components.utils.composable.animatedvisibility.NullableValueAnimatedVisibility

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

    val show_furigana_option: Boolean by LocalPlayerState.current.settings.Lyrics.DEFAULT_FURIGANA.observe()
    val current_line_state: LyricsLineState? = LyricsLineState.rememberCurrentLineState(lyrics, lyrics_linger, getTime = getTime)

    val lyrics_text_style: TextStyle =
        LocalTextStyle.current.copy(
            fontSize =
                when (Platform.current) {
                    Platform.ANDROID -> 16.sp
                    Platform.DESKTOP,
                    Platform.WEB -> 20.sp
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
        current_line_state?.LyricsDisplay { show, line ->
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
            if (current_line_state?.isLineShowing() == true) null else emptyContent,
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
            fontSize =
                when (Platform.current) {
                    Platform.ANDROID -> 16.sp
                    Platform.DESKTOP,
                    Platform.WEB -> 20.sp
                },
            color = text_colour
        )

    val current_line_state: LyricsLineState? = LyricsLineState.rememberCurrentLineState(lyrics, lyrics_linger, getTime = getTime)

    Box(
        modifier,
        contentAlignment = Alignment.Center
    ) {
        current_line_state?.LyricsDisplay { show, line ->
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
