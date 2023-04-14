package com.spectre7.spmp.ui.layout.nowplaying.overlay.lyrics

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.zIndex
import com.spectre7.spmp.PlayerServiceHost
import com.spectre7.spmp.model.Settings
import com.spectre7.spmp.model.Song
import com.spectre7.spmp.ui.component.PillMenu
import com.spectre7.spmp.ui.layout.PlayerViewContext
import com.spectre7.spmp.ui.layout.nowplaying.overlay.OverlayMenu
import com.spectre7.spmp.ui.theme.Theme
import com.spectre7.utils.*
import kotlinx.coroutines.delay

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
        seek_state: Any,
        playerProvider: () -> PlayerViewContext
    ) {

        var lyrics: Song.Lyrics? by remember { mutableStateOf(null) }
        var show_furigana: Boolean by remember { mutableStateOf(Settings.prefs.getBoolean(Settings.KEY_LYRICS_DEFAULT_FURIGANA.name, true)) }

        val scroll_state = rememberLazyListState()
        var search_menu_open by remember { mutableStateOf(false) }
        var reload_lyrics by remember { mutableStateOf(false) }

        LaunchedEffect(songProvider().id, reload_lyrics) {
            lyrics = null
            songProvider().getLyrics {
                if (it == null) {
                    SpMp.context.sendToast(getString("no_lyrics_found"))
                    search_menu_open = true
                }
                else {
                    lyrics = it
                }
            }
        }

        Text(lyrics?.id.toString() + " | " + lyrics?.sync_type.toString())

        AnimatedVisibility(lyrics != null && !search_menu_open, Modifier.zIndex(10f), enter = fadeIn(), exit = fadeOut()) {
            remember { PillMenu(expand_state = mutableStateOf(false)) }.PillMenu(
                3,
                { index, _ ->
                    when (index) {
                        0 -> ActionButton(Icons.Filled.Close, close)
                        1 -> ActionButton(Icons.Filled.Search) { search_menu_open = true }
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
                    }
                },
                _background_colour = Theme.current.accent_provider,
                vertical = true
            )
        }

        Crossfade(search_menu_open) { search ->
            if (search) {
                LyricsSearchMenu(songProvider()) { changed ->
                    search_menu_open = false
                    if (changed) {
                        lyrics = null
                        reload_lyrics = !reload_lyrics
                    }
                }
            }
            else {
                ScrollingLyricsDisplay(playerProvider, size, seek_state, lyrics, scroll_state, show_furigana)
            }
        }
    }
}

@Composable
fun ScrollingLyricsDisplay(playerProvider: () -> PlayerViewContext, size: Dp, seek_state: Any, lyrics: Song.Lyrics?, scroll_state: LazyListState, show_furigana: Boolean) {
    Box {
        Crossfade(targetState = lyrics) {
            if (it == null) {
                Column(Modifier.size(size), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(getStringTemp("Loading lyrics"), fontWeight = FontWeight.Light)
                    Spacer(Modifier.height(20.dp))
                    LinearProgressIndicator(Modifier.fillMaxWidth(0.5f), color = Theme.current.accent, trackColor = Theme.current.on_accent)
                }
            }
            else {
                CoreLyricsDisplay(playerProvider, size, seek_state, it, scroll_state, show_furigana)
            }
        }
    }
}

@Composable
fun CoreLyricsDisplay(playerProvider: () -> PlayerViewContext, size: Dp, seek_state: Any, lyrics: Song.Lyrics, scroll_state: LazyListState, show_furigana: Boolean) {
    val size_px = with(LocalDensity.current) { size.toPx() }

    val line_height = with (LocalDensity.current) { 20.sp.toPx() }
    val line_spacing = with (LocalDensity.current) { 25.dp.toPx() }

//    LyricsTimingOverlay(playerProvider, lyrics, false, seek_state, scroll_state) { position ->
//        if (!Settings.get<Boolean>(Settings.KEY_LYRICS_FOLLOW_ENABLED)) {
//            return@LyricsTimingOverlay
//        }
//
//        val offset = size_px * Settings.prefs.getFloat(Settings.KEY_LYRICS_FOLLOW_OFFSET.name, 0.5f)
//        scroll_state.animateScrollToItem(0, (position - offset).toInt())
//    }

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

    val terms = remember { mutableListOf<TextData>().apply {
        for (line in lyrics.lines) {
            for (term in line.withIndex()) {
                for (subterm in term.value.subterms.withIndex()) {
                    if (subterm.index + 1 == term.value.subterms.size && term.index + 1 == line.size) {
                        add(TextData(subterm.value.text + "\n", subterm.value.furi, term.value))
                    }
                    else {
                        add(TextData(subterm.value.text, subterm.value.furi, term.value))
                    }
                }
            }
        }
    } }

    var current_range: IntRange? by remember { mutableStateOf(null) }

    LaunchedEffect(lyrics) {
        if (lyrics.sync_type != Song.Lyrics.SyncType.NONE) {
            while (true) {
                val time = PlayerServiceHost.status.position_seconds
                var start = -1
                var end = -1
                var next = Float.POSITIVE_INFINITY

                for (item in terms.withIndex()) {
                    val term = item.value.data as Song.Lyrics.Term

                    val range =
                    if (lyrics.sync_type == Song.Lyrics.SyncType.WORD_SYNC && !Settings.get<Boolean>(Settings.KEY_LYRICS_ENABLE_WORD_SYNC)) {
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
                }

                if (start != -1) {
                    current_range = start..end
                }

                delay(minOf((next * 1000).toLong(), 100))
            }
        }
    }

    Crossfade(targetState = show_furigana) { show_readings ->

        LongFuriganaText(
            terms,
            show_readings,
            font_size = 20.sp,
            line_spacing = 5.dp,
            space_wrapped_lines = false,
            receiveTermRect = { term, rect ->
                (term.data as Song.Lyrics.Term).data = rect
            },

            text_element = { is_reading: Boolean, text: String, font_size: TextUnit, index: Int, modifier: Modifier ->
                val is_current by remember { derivedStateOf { current_range?.contains(index) == true } }
                val colour by animateColorAsState(targetValue = if (is_current) Color.White else Color.White.setAlpha(0.5f))

                Text(
                    text,
                    modifier,
                    fontSize = font_size,
                    color = colour
                )
            },

            list_element = { content ->
                LazyColumn(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    state = scroll_state,
                    horizontalAlignment = when (Settings.get<Int>(Settings.KEY_LYRICS_TEXT_ALIGNMENT)) {
                        0 -> if (LocalLayoutDirection.current == LayoutDirection.Ltr) Alignment.Start else Alignment.End
                        1 -> Alignment.CenterHorizontally
                        else -> if (LocalLayoutDirection.current == LayoutDirection.Ltr) Alignment.End else Alignment.Start
                    }
                ) {
                    item {
                        Spacer(Modifier.requiredHeight(padding_height))
                    }
                    content()
                    item {
                        Spacer(Modifier.requiredHeight(padding_height - if (add_padding) with (LocalDensity.current) { (line_spacing * 2).toDp() } else 0.dp))
                    }
                }
            },
            chunk_size = lyrics.lines.size
        )
    }
}
