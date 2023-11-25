import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.toasterofbread.composekit.platform.composable.onWindowBackPressed
import com.toasterofbread.spmp.model.settings.category.DesktopSettings
import com.toasterofbread.spmp.platform.AppContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.skiko.OS
import org.jetbrains.skiko.hostOs
import java.awt.Toolkit
import java.lang.reflect.Field

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val context = AppContext(SpMp.app_name)
    SpMp.init(context)

    val coroutine_scope = CoroutineScope(Job())
    coroutine_scope.launch {
        context.init()
    }

    SpMp.onStart()

    val toolkit: Toolkit = Toolkit.getDefaultToolkit()
    val class_name_field: Field = toolkit.javaClass.getDeclaredField("awtAppClassName")
    class_name_field.isAccessible = true
    class_name_field.set(toolkit, SpMp.app_name.lowercase())

    application {
        Window(
            title = SpMp.app_name,
            onCloseRequest = ::exitApplication,
            onKeyEvent = { event ->
                if (event.key == Key.Escape && event.type == KeyEventType.KeyDown) {
                    return@Window onWindowBackPressed()
                }
                return@Window false
            },
            state = rememberWindowState(size = DpSize(1280.dp, 720.dp), position = WindowPosition(Alignment.Center))
        ) {
            LaunchedEffect(Unit) {
                val startup_command: String = DesktopSettings.Key.STARTUP_COMMAND.get()
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

            SpMp.App(
                Modifier.onPointerEvent(PointerEventType.Press) { event ->
                    // Mouse back click
                    if (event.button?.index == 5) {
                        onWindowBackPressed()
                    }
                }
            )
        }
    }

    SpMp.onStop()
    SpMp.release()

    coroutine_scope.cancel()
}
