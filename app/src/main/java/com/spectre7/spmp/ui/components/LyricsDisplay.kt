package com.spectre7.spmp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.spectre7.ptl.Ptl
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.R
import com.spectre7.spmp.model.Song
import com.spectre7.spmp.ui.layout.PlayerStatus
import com.spectre7.utils.getString
import com.spectre7.utils.hasKanjiAndHiragana
import com.spectre7.utils.isKanji
import com.spectre7.utils.sendToast
import net.zerotask.libraries.android.compose.furigana.TermInfo
import net.zerotask.libraries.android.compose.furigana.TextData
import net.zerotask.libraries.android.compose.furigana.TextWithReading
import kotlin.concurrent.thread

@Composable
fun LyricsDisplay(song: Song, on_close_request: () -> Unit, p_status: PlayerStatus, size: Dp, open_shutter_menu: (@Composable () -> Unit) -> Unit) {

    var lyrics: Song.Lyrics? by remember { mutableStateOf(null) }
    var show_furigana: Boolean by remember { mutableStateOf(false) }

    var t_first_word: Ptl.TimedLyrics.Word? by remember { mutableStateOf(null) }

    var t_word_start by remember { mutableStateOf(-1) }
    var t_word_end by remember { mutableStateOf(-1) }
    val text_positions = remember { mutableStateListOf<TermInfo>() }
    val overlay_terms: MutableList<TermInfo> = remember { mutableStateListOf() }

    LaunchedEffect(song.getId()) {
        lyrics = null
        t_first_word = null
        overlay_terms.clear()
        song.getLyrics {
            if (it == null) {
                sendToast(getString(R.string.lyrics_unavailable))
                on_close_request()
            }
            else {
                lyrics = it
//                if (lyrics is Song.PTLyrics && (lyrics as Song.PTLyrics).getTimed() != null) {
//                    t_first_word = (lyrics as Song.PTLyrics).getTimed()!!.first_word
//                }
            }
        }
    }

    LaunchedEffect(p_status.position) {
        return@LaunchedEffect
        if (t_first_word != null) {

            thread {
                val pos = (p_status.duration * p_status.position) + 0.15

                var word = t_first_word

                var start = -1
                var end = -1

                while(true) {
                    if (pos >= word!!.start_time && pos < word.end_time) {
                        if (start == -1) {
                            if (t_word_start == word.index) {
                                break
                            }
                            start = word.index
                        }
                    }
                    else if (start != -1) {
                        end = word.index - 1
                        break
                    }

                    if (word.next_word == null) {
                        if (start != -1) {
                            end = word.index
                        }
                        break
                    }

                    word = word.next_word
                }

                if ((start != t_word_start || end != t_word_end) && start != -1 && end != -1) {
                    t_word_start = start
                    t_word_end = end

                    overlay_terms.clear()
                    for (term in text_positions) {
                        val word = term.data as Ptl.TimedLyrics.Word?
                        if (word == null) {
                            break
                        }

                        if (word.index >= t_word_start && word.index <= t_word_end) {
                            if (term.text.trim().length > 0) {
                                overlay_terms.add(term)
                            }
                        }
                        else if (word.index > t_word_end) {
                            break
                        }
                    }
                }
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
            null,
            MainActivity.theme.getAccent(),
            MainActivity.theme.getOnAccent(),
            vertical = true, top = true
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
                            val offset = Offset(-75f, -150f)
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                for (term in overlay_terms) {
                                    drawRect(Color.Red, term.position + offset, Rect(0f, 0f, 50f, 50f).size)
                                }
                            }
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

fun trimOkurigana(original: String, furigana: String): List<Pair<String, String?>> {
    if (original.hasKanjiAndHiragana()) {
        var trim_amount: Int = 0
        for (i in 1 until furigana.length + 1) {
            if (original[original.length - i].isKanji() || original[original.length - i] != furigana[furigana.length - i]) {
                trim_amount = i - 1
                break
            }
        }

        if (trim_amount != 0) {
            return listOf(
                Pair(original.slice(0 until original.length - trim_amount), furigana.slice(0 until furigana.length - trim_amount)),
                Pair(original.slice(original.length - trim_amount until original.length), null)
            )
        }
    }

    return listOf(
        Pair(original, furigana)
    )
}

val kakasi = Python.getInstance().getModule("pykakasi").callAttr("Kakasi")
fun getFuriganaTerms(text: String): List<Triple<String, String, String>> {
    val ret = mutableListOf<Triple<String, String, String>>()
    fun getKey(term: PyObject, key: String): String {
        return term.callAttr("get", key).toString().replace("\\n", "\n").replace("\\r", "\r")
    }
    for (term in kakasi.callAttr("convert", text.replace("\n", "\\n").replace("\r", "\\r")).asList()) {
        ret.add(Triple(getKey(term, "orig"), getKey(term, "hira"), getKey(term, "kana")))
    }
    return ret
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
