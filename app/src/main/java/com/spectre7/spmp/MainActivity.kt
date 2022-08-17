package com.spectre7.spmp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kotlin.concurrent.thread
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.spectre7.spmp.api.DataApi
import com.spectre7.spmp.model.Song
import com.spectre7.spmp.ui.components.SongPreview
import com.spectre7.spmp.ui.layout.SearchPage
import com.spectre7.spmp.ui.theme.MyApplicationTheme
import com.spectre7.ytmusicapi.Api

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this

        if (! Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        youtube = Api.YtMusicApi(getString(R.string.yt_music_creds))

        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SearchPage()
                }
            }
        }
    }

    companion object {
        @JvmStatic
        var instance: MainActivity? = null

        @JvmStatic
        var youtube: Api.YtMusicApi? = null

        fun getString(id: Int): String {
            return instance?.resources?.getString(id)!!
        }
    }

}
