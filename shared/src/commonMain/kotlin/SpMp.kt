@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import dev.toastbits.composekit.platform.Platform
import dev.toastbits.composekit.platform.PlatformPreferences
import dev.toastbits.composekit.utils.common.thenIf
import com.toasterofbread.spmp.ProjectBuildConfig
import com.toasterofbread.spmp.model.settings.category.FontMode
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.getUiLanguage
import com.toasterofbread.spmp.platform.playerservice.ClientServerPlayerService
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.service.playercontroller.openUri
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.RootView
import com.toasterofbread.spmp.ui.layout.loadingsplash.LoadingSplash
import com.toasterofbread.spmp.ui.layout.loadingsplash.SplashMode
import com.toasterofbread.spmp.ui.layout.nowplaying.PlayerExpansionState
import com.toasterofbread.spmp.model.appaction.shortcut.LocalShortcutState
import com.toasterofbread.spmp.model.appaction.shortcut.ShortcutState
import com.toasterofbread.spmp.platform.observeUiLanguage
import com.toasterofbread.spmp.ui.layout.nowplaying.ThemeMode
import com.toasterofbread.spmp.ui.theme.ApplicationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import dev.toastbits.spms.socketapi.shared.SPMS_API_VERSION
import org.jetbrains.compose.resources.FontResource
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.action_close
import spmp.shared.generated.resources.app_name
import spmp.shared.generated.resources.warning_spms_api_version_mismatch
import spmp.shared.generated.resources.`warning_spms_api_version_mismatch_$theirs_$ours`

val LocalPlayerState: ProvidableCompositionLocal<PlayerState> = staticCompositionLocalOf { SpMp.player_state }
val LocalProgramArguments: ProvidableCompositionLocal<ProgramArguments> = staticCompositionLocalOf { ProgramArguments() }

object LocalNowPlayingExpansion {
    val current: PlayerExpansionState
        @Composable get() = LocalPlayerState.current.expansion
}

object SpMp {
    fun isDebugBuild(): Boolean = ProjectBuildConfig.IS_DEBUG

    private lateinit var context: AppContext

    var _player_state: PlayerState? = null
        private set
    val player_state: PlayerState get() = _player_state!!

    val prefs: PlatformPreferences get() = context.getPrefs()

    private val low_memory_listeners: MutableList<() -> Unit> = mutableListOf()
    private var window_fullscreen_toggler: (() -> Unit)? = null

    fun init(context: AppContext) {
        this.context = context
    }

    suspend fun initPlayer(
        launch_arguments: ProgramArguments,
        composable_coroutine_scope: CoroutineScope
    ): PlayerState {
        val np_theme_mode: ThemeMode = context.settings.theme.NOWPLAYING_THEME_MODE.get()
        val swipe_sensitivity: Float = context.settings.player.EXPAND_SWIPE_SENSITIVITY.get()
        val player: PlayerState = PlayerState(context, launch_arguments, composable_coroutine_scope, np_theme_mode, swipe_sensitivity)
        player.onStart()
        _player_state = player
        return player
    }

    fun release() {
        _player_state?.release()
    }

    fun onStart() {
    }

    fun onStop() {
        _player_state?.onStop()
    }

    fun toggleFullscreenWindow() {
        window_fullscreen_toggler?.invoke()
    }

    @Composable
    fun App(
        arguments: ProgramArguments,
        shortcut_state: ShortcutState,
        modifier: Modifier = Modifier,
        open_uri: String? = null,
        window_fullscreen_toggler: (() -> Unit)? = null
    ) {
        shortcut_state.ObserveState()
        if (!context.theme.Update()) {
            return
        }

        val coroutine_scope: CoroutineScope = rememberCoroutineScope()

        DisposableEffect(window_fullscreen_toggler) {
            SpMp.window_fullscreen_toggler = window_fullscreen_toggler
            onDispose {
                SpMp.window_fullscreen_toggler = null
            }
        }

        context.theme.ApplicationTheme(context, getFontFamily(context) ?: FontFamily.Default) {
            LaunchedEffect(open_uri) {
                if (open_uri != null) {
                    player_state.openUri(open_uri).onFailure {
                        context.sendNotification(it)
                    }
                }
            }

            Surface(modifier = modifier.fillMaxSize()) {
                val ui_scale: Float by context.settings.system.UI_SCALE.observe()

                CompositionLocalProvider(
                    LocalPlayerState provides player_state,
                    LocalShortcutState provides shortcut_state,
                    LocalDensity provides Density(LocalDensity.current.density * ui_scale, 1f),
                    LocalProgramArguments provides arguments
                ) {
                    var mismatched_server_api_version: Int? by remember { mutableStateOf(null) }
                    val splash_mode: SplashMode? = when (Platform.current) {
                        Platform.ANDROID -> {
                            val external_server_mode: Boolean by player_state.settings.platform.ENABLE_EXTERNAL_SERVER_MODE.observe()
                            if (!player_state.service_connected && external_server_mode) SplashMode.SPLASH
                            else null
                        }
                        Platform.DESKTOP,
                        Platform.WEB ->
                            if (!player_state.service_connected) SplashMode.SPLASH
                            else null
                    }

                    LoadingSplash(
                        splash_mode,
                        player_state.service_load_state,
                        requestServiceChange = { service_companion ->
                            if (!service_companion.isAvailable(player_state.context, arguments)) {
                                return@LoadingSplash
                            }

                            coroutine_scope.launch {
                                player_state.requestServiceChange(service_companion)
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .thenIf(splash_mode != null) {
                                pointerInput(Unit) {}
                            },
                        content_padding = PaddingValues(30.dp),
                    )

                    LaunchedEffect(splash_mode) {
                        mismatched_server_api_version = null
                        if (splash_mode != null) {
                            return@LaunchedEffect
                        }

                        player_state.interactService { service: Any ->
                            if (service !is ClientServerPlayerService) {
                                return@interactService
                            }

                            val server_api_version: Int = service.connected_server?.spms_api_version ?: return@interactService
                            if (server_api_version != SPMS_API_VERSION) {
                                mismatched_server_api_version = server_api_version
                            }
                        }
                    }

                    if (splash_mode == null) {
                        RootView(player_state)
                    }

                    if (mismatched_server_api_version != null) {
                        AlertDialog(
                            { mismatched_server_api_version = null },
                            confirmButton = {
                                Button({ mismatched_server_api_version = null }) {
                                    Text(stringResource(Res.string.action_close))
                                }
                            },
                            title = {
                                Text(stringResource(Res.string.warning_spms_api_version_mismatch))
                            },
                            text = {
                                Text(
                                    stringResource(Res.string.`warning_spms_api_version_mismatch_$theirs_$ours`)
                                        .replace("\$theirs", "v$mismatched_server_api_version")
                                        .replace("\$ours", "v$SPMS_API_VERSION")
                                )
                            }
                        )
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

    @Composable
    private fun getFontFamily(context: AppContext): FontFamily? {
        val ui_language: String by context.observeUiLanguage()
        val font_mode: FontMode by context.settings.system.FONT.observe()
        val font_resource: FontResource? = remember(ui_language, font_mode) { font_mode.getFontResource(ui_language) }

        return font_resource?.let { FontFamily(Font(it)) }
    }

    val app_name: String
        @Composable
        get() = stringResource(Res.string.app_name)
}

expect fun isWindowTransparencySupported(): Boolean
