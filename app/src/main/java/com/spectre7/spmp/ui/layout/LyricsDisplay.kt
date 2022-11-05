package com.spectre7.spmp.ui.layout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.PlayerHost
import com.spectre7.spmp.R
import com.spectre7.spmp.model.Song
import com.spectre7.spmp.ui.component.PillMenu
import com.spectre7.utils.getString
import com.spectre7.utils.sendToast
import net.zerotask.libraries.android.compose.furigana.TermInfo
import net.zerotask.libraries.android.compose.furigana.TextData
import net.zerotask.libraries.android.compose.furigana.TextWithReading

@Composable
fun LyricsDisplay(song: Song, on_close_request: () -> Unit, size: Dp, open_shutter_menu: (@Composable () -> Unit) -> Unit) {

    var lyrics: Song.Lyrics? by remember { mutableStateOf(null) }
    var show_furigana: Boolean by remember { mutableStateOf(false) }

    val text_positions = remember { mutableStateListOf<TermInfo>() }

    LaunchedEffect(song.getId()) {
        lyrics = null
        song.getLyrics {
            if (it == null) {
                sendToast(getString(R.string.lyrics_unavailable))
                on_close_request()
            }
            else {
                lyrics = it
            }
        }
    }

    AnimatedVisibility(lyrics != null, Modifier.zIndex(10f), enter = fadeIn(), exit = fadeOut()) {
        PillMenu(
            3,
            { index ->
                when (index) {
                    0 -> ActionButton(Icons.Filled.Close, on_close_request)
                    1 -> ActionButton(Icons.Filled.Info) {
                        open_shutter_menu {
                            if (lyrics != null) {
                                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                                    Text("Lyrics info", fontSize = 20.sp, fontWeight = FontWeight.Light)
                                    Spacer(Modifier.height(20.dp))
                                    Text(getString(R.string.lyrics_source_prefix) + lyrics!!.source, color = Color.White)
                                }
                            }
                        }
                    }
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
                                    close()
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

    Column(verticalArrangement = Arrangement.Bottom) {
        LazyColumn(modifier = Modifier
            .weight(1f)
            .fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, content = {
            item {
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
                            TimingOverlay(it, text_positions, song.getId())
                            Column {
                                val terms = mutableListOf<TextData>()
                                for (line in it.lyrics) {
                                    for (i in line.indices) {
                                        val term = line[i]
                                        for (j in term.subterms.indices) {
                                            val subterm = term.subterms[j]

                                            if (j + 1 == term.subterms.size && i + 1 == line.size) {
                                                terms.add(TextData(subterm.text + "\n", subterm.furi, term))
                                            }
                                            else {
                                                terms.add(TextData(subterm.text, subterm.furi, term))
                                            }
                                        }
                                    }
                                }

                                FuriganaText(terms, show_furigana, text_positions = text_positions)
                            }
                        }
                    }
                }
            }
        })
    }
}

@Composable
fun FuriganaText(terms: List<TextData>, show_furigana: Boolean, text_positions: MutableList<TermInfo>? = null) {
    Crossfade(targetState = show_furigana) {
        TextWithReading(
            terms,
            showReadings = it,
            textAlign = TextAlign.Left,
            lineHeight = 42.sp,
            fontSize = 20.sp,
            color = Color.White,
            modifier = Modifier.padding(20.dp),
            text_positions = text_positions
        )
    }
}

@Composable
fun TimingOverlay(lyrics: Song.Lyrics, text_positions: List<TermInfo>, reset_state: Any) {
    val overlay_terms: MutableList<TermInfo> = remember { mutableStateListOf() }

    Canvas(modifier = Modifier.fillMaxSize()) {
        for (term in overlay_terms) {
            drawRect(Color.Red, term.rect.topLeft + Offset(-80f, -160f), term.rect.size)
        }
    }

    LaunchedEffect(reset_state) {
        overlay_terms.clear()
    }

    val p_status = PlayerHost.p_status
    LaunchedEffect(p_status.position) {
        val pos = (p_status.duration * p_status.position)

        val lines = lyrics.lyrics
        var term_index = 0

        overlay_terms.clear()
        for (i in lines.indices) {
            val line = lines[i]
            for (j in line.indices) {
                val term = line[j]
                for (subterm in term.subterms) {
                    if (pos >= term.start && pos < term.end) {
                        overlay_terms.add(text_positions[term_index])
                    }
                    term_index++
                }
            }
        }
    }
}
