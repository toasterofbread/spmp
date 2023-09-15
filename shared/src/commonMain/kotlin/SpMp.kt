@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.toasterofbread.spmp.model.FontMode
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.platform.PlatformContext
import com.toasterofbread.spmp.platform.PlatformPreferences
import com.toasterofbread.spmp.platform.composable.PlatformAlertDialog
import com.toasterofbread.spmp.platform.vibrateShort
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.resources.getStringTODO
import com.toasterofbread.spmp.resources.initResources
import com.toasterofbread.spmp.resources.uilocalisation.LocalisedYoutubeString
import com.toasterofbread.spmp.resources.uilocalisation.UnlocalisedStringCollector
import com.toasterofbread.spmp.resources.uilocalisation.YoutubeUILocalisation
import com.toasterofbread.spmp.resources.uilocalisation.localised.UILanguages
import com.toasterofbread.spmp.ui.layout.mainpage.PlayerState
import com.toasterofbread.spmp.ui.layout.mainpage.PlayerStateImpl
import com.toasterofbread.spmp.ui.layout.mainpage.RootView
import com.toasterofbread.spmp.ui.layout.mainpage.LoadingSplashView
import com.toasterofbread.spmp.ui.theme.ApplicationTheme
import com.toasterofbread.spmp.ui.theme.Theme
import com.toasterofbread.spmp.youtubeapi.fromJson
import com.toasterofbread.utils.*
import com.toasterofbread.utils.common.getContrasted
import com.toasterofbread.utils.common.toInt
import com.toasterofbread.utils.composable.OnChangedEffect
import com.toasterofbread.utils.composable.ShapedIconButton
import com.toasterofbread.utils.composable.WidthShrinkText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.CancellationException
import java.util.logging.Logger
import kotlin.math.roundToInt

expect fun getPlatformName(): String

val LocalPlayerState: ProvidableCompositionLocal<PlayerState> = staticCompositionLocalOf { SpMp.player_state }

object SpMp {
    val Log: Logger = Logger.getLogger(SpMp::class.java.name)
    lateinit var context: PlatformContext
    lateinit var error_manager: ErrorManager
    lateinit var player_state: PlayerStateImpl

    private var _yt_ui_localisation: YoutubeUILocalisation? = null
    val yt_ui_localisation: YoutubeUILocalisation get() = _yt_ui_localisation!!

    private val prefs_change_listener =
        object : PlatformPreferences.Listener {
            override fun onChanged(prefs: PlatformPreferences, key: String) {
            }
        }

    private val low_memory_listeners: MutableList<() -> Unit> = mutableListOf()
    private val coroutine_scope = CoroutineScope(Dispatchers.Main)

    fun getUiLanguage(context: PlatformContext = SpMp.context): String =
        Settings.KEY_LANG_UI.get<String>(context.getPrefs()).ifEmpty { Locale.getDefault().toLanguageTag() }
    fun getDataLanguage(context: PlatformContext = SpMp.context): String =
        Settings.KEY_LANG_DATA.get<String>(context.getPrefs()).ifEmpty { Locale.getDefault().toLanguageTag() }

    val ui_language: String get() = getUiLanguage()
    val data_language: String get() = getDataLanguage()

    fun init(context: PlatformContext) {
        this.context = context

        coroutine_scope.launch {
            context.ytapi.init()
        }

        player_state = PlayerStateImpl(context)

        context.getPrefs().addListener(prefs_change_listener)
        error_manager = ErrorManager(context)

        initResources(ui_language, context)
        _yt_ui_localisation = YoutubeUILocalisation(UILanguages())
    }

    fun release() {
        _yt_ui_localisation = null
        coroutine_scope.cancel()
    }

    fun onStart() {
        player_state.onStart()
    }

    fun onStop() {
        player_state.onStop()
    }

    @Composable
    fun App(open_uri: String? = null) {
        Theme.ApplicationTheme(context, getFontFamily(context) ?: FontFamily.Default) {
            Theme.Update(context)

            LaunchedEffect(open_uri) {
                if (open_uri != null) {
                    player_state.openUri(open_uri).onFailure {
                        context.sendNotification(it)
                    }
                }
            }

            Surface(modifier = Modifier.fillMaxSize()) {
                CompositionLocalProvider(LocalPlayerState provides player_state) {
                    if (player_state.service_connected) {
                        RootView(player_state)
                    }
                    LoadingSplashView(player_state.service_connected, Modifier.fillMaxSize())
                }
                error_manager.Indicator(Theme.accent_provider)
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

    fun reportActionError(exception: Throwable?) {
        // TODO add option to disable
        context.sendToast(exception.toString())
    }

    @Composable
    fun ErrorDisplay(exception: Throwable?, modifier: Modifier = Modifier) {
        // TODO
        Text(exception.toString())
    }

    private fun getFontFamily(context: PlatformContext): FontFamily? {
        val font_mode: FontMode = Settings.KEY_FONT.getEnum(context.getPrefs())
        val font_path: String = font_mode.getFontFilePath(ui_language) ?: return null
        return FontFamily(context.loadFontFromFile("font/$font_path"))
    }

    val app_name: String get() = getString("app_name")

    val unlocalised_string_collector: UnlocalisedStringCollector? = UnlocalisedStringCollector()

    fun onUnlocalisedStringFound(string: UnlocalisedStringCollector.UnlocalisedString) {
        if (unlocalised_string_collector?.add(string) == true) {
            Log.warning("String key '${string.key}' of type ${string.type} has not been localised (source lang=${string.source_language})")
        }
    }

    fun onUnlocalisedStringFound(type: String, key: String?, source_language: String) =
        onUnlocalisedStringFound(UnlocalisedStringCollector.UnlocalisedString(type, key, source_language))
    
    fun onUnlocalisedStringFound(string: LocalisedYoutubeString) =
        onUnlocalisedStringFound(UnlocalisedStringCollector.UnlocalisedString.fromLocalised(string))
}

private data class YoutubeiErrorResponse(val error: Error) {
    data class Error(val message: String)
    fun getMessage(): String = error.message
}

// TODO Remove (all errors should be handled by UI)
class ErrorManager(private val context: PlatformContext) {
    private val SIDE_PADDING = 10.dp
    private val INDICATOR_SIZE = 50.dp

    private val errors = mutableStateMapOf<String, Throwable>()

    @Synchronized
    fun onError(key: String, error: Throwable) {
        if (error is CancellationException) {
            println("Skipping cancellation error reported with key '$key': $error")
            return
        }

        println("Error reported with key '$key': $error")
        if (error is RuntimeException && error.message != null) {
            try {
                val error_response: YoutubeiErrorResponse? = Gson().fromJson(error.message!!)
                error_response?.apply {
                    context.sendToast(getMessage(), long = true)
                    return
                }
            }
            catch (_: JsonParseException) {}
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
                            context.vibrateShort()
                        },
                        colours = IconButtonDefaults.iconButtonColors(
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
