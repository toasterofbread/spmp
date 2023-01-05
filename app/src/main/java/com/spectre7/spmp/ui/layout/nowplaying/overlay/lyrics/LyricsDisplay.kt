package com.spectre7.spmp.ui.layout.nowplaying.overlay.lyrics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.R
import com.spectre7.spmp.model.Settings
import com.spectre7.spmp.model.Song
import com.spectre7.spmp.ui.component.PillMenu
import com.spectre7.utils.LongFuriganaText
import com.spectre7.utils.getString
import com.spectre7.utils.sendToast
import net.zerotask.libraries.android.compose.furigana.TermInfo
import net.zerotask.libraries.android.compose.furigana.TextData

@Composable
fun LyricsDisplay(song: Song, close: () -> Unit, size: Dp, seek_state: Any, openShutterMenu: (@Composable () -> Unit) -> Unit) {

    var lyrics: Song.Lyrics? by remember { mutableStateOf(null) }
    var show_furigana: Boolean by remember { mutableStateOf(Settings.prefs.getBoolean(Settings.KEY_LYRICS_DEFAULT_FURIGANA.name, true)) }

    val scroll_state = rememberLazyListState()
    var edit_menu_open by remember { mutableStateOf(false) }
    var reload_lyrics by remember { mutableStateOf(false) }

    LaunchedEffect(song.id, reload_lyrics) {
        lyrics = null
        song.getLyrics {
            if (it == null) {
                sendToast(getString(R.string.lyrics_unavailable))
                close()
            }
            else {
                lyrics = it
            }
        }
    }

    AnimatedVisibility(lyrics != null && !edit_menu_open, Modifier.zIndex(10f), enter = fadeIn(), exit = fadeOut()) {
        PillMenu(
            3,
            { index, _ ->
                when (index) {
                    0 -> ActionButton(Icons.Filled.Close, close)
                    1 -> ActionButton(Icons.Filled.Search) { edit_menu_open = true }
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
                                    this@PillMenu.close()
                                })
                        )
                    }
                }
            },
            remember { mutableStateOf(false) },
            MainActivity.theme.getAccent(),
            MainActivity.theme.getOnAccent(),
            vertical = true
        )
    }

    Crossfade(edit_menu_open) { edit ->
        if (edit) {
            LyricsSearchMenu(song, lyrics) { changed ->
                edit_menu_open = false
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

    LyricsTimingOverlay(lyrics, text_positions, false, seek_state) { position ->
        if (Settings.prefs.getBoolean(Settings.KEY_LYRICS_FOLLOW_ENABLED.name, true)) {
            val offset = size_px * Settings.prefs.getFloat(Settings.KEY_LYRICS_FOLLOW_OFFSET.name, 0.5f)
            scroll_state.animateScrollToItem(0, (position - offset).toInt())
        }
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

            text_element = { is_reading: Boolean, text: String, font_size: TextUnit, modifier: Modifier ->
                Text(
                    text,
                    modifier,
                    fontSize = font_size,
                    lineHeight = 42.sp,
                    color = Color.White,
                )
            },

            list_element = { content ->
                LazyColumn(
                    Modifier.fillMaxSize().padding(start = 20.dp, end = 20.dp),
                    state = scroll_state,
                    horizontalAlignment = when (Settings.get<Int>(Settings.KEY_LYRICS_TEXT_ALIGNMENT)) {
                        0 -> if (LocalLayoutDirection.current == LayoutDirection.Ltr) Alignment.Start else Alignment.End
                        1 -> Alignment.CenterHorizontally
                        else -> if (LocalLayoutDirection.current == LayoutDirection.Ltr) Alignment.End else Alignment.Start
                    },
                    content = content
                )
            }
        )
    }
}
