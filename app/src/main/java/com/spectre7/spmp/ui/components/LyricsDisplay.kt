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
import com.spectre7.utils.sendToast
import com.spectre7.spmp.MainActivity
import net.zerotask.libraries.android.compose.furigana.TextData
import net.zerotask.libraries.android.compose.furigana.TextWithReading
import java.io.BufferedReader
import java.io.InputStreamReader
import net.reduls.gomoku.Morpheme
import net.reduls.gomoku.Tagger
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
fun FuriganaText(text: String, show_furigana: Boolean) {

    // val dict_path = prepareIgoDict()

    for(m in Tagger.parse("汚れなし")) {
        sendToast("surface: ${m.surface}\nfeature: ${m.feature}\nstart: ${m.start}")
    }

    fun generateContent(text: String, content: MutableList<TextData>): MutableList<TextData> {
        for (term in kakasi.callAttr("convert", text.replace("\n", "\\n").replace("\r", "\\r")).asList()) {
            fun getKey(key: String): String {
                return term.callAttr("get", key).toString().replace("\\n", "\n").replace("\\r", "\r")
            }

            val original = getKey("orig")
            val hiragana = getKey("hira")
            val katakana = getKey("kana")

            content.add(TextData(
                text = original,
                reading = if (original != hiragana && original != katakana) hiragana else null
            ))
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
