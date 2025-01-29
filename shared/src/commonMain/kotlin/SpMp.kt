import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.ProjectBuildConfig
import com.toasterofbread.spmp.model.appaction.shortcut.LocalShortcutState
import com.toasterofbread.spmp.model.appaction.shortcut.ShortcutState
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.playerservice.ClientServerPlayerService
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.service.playercontroller.openUri
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.layout.apppage.AppPage
import com.toasterofbread.spmp.ui.layout.apppage.AppPageState
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.RootView
import com.toasterofbread.spmp.ui.layout.loadingsplash.LoadingSplash
import com.toasterofbread.spmp.ui.layout.loadingsplash.SplashMode
import com.toasterofbread.spmp.ui.layout.nowplaying.PlayerExpansionState
import com.toasterofbread.spmp.ui.layout.nowplaying.ThemeMode
import dev.toastbits.composekit.application.ApplicationTheme
import dev.toastbits.composekit.commonsettings.impl.LocalComposeKitSettings
import dev.toastbits.composekit.components.LocalContext
import dev.toastbits.composekit.navigation.compositionlocal.LocalNavigator
import dev.toastbits.composekit.navigation.navigator.Navigator
import dev.toastbits.composekit.navigation.screen.Screen
import dev.toastbits.composekit.navigation.screen.ScreenButton
import dev.toastbits.composekit.settings.PlatformSettings
import dev.toastbits.composekit.theme.core.ThemeValues
import dev.toastbits.composekit.util.platform.Platform
import dev.toastbits.composekit.util.thenIf
import dev.toastbits.spms.socketapi.shared.SPMS_API_VERSION
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
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

    var _player_state: PlayerState? by mutableStateOf(null)
        private set
    val player_state: PlayerState get() = _player_state!!

    val prefs: PlatformSettings get() = context.getPrefs()

    private val low_memory_listeners: MutableList<() -> Unit> = mutableListOf()
    private var window_fullscreen_toggler: (() -> Unit)? = null

    fun init(context: AppContext) {
        this.context = context
    }

    suspend fun initPlayer(
        launch_arguments: ProgramArguments,
        composable_coroutine_scope: CoroutineScope
    ): PlayerState {
        val np_theme_mode: ThemeMode = context.settings.Theme.NOWPLAYING_THEME_MODE.get()
        val swipe_sensitivity: Float = context.settings.Player.EXPAND_SWIPE_SENSITIVITY.get()
        val player: PlayerState = PlayerState(context, launch_arguments, composable_coroutine_scope, np_theme_mode, swipe_sensitivity)
        _player_state = player
        return player
    }

    fun release() {
        _player_state?.release()
    }

    fun onStart() {
        _player_state?.onStart()
    }

    fun onStop() {
        _player_state?.onStop()
    }

    fun toggleFullscreenWindow() {
        window_fullscreen_toggler?.invoke()
    }

    private val navigator = object : Navigator {
        override val currentExtraButtons: List<ScreenButton>
            @Composable
            get() = emptyList()
        override val currentInfo: String?
            @Composable
            get() = null
        override val currentTitle: String?
            @Composable
            get() = null
        override val currentScreen: Screen = Screen.EMPTY

        @Composable
        override fun CurrentScreen(
            modifier: Modifier,
            contentPadding: PaddingValues,
            render: @Composable (Modifier, PaddingValues, @Composable (Modifier, PaddingValues) -> Unit) -> Unit
        ) {
            throw IllegalStateException()
        }

        override fun addHistoryResetListener(listener: Navigator.HistoryResetListener) {
            TODO("Not yet implemented")
        }

        override fun getNavigateBackwardCount(): Int = 0
        override fun getNavigateForwardCount(): Int = 0

        override fun getMostRecentOfOrNull(predicate: (Screen) -> Boolean): Screen? = null
        override fun navigateBackward(by: Int) {
            _player_state?.navigateBack()
        }
        override fun navigateForward(by: Int) {}
        override fun peekRelative(offset: Int): Screen? = null

        override fun pushScreen(screen: Screen) {
            val player: PlayerState = _player_state ?: return
            player.openAppPage(screen.toAppPage(player.app_page_state), replace_current = false)
        }

        override fun removeHistoryResetListener(listener: Navigator.HistoryResetListener) {
            TODO("Not yet implemented")
        }

        override fun removeScreens(amount: Int) {
            TODO("Not yet implemented")
        }

        override fun replaceScreen(screen: Screen, inPlace: Boolean) {
            val player: PlayerState = _player_state ?: return
            player.openAppPage(screen.toAppPage(player.app_page_state), replace_current = true)
        }

        override fun replaceScreenUpTo(
            screen: Screen,
            inPlace: Boolean,
            isLastScreenToReplace: (Screen) -> Boolean
        ) {
            TODO("Not yet implemented")
        }

        override fun clearForwardNavigation() {
            TODO("Not yet implemented")
        }

        override fun visualise(): String {
            TODO("Not yet implemented")
        }
    }

    private fun Screen.toAppPage(state: AppPageState): AppPage =
        object : AppPage() {
            override val state: AppPageState = state

            @Composable
            override fun ColumnScope.Page(
                multiselect_context: MediaItemMultiSelectContext,
                modifier: Modifier,
                content_padding: PaddingValues,
                close: () -> Unit
            ) {
                this@toAppPage.Content(modifier, content_padding)
            }

            override fun onClosed(next_page: AppPage?, going_back: Boolean) {
                super.onClosed(next_page, going_back)
                this@toAppPage.onClosed(going_back)
            }
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

        val coroutine_scope: CoroutineScope = rememberCoroutineScope()

        DisposableEffect(window_fullscreen_toggler) {
            SpMp.window_fullscreen_toggler = window_fullscreen_toggler
            onDispose {
                SpMp.window_fullscreen_toggler = null
            }
        }

        Theme(context) {
            LaunchedEffect(open_uri) {
                if (open_uri != null) {
                    player_state.openUri(open_uri).onFailure {
                        context.sendNotification(it)
                    }
                }
            }

            Surface(modifier = modifier.fillMaxSize()) {
                CompositionLocalProvider(
                    LocalPlayerState provides player_state,
                    LocalShortcutState provides shortcut_state,
                    LocalProgramArguments provides arguments,
                    LocalContext provides context,
                    LocalNavigator provides navigator,
                    LocalComposeKitSettings provides context.settings
                ) {
                    var mismatched_server_api_version: Int? by remember { mutableStateOf(null) }
                    val splash_mode: SplashMode? = when (Platform.current) {
                        Platform.ANDROID -> {
                            val external_server_mode: Boolean by player_state.settings.Platform.ENABLE_EXTERNAL_SERVER_MODE.observe()
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
        exception?.printStackTrace()
        context.sendToast(exception.toString())
    }

    val app_name: String
        @Composable
        get() = stringResource(Res.string.app_name)
}

expect fun isWindowTransparencySupported(): Boolean

@Composable
fun SpMp.Theme(
    context: AppContext,
    theme: ThemeValues = context.theme,
    content: @Composable () -> Unit
) {
    theme.ApplicationTheme(
        context,
        context.settings,
        content = content
    )
}
