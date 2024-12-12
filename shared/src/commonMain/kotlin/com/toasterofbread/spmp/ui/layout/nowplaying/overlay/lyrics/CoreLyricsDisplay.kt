package com.toasterofbread.spmp.ui.layout.nowplaying.overlay.lyrics

import LocalPlayerState
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.*
import com.toasterofbread.spmp.model.lyrics.SongLyrics
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.component.HorizontalFuriganaText
import com.toasterofbread.spmp.ui.layout.nowplaying.NOW_PLAYING_MAIN_PADDING_DP
import com.toasterofbread.spmp.youtubeapi.lyrics.LyricsFuriganaTokeniser
import dev.toastbits.composekit.components.platform.composable.platformClickable
import dev.toastbits.composekit.util.thenIf
import dev.toastbits.composekit.util.thenWith
import dev.toastbits.composekit.components.utils.composable.workingAnimateScrollToItem
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
    onLineAltClick: ((Int) -> Unit)? = null
) {
    val player: PlayerState = LocalPlayerState.current
    val density: Density = LocalDensity.current
    val lyrics_sync_offset: Long? by song.getLyricsSyncOffset(player.database, false)

    val romanise_furigana: Boolean by player.settings.Lyrics.ROMANISE_FURIGANA.observe()
    val add_padding: Boolean by player.settings.Lyrics.EXTRA_PADDING.observe()

    var area_size: Dp by remember { mutableStateOf(0.dp) }
    val size_px: Float = with(density) { ((area_size - (NOW_PLAYING_MAIN_PADDING_DP.dp * 2) - (15.dp * getExpansion() * 2)).value * 0.9.dp).toPx() }
    val line_height: Float = with (density) { 20.sp.toPx() }
    val line_spacing: Float = with (density) { 25.dp.toPx() }

    val static_scroll_offset: Int = with(density) { 2.dp.toPx().toInt() }
    val padding_height: Int =
        if (add_padding) (size_px + line_height + line_spacing).toInt() + static_scroll_offset
        else line_height.toInt() + static_scroll_offset

    var current_range: IntRange? by remember { mutableStateOf(null) }
    var tokenised_lines: List<List<SongLyrics.Term>>? by remember { mutableStateOf(null) }

    suspend fun getScrollOffset(follow_offset: Float? = null): Int =
        (padding_height - static_scroll_offset - size_px * (follow_offset ?: player.settings.Lyrics.FOLLOW_OFFSET.get())).toInt()

    LaunchedEffect(lyrics, romanise_furigana) {
        val tokeniser: LyricsFuriganaTokeniser? = LyricsFuriganaTokeniser.getInstance()
        tokenised_lines =
            if (tokeniser != null) lyrics.lines.map { tokeniser.mergeAndFuriganiseTerms(it, romanise_furigana) }
            else lyrics.lines
    }

    LaunchedEffect(lyrics) {
        scroll_state.scrollToItem(0, getScrollOffset(0f))

        if (!lyrics.synced) {
            return@LaunchedEffect
        }

        while (true) {
            val (range, next) = getTermRangeOfTime(
                player.context,
                lyrics,
                player.status.getPositionMs() + (lyrics_sync_offset ?: 0)
            )

            if (range != null) {
                current_range = range
            }

            delay(minOf(next, 100))
        }
    }

    val font_size_percent: Float by player.settings.Lyrics.FONT_SIZE.observe()
    val font_size: TextUnit = (10 + (font_size_percent * 20)).sp
    val text_style: TextStyle = getLyricsTextStyle(font_size)

    Crossfade(show_furigana, modifier) { readings ->
        var first_scroll by remember { mutableStateOf(true) }
        LaunchedEffect(current_range) {
            if (!enable_autoscroll) {
                return@LaunchedEffect
            }

            val range_start_line: Int = current_range?.first ?: return@LaunchedEffect

            if (first_scroll) {
                first_scroll = false
                scroll_state.scrollToItem(
                    range_start_line,
                    getScrollOffset()
                )
            }
            else {
                scroll_state.workingAnimateScrollToItem(range_start_line, getScrollOffset(), density)
            }
        }

        val text_alignment: Int by player.settings.Lyrics.TEXT_ALIGNMENT.observe()

        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .onSizeChanged {
                    area_size = with(density) { it.height.toDp() }
                },
            state = scroll_state,
            horizontalAlignment =
                when (text_alignment) {
                    0 -> if (LocalLayoutDirection.current == LayoutDirection.Ltr) Alignment.Start else Alignment.End
                    1 -> Alignment.CenterHorizontally
                    else -> if (LocalLayoutDirection.current == LayoutDirection.Ltr) Alignment.End else Alignment.Start
                },
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = with(LocalDensity.current) { padding_height.toDp() }.let { padding ->
                PaddingValues(
                    top = padding,
                    bottom = padding + if (add_padding) 5.dp else 0.dp
                )
            }
        ) {
            itemsIndexed(tokenised_lines.orEmpty()) { index, line ->
                val current: Boolean = current_range?.contains(index) ?: false

                HorizontalFuriganaText(
                    line,
                    Modifier
                        .thenIf(!current) {
                            alpha(0.65f)
                        }
                        .thenWith(onLineAltClick) { onAltClick ->
                            platformClickable(
                                onAltClick = {
                                    onAltClick(index)
                                }
                            )
                        },
                    show_readings = readings,
                    style = text_style
                )
            }
        }
    }
}
