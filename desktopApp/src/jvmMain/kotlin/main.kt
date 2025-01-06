import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.window.WindowPlacement
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.ui.component.shortcut.trigger.KeyboardShortcutTrigger
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.getTextFieldFocusState
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.isTextFieldFocused
import com.toasterofbread.spmp.model.appaction.shortcut.ShortcutState
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import kotlinx.coroutines.*
import org.jetbrains.compose.resources.getString
import org.jetbrains.skiko.OS
import org.jetbrains.skiko.hostOs
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.app_name
import java.awt.Toolkit
import java.awt.Frame
import java.lang.reflect.Field

@OptIn(ExperimentalComposeUiApi::class)
fun main(args: Array<String>) {
    Thread.setDefaultUncaughtExceptionHandler { _: Thread, error: Throwable ->
        error.printStackTrace()
        val dialog = ExceptionDialog(Frame(), error)
        dialog.isVisible = true
    }

    val coroutine_scope: CoroutineScope = CoroutineScope(Job())
    val context: AppContext = runBlocking { AppContext.create(coroutine_scope) }

    SpMp.init(context)

    val force_software_renderer: Boolean = runBlocking { context.settings.Platform.FORCE_SOFTWARE_RENDERER.get() }
    if (force_software_renderer) {
        System.setProperty("skiko.renderApi", "SOFTWARE")
    }

    val arguments: ProgramArguments =
        runBlocking {
            ProgramArguments.parse(
                args,
                onIllegalArgument = { argument ->
                    println("Ignoring unknown argument '$argument'")
                }
            )
        } ?: return

    SpMp.onStart()

    if (hostOs == OS.Linux) {
        coroutine_scope.launch {
            try {
                // Set AWT class name of window
                val toolkit: Toolkit = Toolkit.getDefaultToolkit()
                val class_name_field: Field = toolkit.javaClass.getDeclaredField("awtAppClassName")
                class_name_field.isAccessible = true
                class_name_field.set(toolkit, getString(Res.string.app_name).lowercase())
            }
            catch (_: Throwable) {}
        }
    }

    lateinit var window: ComposeWindow
    val enable_window_transparency: Boolean = runBlocking { context.settings.Theme.ENABLE_WINDOW_TRANSPARENCY.get() }

    val shortcut_state: ShortcutState = ShortcutState()
    var player: PlayerState? = null

    application {
        val text_field_focus_state: Any = getTextFieldFocusState()

        Window(
            title = SpMp.app_name,
            onCloseRequest = ::exitApplication,
            onKeyEvent = { event ->
                val shortcut_modifier = KeyboardShortcutTrigger.KeyboardModifier.ofKey(event.key)
                if (shortcut_modifier != null) {
                    if (event.type == KeyEventType.KeyDown) {
                        shortcut_state.onModifierDown(shortcut_modifier)
                    }
                    else {
                        shortcut_state.onModifierUp(shortcut_modifier)
                    }
                    return@Window false
                }

                if (event.type != KeyEventType.KeyUp) {
                    return@Window false
                }

                player?.also {
                    return@Window shortcut_state.onKeyPress(event, isTextFieldFocused(text_field_focus_state), it)
                }

                return@Window false
            },
            state = rememberWindowState(
                size = DpSize(1280.dp, 720.dp)
            ),
            undecorated = enable_window_transparency,
            transparent = enable_window_transparency
        ) {
            val player_coroutine_scope: CoroutineScope = rememberCoroutineScope()
            var player_initialised: Boolean by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                player = SpMp.initPlayer(arguments, player_coroutine_scope)
                player_initialised = true

                window = this@Window.window

                if (enable_window_transparency) {
                    window.background = java.awt.Color(0, 0, 0, 0)
                }

                val startup_command: String = context.settings.Platform.STARTUP_COMMAND.get()
                if (startup_command.isBlank()) {
                    return@LaunchedEffect
                }

                withContext(Dispatchers.IO) {
                    try {
                        val process_builder: ProcessBuilder =
                            when (hostOs) {
                                OS.Linux -> ProcessBuilder("bash", "-c", startup_command)
                                OS.Windows -> TODO()
                                else -> return@withContext
                            }

                        process_builder.inheritIO().start()
                    }
                    catch (e: Throwable) {
                        RuntimeException("Execution of startup command failed", e).printStackTrace()
                    }
                }
            }

            if (!player_initialised) {
                return@Window
            }

            SpMp.App(
                arguments,
                shortcut_state,
                Modifier.onPointerEvent(PointerEventType.Press) { event ->
                    val index: Int = event.button?.index ?: return@onPointerEvent
                    player?.also {
                        shortcut_state.onButtonPress(index, it)
                    }
                },
                window_fullscreen_toggler = {
                    if (window.placement == WindowPlacement.Fullscreen) {
                        window.placement = WindowPlacement.Floating
                    }
                    else {
                        window.placement = WindowPlacement.Fullscreen
                    }
                }
            )
        }
    }

    coroutine_scope.cancel()

    SpMp.onStop()
    SpMp.release()
}
