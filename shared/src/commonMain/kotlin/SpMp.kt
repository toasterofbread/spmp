@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import com.toasterofbread.composekit.platform.Platform
import com.toasterofbread.composekit.platform.PlatformPreferences
import com.toasterofbread.composekit.platform.PlatformFile
import com.toasterofbread.composekit.utils.common.thenIf
import com.toasterofbread.spmp.ProjectBuildConfig
import com.toasterofbread.spmp.model.settings.category.FontMode
import com.toasterofbread.spmp.model.settings.category.SystemSettings
import com.toasterofbread.spmp.model.settings.getEnum
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.getUiLanguage
import com.toasterofbread.spmp.platform.playerservice.getServerExecutableFilename
import com.toasterofbread.spmp.resources.getStringOrNull
import com.toasterofbread.spmp.resources.initResources
import com.toasterofbread.spmp.resources.uilocalisation.LocalisedString
import com.toasterofbread.spmp.resources.uilocalisation.UnlocalisedStringCollector
import com.toasterofbread.spmp.resources.uilocalisation.YoutubeUILocalisation
import com.toasterofbread.spmp.resources.uilocalisation.localised.UILanguages
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.*
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingExpansionState
import com.toasterofbread.spmp.ui.theme.ApplicationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import java.util.logging.Logger

val LocalPlayerState: ProvidableCompositionLocal<PlayerState> = staticCompositionLocalOf { SpMp.player_state }
object LocalNowPlayingExpansion {
    val current: NowPlayingExpansionState
        @Composable get() = LocalPlayerState.current.expansion
}

object SpMp {
    fun isDebugBuild(): Boolean = ProjectBuildConfig.IS_DEBUG
    val Log: Logger = Logger.getLogger(SpMp::class.java.name)

    private lateinit var context: AppContext

    var _player_state: PlayerStateImpl? = null
        private set
    val player_state: PlayerStateImpl get() = _player_state!!

    val prefs: PlatformPreferences get() = context.getPrefs()

    private var _yt_ui_localisation: YoutubeUILocalisation? = null
    val yt_ui_localisation: YoutubeUILocalisation get() = _yt_ui_localisation!!

    private val low_memory_listeners: MutableList<() -> Unit> = mutableListOf()
    private val coroutine_scope = CoroutineScope(Dispatchers.Main)

    fun init(context: AppContext) {
        this.context = context

        coroutine_scope.launch {
            context.ytapi.init()
        }

        initResources(context.getUiLanguage(), context)
        _yt_ui_localisation = YoutubeUILocalisation(UILanguages)
    }

    fun release() {
        _yt_ui_localisation = null
        coroutine_scope.cancel()
        _player_state?.release()
    }

    fun onStart() {
    }

    fun onStop() {
        _player_state?.onStop()
    }

    @Composable
    fun App(
        arguments: ProgramArguments,
        modifier: Modifier = Modifier,
        open_uri: String? = null
    ) {
        context.theme.Update()

        context.theme.ApplicationTheme(context, getFontFamily(context) ?: FontFamily.Default) {
            val player_coroutine_scope: CoroutineScope = rememberCoroutineScope()

            var player_created: Boolean by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                _player_state = PlayerStateImpl(context, player_coroutine_scope)
                _player_state?.onStart()
                player_created = true
            }

            LaunchedEffect(open_uri) {
                if (open_uri != null) {
                    player_state.openUri(open_uri).onFailure {
                        context.sendNotification(it)
                    }
                }
            }

            Surface(modifier = modifier.fillMaxSize()) {
                if (player_created) {
                    val ui_scale: Float by SystemSettings.Key.UI_SCALE.rememberMutableState()

                    CompositionLocalProvider(
                        LocalPlayerState provides player_state,
                        LocalDensity provides Density(LocalDensity.current.density * ui_scale, 1f)
                    ) {
                        val splash_mode: SplashMode? = when (Platform.current) {
                            Platform.ANDROID -> null
                            Platform.DESKTOP -> if (!player_state.service_connected) SplashMode.SPLASH else null
                        }

                        LoadingSplashView(
                            splash_mode,
                            player_state.service_loading_message,
                            arguments,
                            Modifier
                                .fillMaxSize()
                                .thenIf(splash_mode != null) {
                                    pointerInput(Unit) {}
                                }
                        )

                        if (splash_mode == null) {
                            RootView(player_state)
                        }
                    }
                }
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

    private fun getFontFamily(context: AppContext): FontFamily? {
        val font_mode: FontMode = SystemSettings.Key.FONT.getEnum(context.getPrefs())
        val font_path: String = font_mode.getFontFilePath(context.getUiLanguage()) ?: return null
        return FontFamily(context.loadFontFromFile("font/$font_path"))
    }

    val app_name: String get() = getStringOrNull("app_name") ?: "SpMp"

    val unlocalised_string_collector: UnlocalisedStringCollector? = UnlocalisedStringCollector()

    fun onUnlocalisedStringFound(string: UnlocalisedStringCollector.UnlocalisedString) {
        if (unlocalised_string_collector?.add(string) == true) {
            Log.warning("String key '${string.key}' of type ${string.type} has not been localised (source lang=${string.source_language})")
        }
    }

    fun onUnlocalisedStringFound(type: String, key: String?, source_language: String) =
        onUnlocalisedStringFound(UnlocalisedStringCollector.UnlocalisedString(type, key, source_language))
    
    fun onUnlocalisedStringFound(string: LocalisedString) =
        onUnlocalisedStringFound(UnlocalisedStringCollector.UnlocalisedString.fromLocalised(string))
}
