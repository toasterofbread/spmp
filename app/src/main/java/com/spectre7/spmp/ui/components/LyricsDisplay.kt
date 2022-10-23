package com.spectre7.spmp.ui.components

import android.util.Log
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.background
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaquo.python.Python
import com.chaquo.python.PyObject
import com.spectre7.spmp.R
import com.spectre7.spmp.api.DataApi
import com.spectre7.spmp.model.Song
import com.spectre7.utils.*
import com.spectre7.utils.getString
import com.spectre7.spmp.ui.layout.PlayerStatus
import net.zerotask.libraries.android.compose.furigana.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import com.spectre7.ptl.Ptl
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Rect
import kotlin.concurrent.thread

@Composable
fun LyricsDisplay(song: Song, on_close_request: () -> Unit, p_status: PlayerStatus) {

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
                if (lyrics is Song.PTLyrics && (lyrics as Song.PTLyrics).getTimed() != null) {
                    t_first_word = (lyrics as Song.PTLyrics).getTimed()!!.first_word
                }
            }
        }
    }

    LaunchedEffect(p_status.position) {

        if (t_first_word != null) {

            thread {
                val pos = (p_status.duration * p_status.position) + 0.15

                var word = t_first_word

                var start = -1
                var end = -1

                while(true) {
                    if (pos >= word!!.start_time && pos < word!!.end_time) {
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

                    word = word!!.next_word
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

    Column(verticalArrangement = Arrangement.Bottom) {
        LazyColumn(modifier = Modifier
            .weight(1f)
            .fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, content = {
            item {
                Box {
                    val offset = Offset(-75f, -150f)
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        for (term in overlay_terms) {
                            drawRect(Color.Red, term.position + offset, Rect(0f, 0f, 50f, 50f).size)
                        }
                    }

                    Crossfade(targetState = lyrics) {
                        if (it == null) {
                            CircularProgressIndicator()
                        }
                        else {
                            Column {
                                if (it is Song.PTLyrics && it.getTimed() != null) {
                                    val terms = mutableListOf<Pair<String, Ptl.TimedLyrics.Word>>()

                                    for (line in it.getTimed()!!.lines) {
                                        for (i in 0 until line.words.size) {
                                            val word = line.words[i]
                                            if (i + 1 == line.words.size) {
                                                terms.add(Pair(word.text + "\n", word))
                                            }
                                            else {
                                                terms.add(Pair(word.text, word))
                                            }
                                        }
                                    }

                                    FuriganaText(terms, show_furigana, text_positions = text_positions)
                                }
                                else {
                                    FuriganaText(listOf(Pair(it.getLyricsString(), null)), show_furigana, text_positions = text_positions)
                                }

                                Text(getString(R.string.lyrics_source_prefix) + it.getSource(), textAlign = TextAlign.Left, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            }
        })

        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = show_furigana, onCheckedChange = { show_furigana = it })
                Text(getString(R.string.show_furigana))
            }

            IconButton(onClick = on_close_request) {
                Image(painterResource(R.drawable.ic_close), "", colorFilter = ColorFilter.tint(Color.White))
            }
        }
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
fun FuriganaText(terms: List<Pair<String, Any?>>, show_furigana: Boolean, trim_okurigana: Boolean = true, modifier_provider: ModifierProvider? = null, text_positions: MutableList<TermInfo>? = null) {

    val text_content = remember(terms) {
        val content: MutableList<TextData> = mutableStateListOf<TextData>()

        for (term in terms) {
            for ((orig, hira, kata) in getFuriganaTerms(term.first)) {
                if (orig != hira && orig != kata) {
                    if (trim_okurigana) {
                        for (pair in trimOkurigana(orig, hira)) {
                            content.add(TextData(
                                text = pair.first,
                                reading = pair.second,
                                data = term.second
                            ))
                        }
                    }
                    else {
                        content.add(TextData(
                            text = orig,
                            reading = hira,
                            data = term.second
                        ))
                    }
                }
                else {
                    content.add(TextData(
                        text = orig,
                        reading = null,
                        data = term.second
                    ))
                }
            }
        }

        content
    }

    Crossfade(targetState = show_furigana) {
        TextWithReading(
            text_content,
            showReadings = it,
            textAlign = TextAlign.Left,
            lineHeight = 42.sp,
            fontSize = 20.sp,
            modifier = Modifier.padding(20.dp),
            modifier_provider = modifier_provider,
            text_positions = text_positions
        )
    }
}
