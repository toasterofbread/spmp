package com.spectre7.spmp

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.Lifecycle
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import com.google.android.exoplayer2.database.StandaloneDatabaseProvider
import com.spectre7.spmp.api.DataApi
import com.spectre7.spmp.api.subscribeOrUnsubscribeArtist
import com.spectre7.spmp.model.Artist
import com.spectre7.spmp.model.Cache
import com.spectre7.spmp.model.Settings
import com.spectre7.spmp.model.Song
import com.spectre7.spmp.ui.component.PillMenu
import com.spectre7.spmp.ui.layout.PlayerView
import com.spectre7.spmp.ui.theme.MyApplicationTheme
import com.spectre7.utils.NoRipple
import com.spectre7.utils.Theme
import net.openid.appauth.*
import java.util.*
import kotlin.concurrent.thread


class MainActivity : ComponentActivity() {

    lateinit var prefs: SharedPreferences
    lateinit var theme: Theme
    lateinit var languages: Map<String, Map<String, String>>
    lateinit var database: StandaloneDatabaseProvider

    private lateinit var auth_service: AuthorizationService
    private lateinit var auth_state: AuthState
    private lateinit var auth_activity_launcher: ActivityResultLauncher<(AuthorizationException?) -> Unit>

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

        auth_state = loadAuthState()
        auth_service = AuthorizationService(this)
        auth_activity_launcher = createOauthActivityLauncher()

        database = StandaloneDatabaseProvider(this)

        PlayerServiceHost()
        DataApi.initialise()

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
        auth_service.dispose()
        instance = null
    }

    private fun loadAuthState(): AuthState {
        val state_data = getSharedPreferences("auth", MODE_PRIVATE).getString("state", null)
        if (state_data != null) {
            return AuthState.jsonDeserialize(state_data)
        }
        else {
            return AuthState(AuthorizationServiceConfiguration(
                Uri.parse("https://accounts.google.com/o/oauth2/v2/auth"),
                Uri.parse("https://www.googleapis.com/oauth2/v4/token")
            ))
        }
    }

    private fun saveAuthState(auth_state: AuthState) {
        getSharedPreferences("auth", MODE_PRIVATE).edit()
            .putString("state", auth_state.jsonSerializeString())
            .apply()
    }

    private fun createOauthActivityLauncher(): ActivityResultLauncher<(AuthorizationException?) -> Unit> {
        return registerForActivityResult(object : ActivityResultContract<(AuthorizationException?) -> Unit, Unit>() {
            private lateinit var onFinished: (AuthorizationException?) -> Unit
            
            override fun createIntent(context: Context, onFinished: (AuthorizationException?) -> Unit): Intent {
                this.onFinished = onFinished
                
                val auth_request_builder = AuthorizationRequest.Builder(
                    auth_state.authorizationServiceConfiguration!!,
                    com.spectre7.utils.getString(R.string.oauth_client_id),
                    ResponseTypeValues.CODE,
                    Uri.parse("${context.packageName}:/oauth2redirect")
                )

                val auth_request = auth_request_builder
                    .setScope("https://www.googleapis.com/auth/youtube")
                    .setUiLocales(ui_language)
                    .build()

                return auth_service.getAuthorizationRequestIntent(auth_request)
            }

            override fun parseResult(resultCode: Int, intent: Intent?) {
                val exception = AuthorizationException.fromIntent(intent!!)
                if (exception != null) {
                    onFinished(exception)
                    return
                }

                auth_state.update(AuthorizationResponse.fromIntent(intent), null)
                onFinished(null)
            }
        }, {})
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
                _background_colour = colour,
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
