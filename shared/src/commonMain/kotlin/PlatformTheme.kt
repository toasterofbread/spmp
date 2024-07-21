import androidx.compose.runtime.Composable
import dev.toastbits.composekit.settings.ui.ThemeValues

@Composable
internal expect fun PlatformTheme(theme: ThemeValues, content: @Composable () -> Unit)
