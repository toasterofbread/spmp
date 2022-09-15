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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaquo.python.Python
import com.spectre7.spmp.R
import com.spectre7.spmp.api.DataApi
import com.spectre7.spmp.model.Song
import com.spectre7.utils.*
import com.spectre7.spmp.MainActivity
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

    var first_word: Ptl.TimedLyrics.Word? by remember { mutableStateOf(null) }
    var current_word: Ptl.TimedLyrics.Word? by remember { mutableStateOf(null) }

    LaunchedEffect(song.getId()) {
        lyrics = null
        current_word = null
        first_word = null
        song.getLyrics {
            if (it == null) {
                sendToast(MainActivity.getString(R.string.lyrics_unavailable))
                on_close_request()
            }
            else {
                lyrics = it
                if (lyrics is Song.PTLyrics && (lyrics as Song.PTLyrics).getTimed() != null) {
                    first_word = (lyrics as Song.PTLyrics).getTimed()!!.first_word
                }
            }
        }
    }

    LaunchedEffect(p_status.position) {
        if (first_word != null) {
            val pos = p_status.duration * p_status.position
            var word = first_word
            do {
                if (pos >= word!!.start_time && pos < word!!.end_time) {
                    current_word = word
                    break
                }
                word = word.next_word
            } while(word != null)
            println("${current_word?.index} | ${current_word?.text}")
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
                        val provider = remember(current_word) {
                            object : ModifierProvider {
                                override fun getMainModifier(text: String, text_index: Int, term_index: Int): Modifier {
                                    return if (current_word != null && current_word!!.index == term_index) Modifier.background(Color.Red) else Modifier
                                }
                                override fun getFuriModifier(text: String, text_index: Int, term_index: Int): Modifier {
                                    return getMainModifier(text, text_index, term_index)
                                }
                            }
                        }
                        FuriganaText(it.getLyricsString(), show_furigana, modifier_provider = provider)
                    }
                }
            }
        })

        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.SpaceBetween) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = show_furigana, onCheckedChange = { show_furigana = it })
                Text(MainActivity.getString(R.string.show_furigana))
            }

            IconButton(onClick = on_close_request) {
                Image(painterResource(R.drawable.ic_close), "", colorFilter = ColorFilter.tint(Color.White))
            }
        }
    }

}

val kakasi = Python.getInstance().getModule("pykakasi").callAttr("Kakasi")

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

@Composable
fun FuriganaText(text: String, show_furigana: Boolean, trim_okurigana: Boolean = true, modifier_provider: ModifierProvider? = null) {

    val text_content = remember(text) {
        val content: MutableList<TextData> = mutableStateListOf<TextData>()

        for (term in kakasi.callAttr("convert", text.replace("\n", "\\n").replace("\r", "\\r")).asList()) {
            fun getKey(key: String): String {
                return term.callAttr("get", key).toString().replace("\\n", "\n").replace("\\r", "\r")
            }

            val orig = getKey("orig")
            val hira = getKey("hira")

            if (orig != hira && orig != getKey("kana")) {
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
            modifier_provider = modifier_provider
        )
    }
}
