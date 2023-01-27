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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.Lifecycle
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.google.android.exoplayer2.database.StandaloneDatabaseProvider
import com.spectre7.spmp.model.Settings
import com.spectre7.spmp.model.Cache
import com.spectre7.spmp.ui.component.PillMenu
import com.spectre7.spmp.ui.layout.PlayerView
import com.spectre7.spmp.ui.theme.MyApplicationTheme
import com.spectre7.utils.NoRipple
import com.spectre7.utils.Theme
import com.spectre7.utils.getContrasted
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
        Song.init(prefs)

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

                    error_manager.Indicator(Color.Red)
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
        val error_manager = ErrorManager()
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

class ErrorManager {
    private val current_errors = mutableStateMapOf<Throwable, (resolve: () -> Unit) -> Unit>()

    @Composable
    fun Indicator(colour: Color) {
        AnimatedVisibility(current_errors.isNotEmpty()) {
            remember{ PillMenu() }.PillMenu(
                action_count = current_errors.size - 1,
                getAction = { i, _ ->
                    Text(current_errors.keys.elementAt(i).toString())
                },
                toggleButton = {
                    NoRipple {
                        IconButton({
                            if (current_errors.size == 1) { println("what") } else is_open = !is_open
                        }, it) {
                            Icon(Icons.Filled.Error, null)
                        }
                    }
                },
                expand_state = remember { mutableStateOf(false) },
                background_colour = colour,
                content_colour = colour.getContrasted(),
                top = false,
                left = true,
                vertical = true
            )
        }
    }

    fun onError(e: Throwable, retry: (resolve: () -> Unit) -> Unit) {
        current_errors[e] = retry
    }
}
