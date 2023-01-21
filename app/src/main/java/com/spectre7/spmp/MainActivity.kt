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
import androidx.lifecycle.Lifecycle
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.google.android.exoplayer2.database.StandaloneDatabaseProvider
import com.spectre7.spmp.model.Settings
import com.spectre7.spmp.model.Cache
import com.spectre7.spmp.ui.layout.PlayerView
import com.spectre7.spmp.ui.theme.MyApplicationTheme
import com.spectre7.utils.Theme
import java.util.*

class MainActivity : ComponentActivity() {

    lateinit var prefs: SharedPreferences
    lateinit var theme: Theme
    lateinit var languages: Map<String, Map<String, String>>
    lateinit var database: StandaloneDatabaseProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this

        prefs = getSharedPreferences(this)

        Cache.init(this)
        languages = loadLanguages()

        fun updateLanguage(lang: Int) {
            // TODO
            val myLocale = Locale(MainActivity.languages.keys.elementAt(lang))
            val conf = resources.configuration
            conf.setLocale(myLocale)
//            Locale.setDefault(myLocale)
            conf.setLayoutDirection(myLocale)
            resources.updateConfiguration(conf, resources.displayMetrics)
        }

        Settings.prefs.registerOnSharedPreferenceChangeListener { _, key: String ->
            if (key == Settings.KEY_LANG_UI.name) {
                updateLanguage(Settings.get(Settings.KEY_LANG_UI))
            }
        }

        updateLanguage(Settings.get(Settings.KEY_LANG_UI))

        database = StandaloneDatabaseProvider(this)

        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        PlayerServiceHost()

        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        setContent {
            MyApplicationTheme {
                theme = Theme.default()

                Surface(modifier = Modifier.fillMaxSize()) {
                    if (PlayerServiceHost.service_connected) {
                        PlayerView()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        PlayerServiceHost.release()
        instance = null
    }

    private fun loadLanguages(): MutableMap<String, Map<String, String>> {
        val data = resources.assets.open("languages.json").bufferedReader()
        val ret = mutableMapOf<String, Map<String, String>>()
        for (item in Klaxon().parseJsonObject(data).entries) {
            val map = mutableMapOf<String, String>()
            for (subitem in (item.value as JsonObject).entries) {
                map[subitem.key] = subitem.value.toString()
            }
            ret[item.key] = map
        }
        return ret
    }

    companion object {
        @JvmStatic
        private var instance: MainActivity? = null

        val context: MainActivity get() = instance!!
        val resources: Resources get() = context.resources
        val prefs: SharedPreferences get() = instance!!.prefs
        val theme: Theme get() = context.theme
        val languages: Map<String, Map<String, String>> get() = context.languages
        val network = NetworkConnectivityManager()
        val database get() = context.database

        val ui_language: String get() = languages.keys.elementAt(Settings.get(Settings.KEY_LANG_UI))
        val data_language: String get() = languages.keys.elementAt(Settings.get(Settings.KEY_LANG_DATA))

        fun runInMainThread(action: () -> Unit) {
            Handler(Looper.getMainLooper()).post(action)
        }

        fun getSharedPreferences(context: Context): SharedPreferences {
            return context.getSharedPreferences("com.spectre7.spmp.PREFERENCES", Context.MODE_PRIVATE)
        }

        fun isInForeground(): Boolean {
            if (instance == null) {
                return false
            }
            return instance!!.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        }
    }
}

class NetworkConnectivityManager {
    private val retry_callbacks = mutableListOf<() -> Unit>()
    private val error_callbacks = mutableListOf<() -> Unit>()

    var error by mutableStateOf<Exception?>(null)
    val connected: Boolean get() = error == null

    fun onError(e: Exception) {
        error = e
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
