import androidx.compose.runtime.Composable
import com.toasterofbread.composekit.settings.ui.Theme

@Composable
internal expect fun PlatformTheme(theme: Theme, content: @Composable () -> Unit)
