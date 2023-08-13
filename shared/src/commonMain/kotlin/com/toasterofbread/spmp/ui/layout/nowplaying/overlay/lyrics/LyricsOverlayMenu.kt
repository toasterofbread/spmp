@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
package com.toasterofbread.spmp.ui.layout.nowplaying.overlay.lyrics

import LocalPlayerState
import SpMp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import androidx.compose.ui.zIndex
import com.toasterofbread.spmp.api.lyrics.LyricsSource
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.model.SongLyrics
import com.toasterofbread.spmp.model.mediaitem.Song
import com.toasterofbread.spmp.model.mediaitem.loader.SongLyricsLoader
import com.toasterofbread.spmp.platform.composable.BackHandler
import com.toasterofbread.spmp.platform.composable.PlatformAlertDialog
import com.toasterofbread.spmp.platform.composable.platformClickable
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.component.PillMenu
import com.toasterofbread.spmp.ui.layout.nowplaying.maintab.NOW_PLAYING_MAIN_PADDING
import com.toasterofbread.spmp.ui.layout.nowplaying.overlay.OverlayMenu
import com.toasterofbread.spmp.ui.theme.Theme
import com.toasterofbread.utils.AnnotatedReadingTerm
import com.toasterofbread.utils.ReadingTextData
import com.toasterofbread.utils.calculateReadingsAnnotatedString
import com.toasterofbread.utils.composable.SubtleLoadingIndicator
import com.toasterofbread.utils.launchSingle
import com.toasterofbread.utils.setAlpha
import com.toasterofbread.utils.thenIf
import kotlinx.coroutines.delay

enum class LyricsOverlaySubmenu {
    SEARCH, SYNC
}

class LyricsOverlayMenu: OverlayMenu() {

    override fun closeOnTap(): Boolean = false

    @Composable
    override fun Menu(
        getSong: () -> Song,
        getExpansion: () -> Float,
        openMenu: (OverlayMenu?) -> Unit,
        getSeekState: () -> Any,
        getCurrentSongThumb: () -> ImageBitmap?
    ) {
        val song = getSong()

        val pill_menu = remember { PillMenu(expand_state = mutableStateOf(false)) }
        val scroll_state = rememberLazyListState()
        val coroutine_scope = rememberCoroutineScope()

        val lyrics_state = remember(song.id) { SongLyricsLoader.getItemState(song, SpMp.context.database) }
        var show_furigana: Boolean by remember { mutableStateOf(Settings.KEY_LYRICS_DEFAULT_FURIGANA.get()) }

        var submenu: LyricsOverlaySubmenu? by remember { mutableStateOf(null) }
        var lyrics_sync_line_data: Pair<Int, List<AnnotatedReadingTerm>>? by remember { mutableStateOf(null) }
        var selecting_sync_line: Boolean by remember { mutableStateOf(false) }

        BackHandler(submenu != null || selecting_sync_line) {
            if (selecting_sync_line) {
                selecting_sync_line = false
            }
            else {
                submenu = null
            }
        }

        LaunchedEffect(lyrics_state) {
            SongLyricsLoader.loadBySong(song, SpMp.context)
        }

        LaunchedEffect(lyrics_state.loading) {
            if (!lyrics_state.loading && lyrics_state.lyrics == null && submenu == null) {
                submenu = LyricsOverlaySubmenu.SEARCH
            }
        }

        LaunchedEffect(lyrics_state.lyrics) {
            submenu = null
            lyrics_sync_line_data = null
            selecting_sync_line = false
        }

        Box(contentAlignment = Alignment.Center) {
            var show_lyrics_info by remember { mutableStateOf(false) }
            if (show_lyrics_info) {
                PlatformAlertDialog(
                    { show_lyrics_info = false },
                    {
                        Button({ show_lyrics_info = false }) {
                            Text(getString("action_close"))
                        }
                    }
                ) {
                    Crossfade(lyrics_state.lyrics) { lyrics ->
                        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(15.dp)) {
                            if (lyrics == null) {
                                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Close, null)
                                }
                            }
                            else {
                                @Composable
                                fun Item(title: String, text: String) {
                                    val faint_colour = LocalContentColor.current.setAlpha(0.75f)
                                    Column(Modifier.fillMaxWidth().border(1.dp, faint_colour, RoundedCornerShape(16.dp)).padding(10.dp)) {
                                        Text(title, style = MaterialTheme.typography.bodySmall, color = faint_colour)
                                        Spacer(Modifier.height(5.dp))

                                        SelectionContainer {
                                            Text(text, style = MaterialTheme.typography.titleMedium)
                                        }
                                    }
                                }

                                Item(getString("lyrics_info_key_source"), LyricsSource.fromIdx(lyrics.source_idx).getReadable())
                                Item(getString("lyrics_info_key_id"), lyrics.id)
                                Item(getString("lyrics_info_key_sync_type"), lyrics.sync_type.getReadable())
                            }
                        }
                    }
                }
            }

            // Pill menu
            AnimatedVisibility(submenu != LyricsOverlaySubmenu.SEARCH, Modifier.zIndex(10f), enter = fadeIn(), exit = fadeOut()) {
                pill_menu.PillMenu(
                    if (submenu != null || selecting_sync_line) 1 else if (lyrics_state.lyrics?.synced == true) 5 else 4,
                    { index, _ ->
                        when (index) {
                            0 -> ActionButton(Icons.Filled.Close) {
                                if (submenu != null) {
                                    openMenu(null)
                                }
                                else if (selecting_sync_line) {
                                    selecting_sync_line = false
                                }
                                else {
                                    submenu = null
                                }
                            }
                            1 -> ActionButton(Icons.Outlined.Info) { show_lyrics_info = !show_lyrics_info }
                            2 -> ActionButton(Icons.Filled.Search) { submenu = LyricsOverlaySubmenu.SEARCH }
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
                            4 -> ActionButton(Icons.Default.HourglassEmpty) { selecting_sync_line = true }
                        }
                    },
                    _background_colour = Theme.accent_provider,
                    vertical = true
                )
            }

            Crossfade(Triple(submenu, getSong(), lyrics_state.lyrics ?: lyrics_state.loading), Modifier.fillMaxSize()) { state ->
                val (current_submenu, song, lyrics) = state

                if (current_submenu == LyricsOverlaySubmenu.SEARCH) {
                    LyricsSearchMenu(song, Modifier.fillMaxSize()) { changed ->
                        submenu = null
                        if (changed) {
                            coroutine_scope.launchSingle {
                                val result = SongLyricsLoader.loadBySong(getSong(), SpMp.context)
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
                    if (lyrics is SongLyrics) {
                        lyrics_sync_line_data?.also { line_data ->
                            LyricsSyncMenu(
                                song, 
                                lyrics, 
                                line_data.first, 
                                line_data.second, 
                                Modifier.fillMaxSize()
                            ) {
                                submenu = null
                            }
                        }
                    }
                    else {
                        submenu = null
                    }
                }
                else if (lyrics is SongLyrics) {
                    Box(Modifier.fillMaxSize()) {
                        val lyrics_follow_enabled: Boolean by Settings.KEY_LYRICS_FOLLOW_ENABLED.rememberMutableState()

                        CoreLyricsDisplay(
                            lyrics,
                            song,
                            scroll_state,
                            getExpansion,
                            show_furigana,
                            Modifier.fillMaxSize(),
                            enable_autoscroll = lyrics_follow_enabled && !selecting_sync_line
                        ) {
                            if (selecting_sync_line) { line_data ->
                                submenu = LyricsOverlaySubmenu.SYNC
                                lyrics_sync_line_data = line_data
                                selecting_sync_line = false
                            }
                            else null
                        }

                        AnimatedVisibility(selecting_sync_line) {
                            Text(getString("lyrics_sync_select_line"))
                        }
                    }
                }
                else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        SubtleLoadingIndicator(message = getString("lyrics_loading"))
                    }
                }
            }
        }
    }
}

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

    val lyrics_sync_offset: Long = song.LyricsSyncOffset.observe(SpMp.context.database).value ?: 0

    val screen_width = SpMp.context.getScreenWidth()
    val size_px = with(LocalDensity.current) { ((screen_width - (NOW_PLAYING_MAIN_PADDING.dp * 2) - (15.dp * getExpansion() * 2)).value * 0.9.dp).toPx() }
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
                player.status.getPositionMillis() + lyrics_sync_offset
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

        val text_element: @Composable (is_reading: Boolean, text: String, font_size: TextUnit, index: Int, modifier: Modifier, getLine: () -> Pair<Int, List<AnnotatedReadingTerm>>) -> Unit =
            { is_reading: Boolean, text: String, font_size: TextUnit, index: Int, modifier: Modifier, getLine: () -> Pair<Int, List<AnnotatedReadingTerm>> ->
                val is_current by remember { derivedStateOf { !lyrics.synced || current_range?.contains(index) == true } }
                val colour by animateColorAsState(
                    LocalContentColor.current.let { colour ->
                        colour.setAlpha(
                            if (colour.alpha == 0f) 1f
                            else if (colour.alpha == 1f && is_current) 1f
                            else 0.65f
                        )
                    }
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
            data_with_readings = calculateReadingsAnnotatedString(terms, true, font_size, text_element) { 
                data_with_readings!!.getLineIndexOfTerm(it)
            }
            data_without_readings = calculateReadingsAnnotatedString(terms, false, font_size, text_element) { 
                data_without_readings!!.getLineIndexOfTerm(it)
            }
        }
        else {
            data_without_readings = calculateReadingsAnnotatedString(terms, false, font_size, text_element) { 
                data_without_readings!!.getLineIndexOfTerm(it)
            }
            data_with_readings = calculateReadingsAnnotatedString(terms, true, font_size, text_element) { 
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
        synchronized(lines) {
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
    }

fun List<AnnotatedReadingTerm>.getLineIndexOfTerm(term_index: Int): Int {
    var term_count = 0
    for (line in withIndex()) {
        val line_terms = line.value.annotated_string.annotations?.size ?: 0
        if (term_index < term_count + line_terms) {
            return line.index
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
