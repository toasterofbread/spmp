package com.spectre7.spmp.ui.layout.nowplaying.overlay.lyrics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.PlayerServiceHost
import com.spectre7.spmp.R
import com.spectre7.spmp.model.Settings
import com.spectre7.spmp.model.Song
import com.spectre7.spmp.ui.component.PillMenu
import com.spectre7.spmp.ui.layout.nowplaying.overlay.OverlayMenu
import com.spectre7.utils.LongFuriganaText
import com.spectre7.utils.getString
import com.spectre7.utils.sendToast
import net.zerotask.libraries.android.compose.furigana.TermInfo
import net.zerotask.libraries.android.compose.furigana.TextData

class LyricsOverlayMenu(
    val size: Dp
): OverlayMenu() {
    
    override fun closeOnTap(): Boolean = false

    @Composable
    override fun Menu(
        song: Song,
        expansion: Float,
        openShutterMenu: (@Composable () -> Unit) -> Unit,
        close: () -> Unit,
        seek_state: Any
    ) {

        var lyrics: Song.Lyrics? by remember { mutableStateOf(null) }
        var show_furigana: Boolean by remember { mutableStateOf(Settings.prefs.getBoolean(Settings.KEY_LYRICS_DEFAULT_FURIGANA.name, true)) }

        val scroll_state = rememberLazyListState()
        var search_menu_open by remember { mutableStateOf(false) }
        var reload_lyrics by remember { mutableStateOf(false) }

        LaunchedEffect(song.id, reload_lyrics) {
            lyrics = null
            song.getLyrics {
                if (it == null) {
                    sendToast(getString(R.string.lyrics_unavailable))
                    search_menu_open = true
                }
                else {
                    lyrics = it
                }
            }
        }

        AnimatedVisibility(lyrics != null && !search_menu_open, Modifier.zIndex(10f), enter = fadeIn(), exit = fadeOut()) {
            remember { PillMenu() }.PillMenu(
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
                remember { mutableStateOf(false) },
                MainActivity.theme.getAccent(),
                vertical = true
            )
        }

        Crossfade(search_menu_open) { edit ->
            if (edit) {
                LyricsSearchMenu(song, lyrics) { changed ->
                    search_menu_open = false
                    if (changed) {
                        lyrics = null
                        reload_lyrics = !reload_lyrics
                    }
                }
            }
            else {
                ScrollingLyricsDisplay(size, seek_state, lyrics, scroll_state, show_furigana)
            }
        }
    }
}

@Composable
fun ScrollingLyricsDisplay(size: Dp, seek_state: Any, lyrics: Song.Lyrics?, scroll_state: LazyListState, show_furigana: Boolean) {
    Box {
        Crossfade(targetState = lyrics) {
            if (it == null) {
                Column(Modifier.size(size), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text("Loading lyrics", fontWeight = FontWeight.Light) // TODO
                    Spacer(Modifier.height(20.dp))
                    LinearProgressIndicator(color = MainActivity.theme.getAccent(), trackColor = MainActivity.theme.getOnAccent())
                }
            }
            else {
                CoreLyricsDisplay(size, seek_state, it, scroll_state, show_furigana)
            }
        }
    }
}

@Composable
fun CoreLyricsDisplay(size: Dp, seek_state: Any, lyrics: Song.Lyrics, scroll_state: LazyListState, show_furigana: Boolean) {
    val text_positions = remember { mutableStateListOf<TermInfo>() }
    val size_px = with(LocalDensity.current) { size.toPx() }

    val line_height = with (LocalDensity.current) { 20.sp.toPx() }
    val line_spacing = with (LocalDensity.current) { 25.dp.toPx() }

    val pos = (PlayerServiceHost.status.duration * PlayerServiceHost.status.m_position)
    var range by remember { mutableStateOf(-1 .. -1) }

    LaunchedEffect(pos) {
        var finished = false
        val full_line = true

        var start = -1
        var end = -1

        for (line in lyrics.lyrics.withIndex()) {
            for (term in line.value) {
                if (pos >= term.start!! && pos < term.end!!) {
                    if (full_line) {
                        for (_term in line.value) {
                            for (subterm in _term.subterms) {
                                if (start == -1) {
                                    start = subterm.index
                                }
                                if (end == -1 || end < subterm.index) {
                                    end = subterm.index
                                }
                            }
                        }
                        finished = true
                    } else {
                        for (subterm in term.subterms) {
                            if (start == -1) {
                                start = subterm.index
                            }
                            if (end == -1 || end < subterm.index) {
                                end = subterm.index
                            }
                        }
                    }
                    break
                }
            }
            if (finished) {
                break
            }
        }

        range = start .. end
    }

//    LyricsTimingOverlay(lyrics, text_positions, false, seek_state, scroll_state) { position ->
//        if (Settings.prefs.getBoolean(Settings.KEY_LYRICS_FOLLOW_ENABLED.name, true)) {
//            val offset = size_px * Settings.prefs.getFloat(Settings.KEY_LYRICS_FOLLOW_OFFSET.name, 0.5f)
//            scroll_state.animateScrollToItem(0, (position - offset).toInt())
//        }
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
        for (line in lyrics.lyrics) {
            for (i in line.indices) {
                val term = line[i]
                for (j in term.subterms.indices) {
                    val subterm = term.subterms[j]

                    if (j + 1 == term.subterms.size && i + 1 == line.size) {
                        add(TextData(subterm.text + "\n", subterm.furi, term))
                    }
                    else {
                        add(TextData(subterm.text, subterm.furi, term))
                    }
                }
            }
        }
    } }

    Crossfade(targetState = show_furigana) { show_readings ->

        LongFuriganaText(
            terms,
            show_readings,
            font_size = 20.sp,
            line_spacing = 25.dp,
            space_wrapped_lines = false,
            text_positions = text_positions,

            text_element = { is_reading: Boolean, text: String, font_size: TextUnit, index: Int, modifier: Modifier ->
                Text(
                    text,
                    if (range.contains(index)) modifier.background(MainActivity.theme.getAccent(), CircleShape) else modifier,
                    fontSize = font_size,
                    color = Color.White,
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
            }
        )
    }
}
