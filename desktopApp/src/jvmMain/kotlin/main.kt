import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.spectre7.spmp.platform.PlatformContext

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        MainView(PlatformContext())
    }
}