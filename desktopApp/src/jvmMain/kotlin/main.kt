import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.composekit.platform.composable.onWindowBackPressed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SCREEN_SIZE_UPDATE_INTERVAL: Long = 100

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val context = AppContext(SpMp.app_name)
    SpMp.init(context)

    val coroutine_scope = CoroutineScope(Job())
    coroutine_scope.launch {
        context.init()
    }

    SpMp.onStart()

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
            var initialised by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                context.updateScreenSize()
                initialised = true

                while (true) {
                    context.updateScreenSize()
                    delay(SCREEN_SIZE_UPDATE_INTERVAL)
                }
            }

            if (initialised) {
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
    }

    SpMp.onStop()
    SpMp.release()

    coroutine_scope.cancel()
}
