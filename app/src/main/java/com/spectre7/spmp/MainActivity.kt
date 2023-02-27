package com.spectre7.spmp

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Resources
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import com.spectre7.spmp.api.DataApi
import com.spectre7.spmp.model.Cache
import com.spectre7.spmp.model.Settings
import com.spectre7.spmp.model.Song
import com.spectre7.spmp.ui.layout.PlayerView
import com.spectre7.spmp.ui.theme.ApplicationTheme
import com.spectre7.spmp.ui.theme.Theme
import com.spectre7.utils.*
import net.openid.appauth.*
import java.util.*

class MainActivity : ComponentActivity() {

    lateinit var languages: Map<String, Map<String, String>>

//    private lateinit var auth_service: AuthorizationService
//    private lateinit var auth_state: AuthState
//    private lateinit var auth_activity_launcher: ActivityResultLauncher<(AuthorizationException?) -> Unit>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        instance = this

        languages = loadLanguages()
        fun updateLanguage(lang: Int) {
            // TODO
            val locale = Locale(MainActivity.languages.keys.elementAt(lang))
            val conf = resources.configuration
            conf.setLocale(locale)
            Locale.setDefault(locale)
            conf.setLayoutDirection(locale)
            resources.updateConfiguration(conf, resources.displayMetrics)
        }
        Settings.prefs.registerOnSharedPreferenceChangeListener { _, key: String ->
            if (key == Settings.KEY_LANG_UI.name) {
                updateLanguage(Settings.get(Settings.KEY_LANG_UI))
            }
        }
        updateLanguage(Settings.get(Settings.KEY_LANG_UI))

        Cache.init(this)
        DataApi.initialise()
        Song.init(Companion.getSharedPreferences())

        Thread.setDefaultUncaughtExceptionHandler { _: Thread, error: Throwable ->
            error.printStackTrace()

            context.startActivity(Intent(context, ErrorReportActivity::class.java).apply {
                putExtra("message", error.message)
                putExtra("stack_trace", error.stackTraceToString())
            })
        }

//        auth_state = loadAuthState()
//        auth_service = AuthorizationService(this)
//        auth_activity_launcher = createOauthActivityLauncher()

        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        val service_host = PlayerServiceHost.instance ?: PlayerServiceHost()
        var service_started = false

        setContent {
            ApplicationTheme(getFontFamily()) {
                Theme.Update(this, MaterialTheme.colorScheme.primary)

                Surface(modifier = Modifier.fillMaxSize()) {
                    if (PlayerServiceHost.service_connected) {
                        PlayerView()
                    }
                    else if (!service_started) {
                        service_started = true
                        service_host.startService({ service_started = false })
                    }

                    error_manager.Indicator({ Color.Red })
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        PlayerServiceHost.release()
//        auth_service.dispose()
        instance = null
    }

//    private fun loadAuthState(): AuthState {
//        val state_data = getSharedPreferences("auth", MODE_PRIVATE).getString("state", null)
//        if (state_data != null) {
//            return AuthState.jsonDeserialize(state_data)
//        }
//        else {
//            return AuthState(AuthorizationServiceConfiguration(
//                Uri.parse("https://accounts.google.com/o/oauth2/v2/auth"),
//                Uri.parse("https://www.googleapis.com/oauth2/v4/token")
//            ))
//        }
//    }

//    private fun saveAuthState(auth_state: AuthState) {
//        getSharedPreferences("auth", MODE_PRIVATE).edit()
//            .putString("state", auth_state.jsonSerializeString())
//            .apply()
//    }

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
        data.close()
        return ret
    }

    @OptIn(ExperimentalTextApi::class)
    private fun getFontFamily(): FontFamily {
        val locale = languages.keys.elementAt(Settings.get(Settings.KEY_LANG_UI))
        val font_dirs = resources.assets.list("")!!.filter { it.length > 4 && it.startsWith("font") }

        var font_dir: String? = font_dirs.firstOrNull { it.endsWith("-$locale") }
        if (font_dir == null) {
            val locale_split = locale.indexOf('-')
            if (locale_split > 0) {
                val sublocale = locale.take(locale_split)
                font_dir = font_dirs.firstOrNull { it.endsWith("-$sublocale") }
            }
        }

        return FontFamily(Font("${font_dir ?: "font"}/regular.ttf", resources.assets))
    }

    companion object {
        @JvmStatic
        private var instance: MainActivity? = null

        val context: MainActivity get() = instance!!
        val resources: Resources get() = context.resources
        val languages: Map<String, Map<String, String>> get() = context.languages
        val error_manager = ErrorManager()
        private var prefs: SharedPreferences? = null

//        val auth_state: AuthState get() = context.auth_state
//        val auth_service: AuthorizationService get() = context.auth_service
//        fun saveAuthState() {
//            context.saveAuthState(auth_state)
//        }
//        fun startAuthLogin(onFinished: (exception: AuthorizationException?) -> Unit) {
//            context.auth_activity_launcher.launch(onFinished)
//        }

        val ui_language: String get() = languages.keys.elementAt(Settings.get(Settings.KEY_LANG_UI))
        val data_language: String get() = languages.keys.elementAt(Settings.get(Settings.KEY_LANG_DATA))

        fun getSharedPreferences(context: Context = instance!!): SharedPreferences {
            if (prefs == null) {
                prefs = context.getSharedPreferences("com.spectre7.spmp.PREFERENCES", Context.MODE_PRIVATE)
            }
            return prefs!!
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
    private val current_errors = mutableStateMapOf<String, Pair<Throwable, ((result: (success: Boolean, message: String?) -> Unit) -> Unit)?>>()

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun Indicator(colour: () -> Color) {
        var showing_error: Int by remember { mutableStateOf(-1) }

        if (showing_error >= 0) {
            ErrorInfo(
                showing_error,
                {
                    current_errors.remove(current_errors.entries.elementAt(showing_error).key)
                    showing_error = -1
                }
            ) {
                showing_error = -1
            }
        }

        AnimatedVisibility(
            current_errors.isNotEmpty(),
            enter = slideInVertically(),
            exit = slideOutVertically()
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(top = getStatusBarHeight()),
                contentAlignment = Alignment.TopCenter
            ) {
                Button(
                    {},
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colour(),
                        contentColor = colour().getContrasted()
                    )
                ) {
                    Row(
                        Modifier.padding(2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Filled.WifiTetheringError, null)
                        Text("Network error")

                        Spacer(Modifier.requiredWidth(10.dp))

                        Icon(Icons.Filled.Info, null, Modifier.clickable { throw current_errors.values.first().first })
                        Icon(Icons.Filled.Close, null, Modifier.clickable { vibrateShort(); current_errors.clear() })
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ErrorInfo(i: Int, resolve: () -> Unit, close: () -> Unit) {
        val error = remember(i) { current_errors.entries.elementAt(i) }

        AlertDialog(
            close,
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilledIconButton(close) {
                        Icon(Icons.Filled.Close, null)
                    }
                    Spacer(Modifier
                        .weight(1f)
                        .fillMaxWidth())
                    FilledTonalButton(onClick = {
                        throw error.value.first
                    }) {
                        Text("Throw")
                    }

                    if (error.value.second != null) {
                        FilledTonalButton({
                            error.value.second!!.invoke { success: Boolean, message: String? ->
                                if (success) {
                                    sendToast("Retry succeeded")
                                    resolve()
                                }
                                else {
                                    sendToast("Retry failed")
                                }
                            }
                        }) {
                            Text("Retry")
                        }
                    }
                }
            },
            title = { error.value.first.javaClass.typeName },
            text = {
                @Composable
                fun InfoValue(name: String, value: String) {
                    Column(Modifier.fillMaxWidth()) {
                        Text(name, style = MaterialTheme.typography.labelLarge)
                        Box(Modifier.fillMaxWidth()) {
                            Marquee(false) {
                                Text(value, softWrap = false)
                            }
                        }
                    }
                }

                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.Center) {
                    InfoValue("Key", error.key)
                    InfoValue("Message", error.value.first.message.toString())
                    InfoValue("Stacktrace", error.value.first.stackTraceToString())

                    Spacer(Modifier.requiredHeight(40.dp))

                    Row(horizontalArrangement = Arrangement.End) {
                        val clipboard = LocalClipboardManager.current
                        IconButton({
                            clipboard.setText(AnnotatedString(error.value.first.stackTraceToString()))
                            sendToast("Copied error to clipboard")
                        }) {
                            Icon(Icons.Filled.ContentCopy, null, Modifier.size(20.dp))
                        }

                        val share_intent = Intent.createChooser(Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, error.value.first.stackTraceToString())
                            type = "text/plain"
                        }, null)
                        IconButton({
                            MainActivity.context.startActivity(share_intent)
                        }) {
                            Icon(Icons.Filled.Share, null, Modifier.size(20.dp))
                        }
                    }
                }
            }
        )
    }

    fun onError(key: String, e: Throwable, retry: ((result: (success: Boolean, message: String?) -> Unit) -> Unit)?) {
        current_errors[key] = Pair(Exception(e), retry)
    }
}
