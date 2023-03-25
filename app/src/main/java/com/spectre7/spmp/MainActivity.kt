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
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.swipeable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import com.spectre7.spmp.api.DataApi
import com.spectre7.spmp.model.Cache
import com.spectre7.spmp.model.MediaItem
import com.spectre7.spmp.model.Settings
import com.spectre7.spmp.ui.layout.PlayerView
import com.spectre7.spmp.ui.theme.ApplicationTheme
import com.spectre7.spmp.ui.theme.Theme
import com.spectre7.utils.*
import net.openid.appauth.*
import java.util.*
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {

    lateinit var languages: Map<String, Map<String, String>>
    private val prefs_change_listener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key: String ->
            if (key == Settings.KEY_LANG_UI.name) {
                updateLanguage(Settings.get(Settings.KEY_LANG_UI))
            }
        }

    private fun updateLanguage(lang: Int) {
        // TODO
        val locale = Locale(MainActivity.languages.keys.elementAt(lang))
        val conf = resources.configuration
        conf.setLocale(locale)
        Locale.setDefault(locale)
        conf.setLayoutDirection(locale)
        resources.updateConfiguration(conf, resources.displayMetrics)
    }

//    private lateinit var auth_service: AuthorizationService
//    private lateinit var auth_state: AuthState
//    private lateinit var auth_activity_launcher: ActivityResultLauncher<(AuthorizationException?) -> Unit>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        instance = this

        languages = loadLanguages()
        Settings.prefs.registerOnSharedPreferenceChangeListener(prefs_change_listener)
        updateLanguage(Settings.get(Settings.KEY_LANG_UI))

        Cache.init(this)
        DataApi.initialise()
        MediaItem.init(Companion.getSharedPreferences())

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

                    error_manager.Indicator(Theme.current.accent_provider)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        PlayerServiceHost.release()
        Settings.prefs.unregisterOnSharedPreferenceChangeListener(prefs_change_listener)
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

@Preview
@Composable
fun ErrorManagerPreview() {
    val manager = remember { ErrorManager().apply {
        onError("Key1", RuntimeException("Exception one message"))
        onError("Key2", RuntimeException("Exception two message"))
        onError("Key3", RuntimeException("Exception three message"))
    } }
    manager.Indicator { Color.Red }
}

class ErrorManager {
    val SIDE_PADDING = 10.dp
    val INDICATOR_SIZE = 50.dp

    private val errors = mutableStateMapOf<String, Throwable>()

    fun onError(key: String, error: Throwable) {
        errors[key] = Exception(error)
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    fun Indicator(colour: () -> Color) {
        val swipe_state = rememberSwipeableState(0)
        val dismiss_offset = with(LocalDensity.current) { 
            (-SIDE_PADDING - INDICATOR_SIZE).toPx() - 1
        }
        val swipe_anchors = mapOf(dismiss_offset to 0, 0f to 1)

        LaunchedEffect(errors.isEmpty()) {
            swipe_state.animateTo(errors.isNotEmpty().toInt())
        }

        var dismiss by remember { mutableStateOf(false) }
        OnChangedEffect(dismiss) {
            if (dismiss) {
                swipe_state.animateTo(0)
                dismiss = false
            }
        }

        OnChangedEffect(swipe_state.currentValue) {
            if (swipe_state.currentValue == 0) {
                errors.clear()
            }
        }

        var show_info: Boolean by remember { mutableStateOf(false) }
        if (show_info) {
            InfoPopup({
                dismiss = true
                show_info = false
            }) {
                show_info = false
            }
        }

        Box(
            Modifier
                .fillMaxSize()
                .padding(start = SIDE_PADDING),
            contentAlignment = Alignment.BottomStart
        ) {
            Box(Modifier.swipeable(
                state = swipe_state,
                anchors = swipe_anchors,
                thresholds = { _, _ -> FractionalThreshold(0.3f) },
                orientation = Orientation.Horizontal
            )) {
                if (swipe_state.targetValue == 1) {
                    ShapedIconButton(
                        { show_info = !show_info },
                        Modifier
                            .swipeable(
                                state = swipe_state,
                                anchors = swipe_anchors,
                                thresholds = { _, _ -> FractionalThreshold(0.3f) },
                                orientation = Orientation.Horizontal
                            )
                            .size(INDICATOR_SIZE)
                            .offset { IntOffset(swipe_state.offset.value.roundToInt(), 0) },
                        CircleShape,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = colour(),
                            contentColor = colour().getContrasted()
                        )
                    ) {
                        Icon(Icons.Filled.WifiOff, null)
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun InfoPopup(dismiss: () -> Unit, close: () -> Unit) {
        AlertDialog(
            close,
            confirmButton = {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    FilledIconButton(close) {
                        Icon(Icons.Filled.Close, null)
                    }

                    FilledTonalButton(dismiss) {
                        Text(getString("Dismiss"))
                    }
                }
            },
            title = {
                WidthShrinkText(getString("{errors} error(s) occurred").replace("{errors}", errors.size.toString()))
            },
            text = {
                var expanded_error by remember { mutableStateOf(-1) }

                LazyColumn(Modifier
                    .fillMaxWidth()
                    .height(300.dp)) {
                    items(errors.size, { errors.values.elementAt(it) }) { index ->
                        val error = errors.values.elementAt(index)
                        ErrorItem(error, index, index == expanded_error) {
                            if (expanded_error == index) {
                                expanded_error = -1
                            }
                            else {
                                expanded_error = index
                            }
                        }
                    }
                }
            }
        )
    }

    @Composable
    private fun ErrorItem(error: Throwable, index: Int, expanded: Boolean, onClick: () -> Unit) {
        Column(Modifier
            .animateContentSize()
            .clickable(
                remember { MutableInteractionSource() },
                null,
                onClick = onClick
            )
            .horizontalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Box(Modifier
                    .size(20.dp)
                    .background(Color.Red, RoundedCornerShape(16.dp))) {
                    Text(index.toString(), Modifier.align(Alignment.Center))
                }

                Text(error.message ?: getString("No message"))
            }

            AnimatedVisibility(expanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column {
                    Text(error.stackTraceToString(), softWrap = false)

                    Row(horizontalArrangement = Arrangement.End) {
                        CopyShareButtons(getString("error")) { error.stackTraceToString() }
                        FilledTonalButton(onClick = { throw error }) {
                            Text(getString("Throw"))
                        }
                    }
                }
            }
        }
    }
}
