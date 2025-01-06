import androidx.compose.runtime.Composable
import dev.toastbits.composekit.theme.core.ThemeValues

@Composable
internal actual fun PlatformTheme(theme: ThemeValues, content: @Composable () -> Unit) {
    content()
}
