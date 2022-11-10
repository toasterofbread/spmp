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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.spectre7.spmp.ui.layout.PlayerView
import com.spectre7.spmp.ui.theme.MyApplicationTheme
import com.spectre7.utils.Theme

class MainActivity : ComponentActivity() {

    lateinit var theme: Theme
    lateinit var languages: Map<String, Map<String, String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this

        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        val data = resources.assets.open("languages.json").bufferedReader()

        val langs = mutableMapOf<String, Map<String, String>>()
        for (item in Klaxon().parseJsonObject(data).entries) {
            val map = mutableMapOf<String, String>()
            for (subitem in (item.value as JsonObject).entries) {
                map[subitem.key] = subitem.value.toString()
            }
            langs[item.key] = map
        }
        languages = langs

        PlayerHost()

        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        setContent {
            MyApplicationTheme {
                theme = Theme.default()

                Surface(modifier = Modifier.fillMaxSize()) {
                    if (PlayerHost.service_connected) {
                        PlayerView()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        PlayerHost.release()
    }

    companion object {

        class NetworkConnectivityManager {
            private val retry_callbacks = mutableListOf<() -> Unit>()
            private val error_callbacks = mutableListOf<() -> Unit>()

            var error by mutableStateOf<Exception?>(null)
            val connected: Boolean get() = error == null

            fun onError(e: Exception) {
                error = e
                throw e
                for (callback in error_callbacks) {
                    callback()
                }
            }
            fun onRetry() {
                if (!connected) {
                    error = null
                    for (callback in retry_callbacks) {
                        callback()
                        if (!connected) {
                            break
                        }
                    }
                }
            }

            fun addErrorCallback(callback: () -> Unit) {
                error_callbacks.add(callback)
            }
            fun removeErrorCallback(callback: () -> Unit) {
                error_callbacks.remove(callback)
            }

            fun addRetryCallback(callback: () -> Unit) {
                retry_callbacks.add(callback)
            }
            fun removeRetryCallback(callback: () -> Unit) {
                retry_callbacks.remove(callback)
            }
        }
        val network = NetworkConnectivityManager()

        val context: MainActivity get() = instance!!
        val resources: Resources get() = context.resources
        val prefs: SharedPreferences get() = context.getSharedPreferences("com.spectre7.spmp.PREFERENCES", Context.MODE_PRIVATE)
        val theme: Theme get() = context.theme
        val languages: Map<String, Map<String, String>> get() = context.languages

        @JvmStatic
        private var instance: MainActivity? = null

        fun runInMainThread(action: () -> Unit) {
            Handler(Looper.getMainLooper()).post(action)
        }
    }
}