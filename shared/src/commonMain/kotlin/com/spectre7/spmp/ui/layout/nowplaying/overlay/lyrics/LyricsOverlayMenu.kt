@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
package com.spectre7.spmp.ui.layout.nowplaying.overlay.lyrics

import LocalPlayerState
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.zIndex
import com.spectre7.spmp.model.Settings
import com.spectre7.spmp.model.mediaitem.Song
import com.spectre7.spmp.model.SongLyrics
import com.spectre7.spmp.model.mediaitem.SongLyricsHolder
import com.spectre7.spmp.platform.composable.BackHandler
import com.spectre7.spmp.platform.composable.platformClickable
import com.spectre7.spmp.resources.getString
import com.spectre7.spmp.resources.getStringTODO
import com.spectre7.spmp.ui.component.PillMenu
import com.spectre7.spmp.ui.layout.nowplaying.LocalNowPlayingExpansion
import com.spectre7.spmp.ui.layout.nowplaying.overlay.OverlayMenu
import com.spectre7.spmp.ui.theme.Theme
import com.spectre7.utils.*
import com.spectre7.utils.composable.SubtleLoadingIndicator
import kotlinx.coroutines.delay

enum class LyricsOverlaySubmenu {
    SEARCH, SYNC
}

class LyricsOverlayMenu(
    val size: Dp
): OverlayMenu() {

    override fun closeOnTap(): Boolean = false

    @Composable
    override fun Menu(
        songProvider: () -> Song,
        expansion: Float,
        openShutterMenu: (@Composable () -> Unit) -> Unit,
        close: () -> Unit,
        getSeekState: () -> Any,
        getCurrentSongThumb: () -> ImageBitmap?
    ) {
        val pill_menu = remember { PillMenu(expand_state = mutableStateOf(false)) }
        val scroll_state = rememberLazyListState()
        val coroutine_scope = rememberCoroutineScope()

        val lyrics_holder: SongLyricsHolder = songProvider().lyrics
        var show_furigana: Boolean by remember { mutableStateOf(Settings.KEY_LYRICS_DEFAULT_FURIGANA.get()) }

        var submenu: LyricsOverlaySubmenu? by remember { mutableStateOf(null) }
        var lyrics_sync_line: AnnotatedReadingTerm? by remember { mutableStateOf(null) }
        var selecting_sync_line: Boolean by remember { mutableStateOf(false) }

        BackHandler(submenu != null || selecting_sync_line) {
            if (selecting_sync_line) {
                selecting_sync_line = false
            }
            else {
                submenu = null
            }
        }

        LaunchedEffect(lyrics_holder.loading) {
            if (!lyrics_holder.loading && lyrics_holder.loaded && lyrics_holder.lyrics == null && submenu == null) {
                submenu = LyricsOverlaySubmenu.SEARCH
            }
        }

        LaunchedEffect(lyrics_holder.lyrics) {
            submenu = null
            lyrics_sync_line = null
            selecting_sync_line = false
        }

        Box(contentAlignment = Alignment.Center) {
            // Pill menu
            AnimatedVisibility(submenu != LyricsOverlaySubmenu.SEARCH, Modifier.zIndex(10f), enter = fadeIn(), exit = fadeOut()) {
                pill_menu.PillMenu(
                    if (submenu != null || selecting_sync_line) 1 else if (lyrics_holder.lyrics?.synced == true) 4 else 3,
                    { index, _ ->
                        when (index) {
                            0 -> ActionButton(Icons.Filled.Close) {
                                if (submenu != null) {
                                    close()
                                }
                                else if (selecting_sync_line) {
                                    selecting_sync_line = false
                                }
                                else {
                                    submenu = null
                                }
                            }
                            1 -> ActionButton(Icons.Filled.Search) { submenu = LyricsOverlaySubmenu.SEARCH }
                            2 -> Box(
                                Modifier.size(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("ふ", color = content_colour, fontSize = 20.sp, modifier = Modifier
                                    .offset(y = (-5).dp)
                                    .clickable(
                                        remember { MutableInteractionSource() },
                                        rememberRipple(bounded = false, radius = 20.dp),
                                        onClick = {
                                            show_furigana = !show_furigana
                                            is_open = !is_open
                                        })
                                )
                            }
                            3 -> ActionButton(Icons.Default.HourglassEmpty) { selecting_sync_line = true }
                        }
                    },
                    _background_colour = Theme.current.accent_provider,
                    vertical = true
                )
            }

            Crossfade(Triple(submenu, songProvider(), lyrics_holder.lyrics), Modifier.fillMaxSize()) { state ->
                val (current_submenu, song, lyrics) = state

                if (current_submenu == LyricsOverlaySubmenu.SEARCH) {
                    LyricsSearchMenu(songProvider(), Modifier.fillMaxSize()) { changed ->
                        submenu = null
                        if (changed) {
                            coroutine_scope.launchSingle {
                                val result = songProvider().lyrics.loadAndGet()
                                result.fold(
                                    {},
                                    { error ->
                                        // TODO
                                        SpMp.context.sendToast(error.toString())
                                    }
                                )
                            }
                        }
                    }
                }
                else if (current_submenu == LyricsOverlaySubmenu.SYNC) {
                    if (lyrics != null) {
                        lyrics_sync_line?.also { line ->
                            LyricsSyncMenu(song, lyrics, line, Modifier.fillMaxSize()) {
                                submenu = null
                            }
                        }
                    }
                    else {
                        submenu = null
                    }
                }
                else if (lyrics != null) {
                    Box(Modifier.fillMaxSize()) {
                        val lyrics_follow_enabled: Boolean by Settings.KEY_LYRICS_FOLLOW_ENABLED.rememberMutableState()

                        CoreLyricsDisplay(
                            size,
                            lyrics,
                            song,
                            scroll_state,
                            show_furigana,
                            Modifier.fillMaxSize(),
                            enable_autoscroll = lyrics_follow_enabled && !selecting_sync_line
                        ) {
                            if (selecting_sync_line) { line ->
                                submenu = LyricsOverlaySubmenu.SYNC
                                lyrics_sync_line = line
                                selecting_sync_line = false
                            }
                            else null
                        }

                        AnimatedVisibility(selecting_sync_line) {
                            Text(getString("lyrics_select_sync_line"))
                        }
                    }
                }
                else {
                    Column(Modifier.size(size), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text(getString("lyrics_loading"), fontWeight = FontWeight.Light)
                        Spacer(Modifier.height(20.dp))
                        LinearProgressIndicator(Modifier.fillMaxWidth(0.5f), color = Theme.current.accent, trackColor = Theme.current.on_accent)
                    }
                }
            }
        }
    }
}

@Composable
fun CoreLyricsDisplay(
    size: Dp,
    lyrics: SongLyrics,
    song: Song,
    scroll_state: LazyListState,
    show_furigana: Boolean,
    modifier: Modifier = Modifier,
    enable_autoscroll: Boolean = true,
    getOnLongClick: () -> ((line: AnnotatedReadingTerm) -> Unit)?
) {
    val player = LocalPlayerState.current

    val size_px = with(LocalDensity.current) { size.toPx() }
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
                player.status.getPositionMillis() + song.song_reg_entry.getLyricsSyncOffset()
            )

            if (range != null) {
                current_range = range
            }

            delay(minOf(next, 100))
        }
    }

    val font_size_percent: Float by Settings.KEY_LYRICS_FONT_SIZE.rememberMutableState()
    val font_size = (10 + (font_size_percent * 20)).sp

    var data_with_readings: List<AnnotatedReadingTerm>? by remember { mutableStateOf(null) }
    var data_without_readings: List<AnnotatedReadingTerm>? by remember { mutableStateOf(null) }

    LaunchedEffect(terms) {
        data_with_readings = null
        data_without_readings = null

        val text_element: @Composable (is_reading: Boolean, text: String, font_size: TextUnit, index: Int, modifier: Modifier, getLine: () -> AnnotatedReadingTerm) -> Unit =
            { is_reading: Boolean, text: String, font_size: TextUnit, index: Int, modifier: Modifier, getLine: () -> AnnotatedReadingTerm ->
                val is_current by remember { derivedStateOf { !lyrics.synced || current_range?.contains(index) == true } }
                val colour by animateColorAsState(
                    if (is_current) Color.White
                    else Color.White.setAlpha(0.5f)
                )

                val onLongClick = getOnLongClick()

                Text(
                    text,
                    modifier.thenIf(
                        onLongClick != null,
                        Modifier.platformClickable(
                            onAltClick = { onLongClick?.invoke(getLine()) }
                        )
                    ),
                    fontSize = font_size,
                    color = colour
                )
            }

        if (show_furigana) {
            data_with_readings = calculateReadingsAnnotatedString(terms, true, font_size, text_element) { data_with_readings!!.getLineOfTerm(it) }
            data_without_readings = calculateReadingsAnnotatedString(terms, false, font_size, text_element) { data_without_readings!!.getLineOfTerm(it) }
        }
        else {
            data_without_readings = calculateReadingsAnnotatedString(terms, false, font_size, text_element) { data_without_readings!!.getLineOfTerm(it) }
            data_with_readings = calculateReadingsAnnotatedString(terms, true, font_size, text_element) { data_with_readings!!.getLineOfTerm(it) }
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
            LaunchedEffect(current_range) {
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
                    .padding(horizontal = 20.dp),
                state = scroll_state,
                horizontalAlignment = when (Settings.get<Int>(Settings.KEY_LYRICS_TEXT_ALIGNMENT)) {
                    0 -> if (LocalLayoutDirection.current == LayoutDirection.Ltr) Alignment.Start else Alignment.End
                    1 -> Alignment.CenterHorizontally
                    else -> if (LocalLayoutDirection.current == LayoutDirection.Ltr) Alignment.End else Alignment.Start
                },
                verticalArrangement = Arrangement.spacedBy(5.dp),
                contentPadding = with(LocalDensity.current) { padding_height.toDp() }.let { padding ->
                    PaddingValues(
                        top = padding,
                        bottom = padding + if (add_padding) 5.dp else 0.dp
                    )
                }
            ) {
                itemsIndexed(text_data) { line, item ->
                    Text(
                        item.annotated_string,
                        inlineContent = item.inline_content,
                        style = getLyricsTextStyle(font_size)
                    )
                }
            }
        }
    }
}

fun List<ReadingTextData>.getTermRangeOfTime(lyrics: SongLyrics, time: Long): Pair<IntRange?, Long> {
    require(lyrics.synced)
    
    var start = -1
    var end = -1
    var next = Long.MAX_VALUE
    var last_before: Int? = null

    for (item in withIndex()) {
        val term = item.value.data as SongLyrics.Term

        val range =
            if (lyrics.sync_type == SongLyrics.SyncType.WORD_SYNC && !Settings.get<Boolean>(Settings.KEY_LYRICS_ENABLE_WORD_SYNC)) {
                term.line_range ?: term.range
            }
            else {
                term.range
            }

        if (range.contains(time)) {
            if (start == -1) {
                start = item.index
            }
            end = item.index
        }
        else if (start != -1) {
            if (term.start!! > time) {
                next = term.start - time
            }
            break
        }
        else if (time > range.last) {
            last_before = item.index
        }
    }

    if (start != -1) {
        return Pair(start .. end, next)
    }
    else if (last_before != null) {
        for (i in last_before - 1 downTo 0) {
            if (get(i).text.contains('\n')) {
                return Pair(i + 1 .. last_before, next)
            }
        }
    }

    return Pair(null, next)
}

fun SongLyrics.getReadingTerms(): MutableList<ReadingTextData> =
    mutableListOf<ReadingTextData>().apply {
        for (line in lines) {
            for (term in line.withIndex()) {
                for (subterm in term.value.subterms.withIndex()) {
                    if (subterm.index + 1 == term.value.subterms.size && term.index + 1 == line.size) {
                        add(ReadingTextData(subterm.value.text + "\n", subterm.value.furi, term.value))
                    }
                    else {
                        add(ReadingTextData(subterm.value.text, subterm.value.furi, term.value))
                    }
                }
            }
        }
    }

fun List<AnnotatedReadingTerm>.getLineOfTerm(term_index: Int): AnnotatedReadingTerm {
    var term_count = 0
    for (line in withIndex()) {
        val line_terms = line.value.annotated_string.annotations?.size ?: 0
        if (term_index < term_count + line_terms) {
            return line.value
        }
        term_count += line_terms
    }
    throw IndexOutOfBoundsException(term_index)
}

@Composable
fun getLyricsTextStyle(font_size: TextUnit): TextStyle =
    LocalTextStyle.current.copy(
        fontSize = font_size,
        lineHeight = ((font_size.value * 1.5) + 10).sp
    )
