import androidx.compose.runtime.Composable
import dev.toastbits.composekit.settings.ui.ThemeValues

@Composable
internal actual fun PlatformTheme(theme: ThemeValues, content: @Composable () -> Unit) {
    content()
}
