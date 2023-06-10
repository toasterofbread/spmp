@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.beust.klaxon.Klaxon
import com.beust.klaxon.KlaxonException
import com.spectre7.spmp.api.Api
import com.spectre7.spmp.api.YoutubeUITranslation
import com.spectre7.spmp.model.Cache
import com.spectre7.spmp.model.mediaitem.MediaItem
import com.spectre7.spmp.model.Settings
import com.spectre7.spmp.platform.PlatformContext
import com.spectre7.spmp.platform.ProjectPreferences
import com.spectre7.spmp.platform.composable.PlatformAlertDialog
import com.spectre7.spmp.platform.vibrateShort
import com.spectre7.spmp.resources.getString
import com.spectre7.spmp.resources.getStringTODO
import com.spectre7.spmp.resources.initResources
import com.spectre7.spmp.ui.layout.mainpage.PlayerState
import com.spectre7.spmp.ui.layout.mainpage.PlayerStateImpl
import com.spectre7.spmp.ui.layout.mainpage.RootView
import com.spectre7.spmp.ui.theme.ApplicationTheme
import com.spectre7.spmp.ui.theme.Theme
import com.spectre7.utils.*
import com.spectre7.utils.composable.OnChangedEffect
import com.spectre7.utils.composable.ShapedIconButton
import com.spectre7.utils.composable.WidthShrinkText
import java.util.*
import java.util.logging.Logger
import kotlin.concurrent.thread
import kotlin.math.roundToInt

expect fun getPlatformName(): String

val GlobalPlayerState = PlayerStateImpl()
val LocalPlayerState: ProvidableCompositionLocal<PlayerState> = staticCompositionLocalOf { GlobalPlayerState }

object SpMp {
    private val LANGUAGES = listOf("af", "am", "ar", "as", "az", "be", "bg", "bn", "bs", "ca", "cs", "da", "de", "el", "en-GB", "en-IN", "en", "es", "es-419", "es-US", "et", "eu", "fa", "fi", "fil", "fr-CA", "fr", "gl", "gu", "hi", "hr", "hu", "hy", "id", "is", "it", "iw", "ja", "ka", "kk", "km", "kn", "ko", "ky", "lo", "lt", "lv", "mk", "ml", "mn", "mr", "ms", "my", "no", "ne", "nl", "or", "pa", "pl", "pt", "pt-PT", "ro", "ru", "si", "sk", "sl", "sq", "sr-Latn", "sr", "sv", "sw", "ta", "te", "th", "tr", "uk", "ur", "uz", "vi", "zh-CN", "zh-HK", "zh-TW", "zu")

    val Log = Logger.getLogger(SpMp::class.java.name)
    lateinit var context: PlatformContext
    lateinit var error_manager: ErrorManager

    private var _yt_ui_translation: YoutubeUITranslation? = null
    val yt_ui_translation: YoutubeUITranslation get() = _yt_ui_translation!!

    private val prefs_change_listener =
        object : ProjectPreferences.Listener {
            override fun onChanged(prefs: ProjectPreferences, key: String) {
            }
        }

    private val low_memory_listeners: MutableList<() -> Unit> = mutableListOf()

    fun getLanguageCode(index: Int): String = LANGUAGES[index]
    fun getLanguageIndex(language_code: String): Int = LANGUAGES.indexOf(language_code)
    fun getLanguageCount(): Int = LANGUAGES.size

    val ui_language: String get() = getLanguageCode(Settings.get(Settings.KEY_LANG_UI))
    val data_language: String get() = getLanguageCode(Settings.get(Settings.KEY_LANG_DATA))

    fun init(context: PlatformContext) {
        this.context = context

        context.getPrefs().addListener(prefs_change_listener)
        error_manager = ErrorManager(context)

        thread {
            MediaItem.init(Settings.prefs)
        }

        val ui_lang: Int = Settings.get(Settings.KEY_LANG_UI)
        initResources(LANGUAGES.elementAt(ui_lang), context)

        _yt_ui_translation = YoutubeUITranslation(LANGUAGES)
        Cache.init(context)
        Api.initialise()

        GlobalPlayerState.init(context)
    }

    fun release() {
        _yt_ui_translation = null
    }

    fun onStart() {
        GlobalPlayerState.onStart()
    }

    fun onStop() {
        GlobalPlayerState.onStop()
    }

    @Composable
    fun App(open_uri: String? = null) {
        ApplicationTheme(context, getFontFamily(context)) {
            Theme.Update(context, MaterialTheme.colorScheme.primary)

            val player = GlobalPlayerState
            player.initComposable(context)

            LaunchedEffect(open_uri) {
                if (open_uri != null) {
                    player.openUri(open_uri).onFailure {
                        context.sendNotification(it)
                    }
                }
            }

            Surface(modifier = Modifier.fillMaxSize()) {
                if (player.service_connected) {
                    RootView(player)
                }
                error_manager.Indicator(Theme.current.accent_provider)
            }
        }
    }

    fun addLowMemoryListener(listener: () -> Unit) {
        low_memory_listeners.add(listener)
    }
    fun removeLowMemoryListener(listener: () -> Unit) {
        low_memory_listeners.remove(listener)
    }

    fun onLowMemory() {
        low_memory_listeners.forEach { it.invoke() }
    }

    private fun getFontFamily(context: PlatformContext): FontFamily {
        val locale = ui_language
        val font_dirs = context.listResourceFiles("")!!.filter { it.length > 4 && it.startsWith("font") }

        var font_dir: String? = font_dirs.firstOrNull { it.endsWith("-$locale") }
        if (font_dir == null) {
            val locale_split = locale.indexOf('-')
            if (locale_split > 0) {
                val sublocale = locale.take(locale_split)
                font_dir = font_dirs.firstOrNull { it.endsWith("-$sublocale") }
            }
        }

        val font_name = font_dir ?: "font"
        return FontFamily(context.loadFontFromFile("$font_name/regular.ttf"))
    }

    val app_name: String get() = getString("app_name")
}

private data class YoutubeiErrorResponse(val error: Error) {
    data class Error(val message: String)
    fun getMessage(): String = error.message
}

class ErrorManager(private val context: PlatformContext) {
    private val SIDE_PADDING = 10.dp
    private val INDICATOR_SIZE = 50.dp

    private val errors = mutableStateMapOf<String, Throwable>()

    fun onError(key: String, error: Throwable) {
        println("Error reported with key '$key': $error")
        if (error is RuntimeException && error.message != null) {
            try {
                val error_response: YoutubeiErrorResponse? = Klaxon().parse(error.message!!)
                error_response?.apply {
                    context.sendToast(getMessage(), long = true)
                    return
                }
            }
            catch (_: KlaxonException) {}
        }

        errors[key] = Exception(error)
    }

    @OptIn(ExperimentalMaterial3Api::class)
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
                        onLongClick = {
                            dismiss = true
                            SpMp.context.vibrateShort()
                        },
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

    @Composable
    fun InfoPopup(dismiss: () -> Unit, close: () -> Unit) {
        PlatformAlertDialog(
            close,
            modifier = Modifier.fillMaxSize(),
            confirmButton = {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    FilledIconButton(close) {
                        Icon(Icons.Filled.Close, null)
                    }

                    FilledTonalButton(dismiss) {
                        Text(getStringTODO("Dismiss"))
                    }
                }
            },
            title = {
                WidthShrinkText(getStringTODO("{errors} error(s) occurred").replace("{errors}", errors.size.toString()))
            },
            text = {
                var expanded_error by remember { mutableStateOf(-1) }

                LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    items(errors.size, { errors.values.elementAt(it) }) { index ->
                        val error = errors.entries.elementAt(index)
                        ErrorItem(error.key, error.value, index, index == expanded_error) {
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
    private fun ErrorItem(key: String, error: Throwable, index: Int, expanded: Boolean, onClick: () -> Unit) {
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
            Column {
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

                    Text(error.message ?: getStringTODO("No message"))
                }

                Text("Key: $key")
            }

            AnimatedVisibility(expanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column {
                    Text(error.stackTraceToString(), softWrap = false)

                    Row(horizontalArrangement = Arrangement.End) {
                        context.CopyShareButtons(getStringTODO("error")) { error.stackTraceToString() }
                        FilledTonalButton(onClick = { throw error }) {
                            Text(getStringTODO("Throw"))
                        }
                    }
                }
            }
        }
    }
}
