@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
package com.toasterofbread.spmp.ui.layout.nowplaying.overlay.lyrics

import LocalPlayerState
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.model.lyrics.SongLyrics
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.composekit.platform.composable.platformClickable
import com.toasterofbread.spmp.ui.layout.nowplaying.NOW_PLAYING_MAIN_PADDING_DP
import com.toasterofbread.spmp.ui.component.AnnotatedReadingTerm
import com.toasterofbread.spmp.ui.component.calculateReadingsAnnotatedString
import com.toasterofbread.composekit.utils.common.thenIf
import com.toasterofbread.composekit.utils.composable.SubtleLoadingIndicator
import kotlinx.coroutines.delay

@Composable
fun CoreLyricsDisplay(
    lyrics: SongLyrics,
    song: Song,
    scroll_state: LazyListState,
    getExpansion: () -> Float,
    show_furigana: Boolean,
    modifier: Modifier = Modifier,
    enable_autoscroll: Boolean = true,
    getOnLongClick: () -> ((line_data: Pair<Int, List<AnnotatedReadingTerm>>) -> Unit)?
) {
    val player = LocalPlayerState.current
    val density = LocalDensity.current
    val lyrics_sync_offset: Long? by song.getLyricsSyncOffset(player.database, false)

    var area_size: Dp by remember { mutableStateOf(0.dp) }
    val size_px = with(LocalDensity.current) { ((area_size - (NOW_PLAYING_MAIN_PADDING_DP.dp * 2) - (15.dp * getExpansion() * 2)).value * 0.9.dp).toPx() }
    val line_height = with (LocalDensity.current) { 20.sp.toPx() }
    val line_spacing = with (LocalDensity.current) { 25.dp.toPx() }

    val add_padding: Boolean = Settings.get(Settings.KEY_LYRICS_EXTRA_PADDING)
    val static_scroll_offset = with(LocalDensity.current) { 2.dp.toPx().toInt() }
    val padding_height =
        if (add_padding) (size_px + line_height + line_spacing).toInt() + static_scroll_offset
        else line_height.toInt() + static_scroll_offset

    val terms = remember(lyrics) { lyrics.getReadingTerms() }
    var current_range: IntRange? by remember { mutableStateOf(null) }

    fun getScrollOffset(follow_offset: Float = Settings.KEY_LYRICS_FOLLOW_OFFSET.get()): Int =
        (padding_height - static_scroll_offset - size_px * follow_offset).toInt()

    LaunchedEffect(lyrics) {
        scroll_state.scrollToItem(0, getScrollOffset(0f))

        if (!lyrics.synced) {
            return@LaunchedEffect
        }

        while (true) {
            val (range, next) = terms.getTermRangeOfTime(
                lyrics,
                player.status.getPositionMillis() + (lyrics_sync_offset ?: 0)
            )

            if (range != null) {
                current_range = range
            }

            delay(minOf(next, 100))
        }
    }

    val font_size_percent: Float by Settings.KEY_LYRICS_FONT_SIZE.rememberMutableState()
    val text_style: TextStyle = getLyricsTextStyle((10 + (font_size_percent * 20)).sp)

    var data_with_readings: List<AnnotatedReadingTerm>? by remember { mutableStateOf(null) }
    var data_without_readings: List<AnnotatedReadingTerm>? by remember { mutableStateOf(null) }
    val line_counts: MutableMap<Int, Int> = remember { mutableStateMapOf() }

    LaunchedEffect(terms) {
        data_with_readings = null
        data_without_readings = null

        val text_element = @Composable { is_reading: Boolean, b: String, c: TextStyle, index: Int, modifier: Modifier, f: () -> Pair<Int, List<AnnotatedReadingTerm>> ->
            LyricsTextElement(
                lyrics,
                current_range,
                getOnLongClick(),
                is_reading,
                b,
                c,
                index,
                modifier.thenIf(!is_reading) {
                    width(area_size)
                },
                f,
                onTextLayout = { result ->
                    line_counts[index] = result.lineCount
                }
            )
        }

        if (show_furigana) {
            data_with_readings = calculateReadingsAnnotatedString(terms, true, text_style, text_element) {
                data_with_readings!!.getLineIndexOfTerm(it)
            }
            data_without_readings = calculateReadingsAnnotatedString(terms, false, text_style, text_element) {
                data_without_readings!!.getLineIndexOfTerm(it)
            }
        }
        else {
            data_without_readings = calculateReadingsAnnotatedString(terms, false, text_style, text_element) {
                data_without_readings!!.getLineIndexOfTerm(it)
            }
            data_with_readings = calculateReadingsAnnotatedString(terms, true, text_style, text_element) {
                data_with_readings!!.getLineIndexOfTerm(it)
            }
        }
    }

    Crossfade(if (show_furigana) data_with_readings else data_without_readings, modifier) { text_data ->
        if (text_data == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                SubtleLoadingIndicator()
            }
        }
        else {
            var first_scroll by remember { mutableStateOf(true) }
            LaunchedEffect(current_range, size_px) {
                if (!enable_autoscroll) {
                    return@LaunchedEffect
                }

                val range_start = current_range?.first ?: return@LaunchedEffect

                var term_count = 0
                for (line in text_data.withIndex()) {
                    term_count += line.value.annotated_string.annotations?.size ?: 0

                    if (term_count > range_start) {
                        if (first_scroll) {
                            first_scroll = false
                            scroll_state.scrollToItem(
                                line.index,
                                getScrollOffset()
                            )
                        } 
                        else {
                            scroll_state.animateScrollToItem(
                                line.index,
                                getScrollOffset()
                            )
                        }
                        break
                    }
                }
            }

            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .onSizeChanged {
                        area_size = with(density) { it.height.toDp() }
                    },
                state = scroll_state,
                horizontalAlignment = when (Settings.get<Int>(Settings.KEY_LYRICS_TEXT_ALIGNMENT)) {
                    0 -> if (LocalLayoutDirection.current == LayoutDirection.Ltr) Alignment.Start else Alignment.End
                    1 -> Alignment.CenterHorizontally
                    else -> if (LocalLayoutDirection.current == LayoutDirection.Ltr) Alignment.End else Alignment.Start
                },
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = with(LocalDensity.current) { padding_height.toDp() }.let { padding ->
                    PaddingValues(
                        top = padding,
                        bottom = padding + if (add_padding) 5.dp else 0.dp
                    )
                }
            ) {
                itemsIndexed(text_data) { index, item ->
                    val line_count: Int = line_counts[index] ?: 1
                    val text_line_height: Dp = with (LocalDensity.current) { text_style.lineHeight.toDp() }
                    val vertical_padding: Dp = text_line_height * (line_count - 1) / 2

                    Text(
                        item.annotated_string,
                        inlineContent = item.inline_content,
                        style = text_style,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                top = vertical_padding,
                                bottom = vertical_padding + (if (line_count > 1) text_line_height * 0.25f else 0.dp)
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun LyricsTextElement(
    lyrics: SongLyrics,
    current_range: IntRange?,
    onLongClick: ((line_data: Pair<Int, List<AnnotatedReadingTerm>>) -> Unit)?,
    is_reading: Boolean,
    text: String,
    text_style: TextStyle,
    index: Int,
    modifier: Modifier,
    getLine: () -> Pair<Int, List<AnnotatedReadingTerm>>,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    val is_current by remember(index, current_range, lyrics.synced) {
        derivedStateOf { !lyrics.synced || current_range?.contains(index) == true }
    }
    val colour by animateColorAsState(
        LocalContentColor.current.let { colour ->
            colour.copy(alpha = 
                if (colour.alpha == 0f) 1f
                else if (colour.alpha == 1f && is_current) 1f
                else 0.65f
            )
        }
    )

    Text(
        text,
        modifier
            .thenIf(onLongClick != null) {
                platformClickable(
                    onAltClick = { onLongClick?.invoke(getLine()) }
                )
            },
        style = text_style,
        color = colour,
        overflow = TextOverflow.Visible,
        softWrap = true,
        onTextLayout = onTextLayout
    )
}
