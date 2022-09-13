package com.spectre7.spmp

import android.util.Log
import android.content.res.Resources
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.spectre7.spmp.ui.layout.PlayerView
import com.spectre7.spmp.ui.theme.MyApplicationTheme
import com.spectre7.ytmusicapi.Api

class MainActivity : ComponentActivity() {

    lateinit var player: PlayerHost
    lateinit var youtube: Api.YtMusicApi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this

        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        youtube = Api.YtMusicApi(getString(R.string.yt_music_creds))
        PlayerHost(this)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PlayerView()
                }
            }
        }
    }

    override fun onDestroy() {
        PlayerHost.release()
        super.onDestroy()
    }

    companion object {

        val context: MainActivity get() = instance!!
        val resources: Resources get() = context.resources
        val youtube: Api.YtMusicApi get() = context.youtube

        @JvmStatic
        private var instance: MainActivity? = null

        fun getString(id: Int): String {
            return instance?.resources?.getString(id)!!
        }

        fun runInMainThread(code: () -> Unit) {
            Handler(Looper.getMainLooper()).post {
                code()
            };
        }
    }
}
