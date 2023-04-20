import androidx.compose.runtime.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.spectre7.spmp.platform.PlatformContext
import kotlinx.coroutines.delay

private const val SCREEN_SIZE_UPDATE_INTERVAL: Long = 100

fun main() = application {
    val context = PlatformContext()
    SpMp.init(context)

    Window(onCloseRequest = ::exitApplication) {
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
            SpMp.App()
        }
    }
}