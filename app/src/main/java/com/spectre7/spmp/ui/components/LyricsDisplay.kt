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
import net.zerotask.libraries.android.compose.furigana.TextData
import net.zerotask.libraries.android.compose.furigana.TextWithReading
import net.zerotask.libraries.android.compose.furigana.ModifierProvider
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import com.spectre7.ptl.Ptl

@Composable
fun LyricsDisplay(song: Song, on_close_request: () -> Unit, p_status: PlayerStatus) {

    var lyrics: Song.Lyrics? by remember { mutableStateOf(null) }
    var show_furigana: Boolean by remember { mutableStateOf(false) }

    var t_first_word: Ptl.TimedLyrics.Word? by remember { mutableStateOf(null) }
    var t_current_word: Ptl.TimedLyrics.Word? by remember { mutableStateOf(null) }

    LaunchedEffect(song.getId()) {
        lyrics = null
        t_current_word = null
        t_first_word = null
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

    val text_positions = remember { mutableStateListOf<Pair<Int, Offset>>() }

    LaunchedEffect(p_status.position) {
        if (t_first_word != null) {
            val pos = p_status.duration * p_status.position

            // If no current word
            if (t_current_word == null) {
                var word = t_first_word
                do {
                    if (pos >= word!!.start_time && pos < word!!.end_time) {
                        t_current_word = word
                        break
                    }
                    else if (pos < word!!.start_time) {
                        break
                    }
                    word = word.next_word
                } while(word != null)
            }
            // If playback is ahead of current word
            else if (pos >= t_current_word!!.end_time) {
                var word = t_current_word!!.next_word
                while(word != null) {
                    if (pos >= word!!.start_time && pos < word!!.end_time) {
                        t_current_word = word
                        break
                    }
                    else if (pos < word!!.start_time) {
                        break
                    }
                    word = word.next_word
                }
            }
            // If playback is behind current word
            else if (pos < t_current_word!!.start_time) {
                var word = t_current_word!!.prev_word
                while(word != null) {
                    if (pos >= word!!.start_time && pos < word!!.end_time) {
                        t_current_word = word
                        break
                    }
                    else if (pos >= word!!.end_time) {
                        break
                    }
                    word = word.prev_word
                }
            }
            // println("${t_current_word?.index} | ${t_current_word?.text}")
        }
    }

    Column(verticalArrangement = Arrangement.Bottom) {
        LazyColumn(modifier = Modifier
            .weight(1f)
            .fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, content = {
            item {
                Crossfade(targetState = lyrics) {
                    if (it == null) {
                        CircularProgressIndicator()
                    }
                    else {
                        if (t_current_word != null) {
                            Box(Modifier.requiredSize(25.dp).background(Color.Red).offset(text_positions[t_current_word!!.index]))
                        }

                        Column {
                            FuriganaText(it.getLyricsString(), show_furigana, text_positions = text_positions)
                            Text(getString(R.string.lyrics_source_prefix) + it.getSource(), textAlign = TextAlign.Left, modifier = Modifier.fillMaxWidth())
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
fun FuriganaText(text: String, show_furigana: Boolean, trim_okurigana: Boolean = true, modifier_provider: ModifierProvider? = null, text_positions: MutableList<Pair<Int, Offset>>? = null) {

    val text_content = remember(text) {
        val content: MutableList<TextData> = mutableStateListOf<TextData>()

        for ((orig, hira, kata) in getFuriganaTerms(text)) {
            if (orig != hira && orig != kata) {
                if (trim_okurigana) {
                    for (pair in trimOkurigana(orig, hira)) {
                        content.add(TextData(
                            text = pair.first,
                            reading = pair.second
                        ))
                    }
                }
                else {
                    content.add(TextData(
                        text = orig,
                        reading = hira
                    ))
                }
            }
            else {
                content.add(TextData(
                    text = orig,
                    reading = null
                ))
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
