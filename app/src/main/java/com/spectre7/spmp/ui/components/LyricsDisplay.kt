package com.spectre7.spmp.ui.components

import android.util.Log
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import net.zerotask.libraries.android.compose.furigana.TextData
import net.zerotask.libraries.android.compose.furigana.TextWithReading
import java.io.BufferedReader
import java.io.InputStreamReader
import net.reduls.igo.Morpheme
import net.reduls.igo.Tagger
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

@Composable
fun LyricsDisplay(song: Song, on_close_request: () -> Unit) {

    var lyrics: Song.Lyrics? by remember { mutableStateOf(null) }
    var show_furigana: Boolean by remember { mutableStateOf(false) }

    LaunchedEffect(song.getId()) {
        lyrics = null
        song.getLyrics {
            lyrics = it
            if (lyrics == null) {
                sendToast("Lyrics unavailable")
                on_close_request()
            }
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
                        FuriganaText(it.lyrics, show_furigana)
                    }
                }
            }
        })

        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.SpaceBetween) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = show_furigana, onCheckedChange = { show_furigana = it })
                Text("Show furigana")
            }

            IconButton(onClick = on_close_request) {
                Image(painterResource(R.drawable.ic_close), "", colorFilter = ColorFilter.tint(Color.White))
            }
        }
    }

}

fun prepareIgoDict(): String {
    val path = MainActivity.context.getExternalFilesDir(null)
    val dict = File(path, "ipadic")
    dict.mkdirs()

    val assets = MainActivity.context.getAssets()

    for (file in assets.list("ipadic")!!) {
        val dest = File(dict, file)
        if (!dest.exists()) {
            val s = assets.open("ipadic/$file")
            dest.outputStream().use { fileOut ->
                s.copyTo(fileOut)
            }
        }
    }

    return dict.absolutePath
}

val kakasi = Python.getInstance().getModule("pykakasi").callAttr("Kakasi")

@Composable
fun FuriganaText(text: String, show_furigana: Boolean, trim_okurigana: Boolean = true) {

    // TODO | Consider replacing Kakasi with Igo
    // val dict_path = prepareIgoDict()
    // for(m in Tagger(dict_path).parse("汚れなし")) {
    //     Log.d("IGO SURFACE", m.surface)
    //     Log.d("IGO FEATURE", m.feature)
    //     Log.d("IGO START", m.start.toString())
    // }

    fun generateContent(text: String, content: MutableList<TextData>): MutableList<TextData> {
        for (term in kakasi.callAttr("convert", text.replace("\n", "\\n").replace("\r", "\\r")).asList()) {
            fun getKey(key: String): String {
                return term.callAttr("get", key).toString().replace("\\n", "\n").replace("\\r", "\r")
            }

            val original = getKey("orig")
            val hiragana = getKey("hira")

            if (original != hiragana && original != getKey("kana")) {
                if (trim_okurigana && original.hasKanjiAndHiragana()) {
                    var trim_amount: Int = 0
                    for (i in 1 until hiragana.length + 1) {
                        if (original[original.length - i].isKanji() || original[original.length - i] != hiragana[hiragana.length - i]) {
                            trim_amount = i - 1
                            break
                        }
                    }

                    if (trim_amount != 0) {
                        content.add(TextData(
                            text = original.slice(0 until original.length - trim_amount),
                            reading = hiragana.slice(0 until hiragana.length - trim_amount)
                        ))

                        content.add(TextData(
                            text = original.slice(original.length - trim_amount until original.length),
                            reading = null
                        ))
                    }
                }
                else {
                    content.add(TextData(
                        text = original,
                        reading = hiragana
                    ))
                }
            }
            else {
                content.add(TextData(
                    text = original,
                    reading = null
                ))
            }


        }
        return content
    }

    val text_content = remember(text) { generateContent(text, mutableStateListOf<TextData>()) }

    Crossfade(targetState = show_furigana) {
        TextWithReading(
            text_content,
            showReadings = it,
            textAlign = TextAlign.Left,
            lineHeight = 42.sp,
            fontSize = 20.sp,
            modifier = Modifier.padding(20.dp)
        )
    }
}
