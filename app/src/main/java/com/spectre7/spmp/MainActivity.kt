package com.spectre7.spmp

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.spectre7.spmp.model.Song
import com.spectre7.spmp.ui.layout.HomePage
import com.spectre7.spmp.ui.theme.MyApplicationTheme
import com.spectre7.ytmusicapi.Api
import kotlin.concurrent.thread


class MainActivity : ComponentActivity() {

    lateinit var player: PlayerHost

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this

        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        youtube = Api.YtMusicApi(getString(R.string.yt_music_creds))

        player = PlayerHost(this)

//        if (!player.isServiceRunning()) {
//            player.interact {
//                it.addToQueue(Song.fromId("1cGQotpn8r4"))
//                it.play()
//            }
//            sendToast("Not running, calling play")
//        }
//        else {
//            sendToast("Already running, not calling play")
//        }

        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    HomePage()
                }
            }
        }
    }

    override fun onDestroy() {
        player.release()
        super.onDestroy()
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
