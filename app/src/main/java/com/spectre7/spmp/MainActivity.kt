package com.spectre7.spmp

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.spectre7.spmp.api.DataApi
import com.spectre7.spmp.ui.layout.PlayerView
import com.spectre7.spmp.ui.theme.MyApplicationTheme
import com.spectre7.utils.Theme
import kotlinx.coroutines.runBlocking
import kotlin.concurrent.thread

class MainActivity : ComponentActivity() {

    lateinit var theme: Theme

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this

        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        PlayerHost(this)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        setContent {
            MyApplicationTheme {
                theme = Theme.default()

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
        val prefs: SharedPreferences get() = context.getSharedPreferences("com.spectre7.spmp.PREFERENCES", Context.MODE_PRIVATE)
        val theme: Theme get() = context.theme

        @JvmStatic
        private var instance: MainActivity? = null

        fun runInMainThread(code: () -> Unit) {
            Handler(Looper.getMainLooper()).post {
                code()
            }
        }
    }
}