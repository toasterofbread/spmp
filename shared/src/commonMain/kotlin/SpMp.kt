@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import com.toasterofbread.spmp.model.FontMode
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.toastercomposetools.platform.PlatformPreferences
import com.toasterofbread.spmp.ProjectBuildConfig
import com.toasterofbread.spmp.platform.getUiLanguage
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.resources.initResources
import com.toasterofbread.spmp.resources.uilocalisation.LocalisedString
import com.toasterofbread.spmp.resources.uilocalisation.UnlocalisedStringCollector
import com.toasterofbread.spmp.resources.uilocalisation.YoutubeUILocalisation
import com.toasterofbread.spmp.resources.uilocalisation.localised.UILanguages
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.LoadingSplashView
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.PlayerState
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.PlayerStateImpl
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.RootView
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingExpansionState
import com.toasterofbread.spmp.ui.theme.ApplicationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.logging.Logger

expect fun getPlatformName(): String

val LocalPlayerState: ProvidableCompositionLocal<PlayerState> = staticCompositionLocalOf { SpMp.player_state }
object LocalNowPlayingExpansion {
    val current: NowPlayingExpansionState
        @Composable get() = LocalPlayerState.current.expansion
}

object SpMp {
    fun isDebugBuild(): Boolean = ProjectBuildConfig.IS_DEBUG
    val Log: Logger = Logger.getLogger(SpMp::class.java.name)

    private lateinit var context: AppContext
    private lateinit var player_state: PlayerStateImpl

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

        player_state = PlayerStateImpl(context)

        initResources(context.getUiLanguage(), context)
        _yt_ui_localisation = YoutubeUILocalisation(UILanguages())
    }

    fun release() {
        _yt_ui_localisation = null
        coroutine_scope.cancel()
        player_state.release()
    }

    fun onStart() {
        player_state.onStart()
    }

    fun onStop() {
        player_state.onStop()
    }

    @Composable
    fun App(open_uri: String? = null) {
        context.theme.Update()

        context.theme.ApplicationTheme(context, getFontFamily(context) ?: FontFamily.Default) {
            LaunchedEffect(open_uri) {
                if (open_uri != null) {
                    player_state.openUri(open_uri).onFailure {
                        context.sendNotification(it)
                    }
                }
            }

            Surface(modifier = Modifier.fillMaxSize()) {
                CompositionLocalProvider(LocalPlayerState provides player_state) {
                    RootView(player_state)
                    LoadingSplashView(Modifier.fillMaxSize())
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
        val font_mode: FontMode = Settings.KEY_FONT.getEnum(context.getPrefs())
        val font_path: String = font_mode.getFontFilePath(context.getUiLanguage()) ?: return null
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
    
    fun onUnlocalisedStringFound(string: LocalisedString) =
        onUnlocalisedStringFound(UnlocalisedStringCollector.UnlocalisedString.fromLocalised(string))
}
