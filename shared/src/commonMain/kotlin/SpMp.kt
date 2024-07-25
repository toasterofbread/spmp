@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

import androidx.compose.foundation.gestures.AnchoredDraggableState
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
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import dev.toastbits.composekit.platform.Platform
import dev.toastbits.composekit.platform.PlatformPreferences
import dev.toastbits.composekit.utils.common.thenIf
import com.toasterofbread.spmp.ProjectBuildConfig
import com.toasterofbread.spmp.db.Database
import com.toasterofbread.spmp.model.settings.category.FontMode
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.playerservice.ClientServerPlayerService
import com.toasterofbread.spmp.service.playercontroller.openUri
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.RootView
import com.toasterofbread.spmp.ui.layout.loadingsplash.LoadingSplash
import com.toasterofbread.spmp.ui.layout.loadingsplash.SplashMode
import com.toasterofbread.spmp.ui.layout.nowplaying.PlayerExpansionState
import com.toasterofbread.spmp.model.appaction.shortcut.LocalShortcutState
import com.toasterofbread.spmp.model.appaction.shortcut.ShortcutState
import com.toasterofbread.spmp.model.settings.Settings
import com.toasterofbread.spmp.model.state.PlayerState
import com.toasterofbread.spmp.model.state.PlayerStateImpl
import com.toasterofbread.spmp.model.state.SessionState
import com.toasterofbread.spmp.model.state.SessionStateImpl
import com.toasterofbread.spmp.model.state.UiState
import com.toasterofbread.spmp.model.state.UiStateImpl
import com.toasterofbread.spmp.platform.AppThemeManager
import com.toasterofbread.spmp.platform.observeUiLanguage
import com.toasterofbread.spmp.ui.layout.nowplaying.ThemeMode
import com.toasterofbread.spmp.ui.theme.ApplicationTheme
import dev.toastbits.composekit.settings.ui.ThemeManager
import dev.toastbits.composekit.settings.ui.ThemeValues
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

val LocalAppState: ProvidableCompositionLocal<SpMp.State> = staticCompositionLocalOf { SpMp.state }
val LocalPlayerState: ProvidableCompositionLocal<PlayerState> = staticCompositionLocalOf { SpMp.player_state }
val LocalSessionState: ProvidableCompositionLocal<SessionState> = staticCompositionLocalOf { SpMp.session_state }
val LocalUiState: ProvidableCompositionLocal<UiState> = staticCompositionLocalOf { SpMp.ui_state }
val LocalProgramArguments: ProvidableCompositionLocal<ProgramArguments> = staticCompositionLocalOf { ProgramArguments() }

object LocalNowPlayingExpansion {
    val current: PlayerExpansionState
        @Composable get() = LocalUiState.current.player_expansion
}

object LocalAppContext {
    val current: AppContext
        @Composable get() = LocalAppState.current.context
}

object LocalDataase {
    val current: Database
        @Composable get() = LocalAppContext.current.database
}

object LocalTheme {
    val current: ThemeValues
        @Composable get() = LocalUiState.current.theme
}

object LocalSettings {
    val current: Settings
        @Composable get() = LocalAppContext.current.settings
}

object SpMp {
    fun isDebugBuild(): Boolean = ProjectBuildConfig.IS_DEBUG

    private lateinit var context: AppContext

    var _state: State? = null
        private set
    val state: State get() = _state!!

    data class State(
        val context: AppContext,
        val player: PlayerState,
        val session: SessionState,
        val ui: UiState
    ) {
        val settings: Settings get() = context.settings
        val database: Database get() = context.database
        val theme: AppThemeManager get() = ui.theme
    }

    lateinit var player_state: PlayerStateImpl
        private set
    lateinit var session_state: SessionStateImpl
        private set
    lateinit var ui_state: UiStateImpl
        private set

    val prefs: PlatformPreferences get() = context.getPrefs()

    private val low_memory_listeners: MutableList<() -> Unit> = mutableListOf()
    private var window_fullscreen_toggler: (() -> Unit)? = null
    private val screen_size_state: MutableState<DpSize> = mutableStateOf(DpSize.Zero)

    fun init(context: AppContext) {
        this.context = context
    }

    suspend fun initState(
        launch_arguments: ProgramArguments,
        composable_coroutine_scope: CoroutineScope
    ): State {
        val np_theme_mode: ThemeMode = context.settings.theme.NOWPLAYING_THEME_MODE.get()
        val swipe_sensitivity: Float = context.settings.state.EXPAND_SWIPE_SENSITIVITY.get()

        session_state =
            SessionStateImpl(
                context,
                composable_coroutine_scope,
                launch_arguments,
                low_memory_listener = {
                    for (listener in low_memory_listeners) {
                        listener.invoke()
                    }
                }
            )

        val np_swipe_state: AnchoredDraggableState<Int> = PlayerStateImpl.createSwipeState()

        player_state =
            PlayerStateImpl(
                context.settings,
                session_state,
                composable_coroutine_scope,
                np_theme_mode,
                swipe_sensitivity,
                requestFocus = { page ->
                    ui_state.switchPlayerPage(page)
                },
                np_swipe_state = np_swipe_state
            )

        ui_state =
            UiStateImpl(
                context,
                composable_coroutine_scope,
                player_state,
                np_swipe_state,
                screen_size_state
            )

        session_state.onStart()
        _state = State(
            context,
            player_state,
            session_state,
            ui_state
        )
        return state
    }

    fun release() {
        _state?.session?.release()
    }

    fun onStart() {
    }

    fun onStop() {
        _state?.session?.onStop()
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
                    state.openUri(open_uri).onFailure {
                        context.sendNotification(it)
                    }
                }
            }

            Surface(modifier = modifier.fillMaxSize()) {
                val ui_scale: Float by context.settings.system.UI_SCALE.observe()

                CompositionLocalProvider(
                    LocalAppState provides state,
                    LocalPlayerState provides player_state,
                    LocalSessionState provides session_state,
                    LocalUiState provides ui_state,
                    LocalShortcutState provides shortcut_state,
                    LocalDensity provides Density(LocalDensity.current.density * ui_scale, 1f),
                    LocalProgramArguments provides arguments
                ) {
                    var mismatched_server_api_version: Int? by remember { mutableStateOf(null) }
                    val splash_mode: SplashMode? = when (Platform.current) {
                        Platform.ANDROID -> {
                            val external_server_mode: Boolean by state.settings.platform.ENABLE_EXTERNAL_SERVER_MODE.observe()
                            if (!session_state.service_connected && external_server_mode) SplashMode.SPLASH
                            else null
                        }
                        Platform.DESKTOP,
                        Platform.WEB ->
                            if (!session_state.service_connected) SplashMode.SPLASH
                            else null
                    }

                    LoadingSplash(
                        splash_mode,
                        session_state.service_load_state,
                        requestServiceChange = { service_companion ->
                            if (!service_companion.isAvailable(state.context, arguments)) {
                                return@LoadingSplash
                            }

                            coroutine_scope.launch {
                                session_state.requestServiceChange(service_companion)
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

                        session_state.interactService { service: Any ->
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
                        RootView(
                            state,
                            onScreenSizeChanged = { size ->
                                screen_size_state.value = size
                            }
                        )
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
        exception?.printStackTrace()
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
