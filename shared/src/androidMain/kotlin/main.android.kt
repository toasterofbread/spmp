import androidx.compose.runtime.Composable
import com.spectre7.spmp.platform.ProjectContext

actual fun getPlatformName(): String = "Android"

@Composable fun MainView(context: ProjectContext) = SpMp.App(context)
