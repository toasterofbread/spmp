@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
package com.spectre7.spmp.ui.layout.nowplaying.overlay.lyrics

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.zIndex
import com.spectre7.spmp.PlayerServiceHost
import com.spectre7.spmp.model.Settings
import com.spectre7.spmp.model.mediaitem.Song
import com.spectre7.spmp.model.SongLyrics
import com.spectre7.spmp.model.mediaitem.SongLyricsHolder
import com.spectre7.spmp.platform.composable.platformClickable
import com.spectre7.spmp.resources.getStringTODO
import com.spectre7.spmp.ui.component.PillMenu
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
        var lyrics_sync_line: Int? by remember { mutableStateOf(null) }

        LaunchedEffect(lyrics_holder.loading) {
            if (!lyrics_holder.loading && lyrics_holder.loaded && lyrics_holder.lyrics == null && submenu == null) {
                submenu = LyricsOverlaySubmenu.SEARCH
            }
        }

        Box(contentAlignment = Alignment.Center) {
            // Pill menu
            AnimatedVisibility(submenu != LyricsOverlaySubmenu.SEARCH, Modifier.zIndex(10f), enter = fadeIn(), exit = fadeOut()) {
                pill_menu.PillMenu(
                    if (submenu == null) 4 else 1,
                    { index, _ ->
                        when (index) {
                            0 -> ActionButton(Icons.Filled.Close, close)
                            1 -> ActionButton(Icons.Filled.Search) { submenu = LyricsOverlaySubmenu.SEARCH }
                            2 -> ActionButton(Icons.Default.HourglassEmpty) { submenu = LyricsOverlaySubmenu.SYNC }
                            3 -> Box(
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
                        }
                    },
                    _background_colour = Theme.current.accent_provider,
                    vertical = true
                )
            }

            Crossfade(Triple(submenu, songProvider(), lyrics_holder.lyrics), Modifier.fillMaxSize()) {
                val (current_submenu, song, lyrics) = it

                if (current_submenu == LyricsOverlaySubmenu.SEARCH) {
                    LyricsSearchMenu(songProvider(), Modifier.fillMaxSize()) { changed ->
                        submenu = null
                        if (changed) {
                            coroutine_scope.launchSingle {
                                songProvider().lyrics.loadAndGet()
                            }
                        }
                    }
                }
                else if (current_submenu == LyricsOverlaySubmenu.SYNC && lyrics_sync_line != null) {
                    if (lyrics != null) {
                        LyricsSyncMenu(song, lyrics, lyrics_sync_line!!, Modifier.fillMaxSize())
                    }
                    else {
                        submenu = null
                    }
                }
                else if (lyrics != null) {
                    CoreLyricsDisplay(
                        size,
                        lyrics,
                        song,
                        scroll_state,
                        show_furigana,
                        Modifier.fillMaxSize(),
                        if (current_submenu == LyricsOverlaySubmenu.SYNC) { term ->
                            lyrics_sync_line = TODO()
                            submenu = LyricsOverlaySubmenu.SYNC
                        }
                        else null
                    )
                }
                else {
                    Column(Modifier.size(size), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text(getStringTODO("Loading lyrics"), fontWeight = FontWeight.Light)
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
    onLongClick: ((term_index: Int) -> Unit)?
) {
    val size_px = with(LocalDensity.current) { size.toPx() }
    val line_height = with (LocalDensity.current) { 20.sp.toPx() }
    val line_spacing = with (LocalDensity.current) { 25.dp.toPx() }

    val add_padding: Boolean = Settings.get(Settings.KEY_LYRICS_EXTRA_PADDING)
    val padding_height = with (LocalDensity.current) {
        if (add_padding) {
            (size_px - line_height - line_spacing).toDp()
        }
        else {
            line_height.toDp()
        }
    }

    LaunchedEffect(Unit) {
        scroll_state.scrollBy(if (add_padding) (size_px - line_height - (line_spacing * 2)) else (line_height - line_spacing))
    }

    val terms = remember { mutableListOf<ReadingTextData>().apply {
        for (line in lyrics.lines) {
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
    } }

    var current_range: IntRange? by remember { mutableStateOf(null) }

    LaunchedEffect(lyrics) {
        if (lyrics.sync_type == SongLyrics.SyncType.NONE) {
            return@LaunchedEffect
        }

        while (true) {
            val time = PlayerServiceHost.status.position_ms + song.song_reg_entry.getLyricsSyncOffset()
            var start = -1
            var end = -1
            var next = Long.MAX_VALUE
            var last_before: Int? = null

            for (item in terms.withIndex()) {
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
                current_range = start..end
            }
            else if (last_before != null) {
                for (i in last_before - 1 downTo 0) {
                    if (terms[i].text.contains('\n')) {
                        current_range = i + 1 .. last_before
                        break
                    }
                }
            }

            delay(minOf(next, 100))
        }
    }

    val font_size = 20.sp
    val reading_font_size = font_size / 2

    var data_with_readings: List<Pair<AnnotatedString, Map<String, InlineTextContent>>>? by remember { mutableStateOf(null) }
    var data_without_readings: List<Pair<AnnotatedString, Map<String, InlineTextContent>>>? by remember { mutableStateOf(null) }

    LaunchedEffect(terms) {
        data_with_readings = null
        data_without_readings = null

        val text_element: @Composable (is_reading: Boolean, text: String, font_size: TextUnit, index: Int, modifier: Modifier) -> Unit =
            { is_reading: Boolean, text: String, font_size: TextUnit, index: Int, modifier: Modifier ->
                val is_current by remember { derivedStateOf { lyrics.sync_type == SongLyrics.SyncType.NONE || current_range?.contains(index) == true } }
                val colour by animateColorAsState(
                    if (is_current) Color.White
                    else Color.White.setAlpha(0.5f)
                )

                Text(
                    text,
                    modifier.thenIf(
                        onLongClick != null,
                        Modifier.platformClickable(
                            onAltClick = { onLongClick?.invoke(index) }
                        )
                    ),
                    fontSize = font_size,
                    color = colour
                )
            }

        if (show_furigana) {
            data_with_readings = calculateReadingsAnnotatedString(terms, true, font_size, text_element)
            data_without_readings = calculateReadingsAnnotatedString(terms, false, font_size, text_element)
        }
        else {
            data_without_readings = calculateReadingsAnnotatedString(terms, false, font_size, text_element)
            data_with_readings = calculateReadingsAnnotatedString(terms, true, font_size, text_element)
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
                val range_start = current_range?.first ?: return@LaunchedEffect

                var term_count = 0
                for (line in text_data.withIndex()) {
                    term_count += line.value.first.annotations?.size ?: 0

                    if (term_count > range_start) {
                        val offset: Float = line_height - (size_px * Settings.KEY_LYRICS_FOLLOW_OFFSET.get<Float>())

                        if (first_scroll) {
                            first_scroll = false
                            delay(25)
                            scroll_state.scrollToItem(
                                line.index,
                                offset.toInt()
                            )
                        }
                        else {
                            scroll_state.animateScrollToItem(
                                line.index,
                                offset.toInt()
                            )
                        }
                        break
                    }
                }
            }

            val text_line_height = font_size.value + reading_font_size.value + 10

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
                contentPadding = PaddingValues(
                    top = padding_height,
                    bottom = padding_height - if (add_padding) with (LocalDensity.current) { (line_spacing * 2).toDp() } else 0.dp
                )
            ) {
                itemsIndexed(text_data) { line, item ->
                    Text(
                        item.first,
                        inlineContent = item.second,
                        style = LocalTextStyle.current.copy(
                            fontSize = font_size,
                            lineHeight = text_line_height.sp
                        )
                    )
                }
            }
        }
    }
}
