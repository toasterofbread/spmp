import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import com.spectre7.spmp.platform.ProjectContext

actual fun getPlatformName(): String = "Desktop"

@Composable fun MainView(context: ProjectContext) = SpMp.App(context)

@Preview
@Composable
fun AppPreview() {
    SpMp.App(ProjectContext())
}