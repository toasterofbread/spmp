import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.runtime.*
import com.spectre7.spmp.platform.PlatformContext

actual fun getPlatformName(): String = "Desktop"

@Preview
@Composable
fun AppPreview() {
    var initialised by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        SpMp.init(PlatformContext())
        initialised = true
    }

    if (initialised) {
        SpMp.App()
    }
}