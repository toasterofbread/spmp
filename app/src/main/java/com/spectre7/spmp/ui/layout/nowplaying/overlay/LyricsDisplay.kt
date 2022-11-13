package com.spectre7.spmp.ui.layout.nowplaying.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
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
import kotlin.math.abs

@Composable
fun LyricsDisplay(song: Song, close: () -> Unit, size: Dp, seek_state: Any, open_shutter_menu: (@Composable () -> Unit) -> Unit) {

    var lyrics: Song.Lyrics? by remember { mutableStateOf(null) }
    var show_furigana: Boolean by remember { mutableStateOf(MainActivity.prefs.getBoolean("lyrics_default_furigana", true)) }

    val text_positions = remember { mutableStateListOf<TermInfo>() }
    val scroll_state = rememberLazyListState()

    LaunchedEffect(song.getId()) {
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

    AnimatedVisibility(lyrics != null, Modifier.zIndex(10f), enter = fadeIn(), exit = fadeOut()) {
        PillMenu(
            3,
            { index, _ ->
                when (index) {
                    0 -> ActionButton(Icons.Filled.Close, close)
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

    Column(verticalArrangement = Arrangement.Bottom) {
        LazyColumn(state = scroll_state, modifier =  Modifier
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
                            val size_px = with(LocalDensity.current) { size.toPx() }
                            TimingOverlay(it, text_positions, false, seek_state) { position ->
                                if (MainActivity.prefs.getBoolean("lyrics_follow_enabled", true)) {
                                    val offset = size_px * MainActivity.prefs.getFloat("lyrics_follow_offset", 0.5f)
                                    scroll_state.animateScrollToItem(0, (position - offset).toInt())
                                }
                            }
                            Column {
                                val terms = remember { mutableListOf<TextData>().apply {
                                    for (line in it.lyrics) {
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

                                Crossfade(targetState = show_furigana) {
                                    TextWithReading(
                                        terms,
                                        show_readings = it,
                                        textAlign = when (MainActivity.prefs.getInt("lyrics_text_alignment", 0)) {
                                            0 -> TextAlign.Left
                                            1 -> TextAlign.Center
                                            else -> TextAlign.Right
                                        },
                                        lineHeight = 42.sp,
                                        fontSize = 20.sp,
                                        color = Color.White,
                                        modifier = Modifier.padding(20.dp),
                                        text_positions = text_positions
                                    )
                                }
                            }
                        }
                    }
                }
            }
        })
    }
}

@Composable
fun TimingOverlay(lyrics: Song.Lyrics, text_positions: List<TermInfo>, full_line: Boolean, seek_state: Any, scrollTo: suspend (Float) -> Unit) {

    var show_highlight by remember { mutableStateOf(false) }
    var highlight_instantly by remember { mutableStateOf(false) }
    var highlight_unset by remember { mutableStateOf(true) }

    var highlight_position_x by remember { mutableStateOf(-1f) }
    var highlight_position_y by remember { mutableStateOf(-1f) }
    var highlight_width by remember { mutableStateOf(-1f) }
    var highlight_height by remember { mutableStateOf(-1f) }

    val highlight_position_state = animateOffsetAsState(
        targetValue = Offset(highlight_position_x, highlight_position_y)
    )
    val highlight_size_state = animateOffsetAsState(
        targetValue = Offset(highlight_width, highlight_height)
    )

    AnimatedVisibility(show_highlight && !highlight_unset) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (highlight_instantly) {
                drawRoundRect(MainActivity.theme.getBackground(true), Offset(highlight_position_x, highlight_position_y), Size(highlight_width, highlight_height), CornerRadius(25f, 25f))
            }
            else {
                drawRoundRect(MainActivity.theme.getBackground(true), highlight_position_state.value, Size(highlight_size_state.value.x, highlight_size_state.value.y), CornerRadius(25f, 25f))
            }
        }
    }

    LaunchedEffect(lyrics, seek_state) {
        show_highlight = false
        highlight_unset = true
    }

    LaunchedEffect(highlight_position_y) {
        scrollTo(highlight_position_y)
    }

    LaunchedEffect(PlayerHost.status.m_position, full_line) {

        val offset = Offset(-100f, -170f)

        val terms = mutableListOf<Song.Lyrics.Subterm>()
        val pos = (PlayerHost.status.duration * PlayerHost.status.position)
        var finished = false

        for (line in lyrics.lyrics) {
            for (term in line) {
                if (pos >= term.start && pos < term.end) {
                    if (full_line) {
                        for (_term in line) {
                            for (subterm in _term.subterms) {
                                terms.add(subterm)
                            }
                        }
                        finished = true
                    }
                    else {
                        for (subterm in term.subterms) {
                            terms.add(subterm)
                        }
                    }
                    break
                }
            }
            if (finished) {
                break
            }
        }

        var target_x: Float? = null
        var target_y: Float? = null
        var target_br_x: Float? = null
        var target_br_y: Float? = null

        for (term in terms) {
            val rect = text_positions[term.index].rect

            if (target_x == null || rect.left < target_x) {
                target_x = rect.left + offset.x
            }
            if (target_y == null || rect.top < target_y) {
                target_y = rect.top + offset.y
            }

            if (target_br_x == null || rect.right > target_br_x) {
                target_br_x = rect.right + offset.x
            }
            if (target_br_y == null || rect.bottom > target_br_y) {
                target_br_y = rect.bottom + offset.y
            }
        }

        if (target_x != null) {

            if (highlight_position_x != target_x || highlight_position_y != target_y || highlight_width != abs(target_br_x!! - target_x) || highlight_height != abs(target_br_y!! - target_y)) {
                highlight_position_x = target_x
                highlight_position_y = target_y!!
                highlight_width = abs(target_br_x!! - target_x)
                highlight_height = abs(target_br_y!! - target_y)

                highlight_instantly = highlight_unset
                highlight_unset = false
            }

            show_highlight = true
        }
    }
}
