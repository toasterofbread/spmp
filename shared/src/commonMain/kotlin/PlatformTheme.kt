import androidx.compose.runtime.Composable
import dev.toastbits.composekit.settings.ui.Theme

@Composable
internal expect fun PlatformTheme(theme: Theme, content: @Composable () -> Unit)
