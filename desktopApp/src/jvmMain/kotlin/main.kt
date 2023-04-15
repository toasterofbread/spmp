import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.spectre7.spmp.platform.PlatformContext

fun main() = application {
    SpMp.init(PlatformContext())
    Window(onCloseRequest = ::exitApplication) {
        SpMp.App()
    }
}