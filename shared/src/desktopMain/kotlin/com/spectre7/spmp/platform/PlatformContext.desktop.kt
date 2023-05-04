package com.spectre7.spmp.platform

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.spectre7.spmp.PlayerService
import com.spectre7.utils.getString
import org.jetbrains.skiko.OS
import org.jetbrains.skiko.hostOs
import java.awt.Desktop
import java.awt.Dimension
import java.awt.Window
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Files
import kotlin.io.path.name
import kotlin.streams.toList

private fun getHomeDir(): File = File(System.getProperty("user.home"))

actual open class PlatformContext {
    actual fun getPrefs(): ProjectPreferences = ProjectPreferences.getInstance(this)

    actual fun getFilesDir(): File {
        val subdir = when (hostOs) {
            OS.Linux -> ".local/share"
            OS.Windows -> TODO()
            OS.MacOS -> TODO()
            else -> throw NotImplementedError(hostOs.name)
        }
        return getHomeDir().resolve(subdir).resolve(SpMp.app_name)
    }

    actual fun getCacheDir(): File {
        val subdir = when (hostOs) {
            OS.Linux -> ".cache"
            OS.Windows -> TODO()
            OS.MacOS -> TODO()
            else -> throw NotImplementedError(hostOs.name)
        }
        return getHomeDir().resolve(subdir).resolve(SpMp.app_name)
    }

    actual fun isAppInForeground(): Boolean {
        TODO("Not yet implemented")
    }

    @Composable
    actual fun getStatusBarHeight(): Dp = 0.dp

    actual fun setStatusBarColour(colour: Color, dark_icons: Boolean) {}

    actual fun getLightColorScheme(): ColorScheme = lightColorScheme()

    actual fun getDarkColorScheme(): ColorScheme = darkColorScheme()

    actual fun canShare(): Boolean = false

    actual fun shareText(text: String, title: String?) {
        throw NotImplementedError()
    }

    actual fun canOpenUrl(): Boolean = Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)

    actual fun openUrl(url: String) {
        assert(canOpenUrl())
        Desktop.getDesktop().browse(URI(url))
    }

    actual fun canSendNotifications(): Boolean {
        TODO()
    }

    actual fun sendNotification(title: String, body: String) {
    }

    actual fun sendNotification(throwable: Throwable) {
    }

    actual fun sendToast(text: String, long: Boolean) {
    }

    actual fun vibrate(duration: Double) {}

    actual fun openFileInput(name: String): FileInputStream =
        getFilesDir().resolve(name).inputStream()

    actual fun openFileOutput(name: String, append: Boolean): FileOutputStream {
        val path = getFilesDir().resolve(name)
        path.createNewFile()
        return path.outputStream()
    }

    private fun getResourceDir(): File = File("/assets")

    actual fun openResourceFile(path: String): InputStream {
        val resource_path = getResourceDir().resolve(path).path
        return PlayerService::class.java.getResourceAsStream(resource_path)!!
    }

    actual fun listResourceFiles(path: String): List<String>? {
        val resource_path = getResourceDir().resolve(path).path
        val resource = PlayerService::class.java.getResource(resource_path)!!

        val file_system = FileSystems.newFileSystem(resource.toURI(), emptyMap<String, Any>())
        return Files.list(file_system.getPath(resource_path)).toList().map { it.name }
    }

    actual fun loadFontFromFile(path: String): Font {
        val resource_path = getResourceDir().resolve(path).path

        val stream = PlayerService::class.java.getResourceAsStream(resource_path)!!
        val bytes = stream.readBytes()
        stream.close()

        return Font(path, bytes)
    }

    actual fun mainThread(block: () -> Unit) {
        // TODO
        block()
    }

    actual fun networkThread(block: () -> Unit): Thread {
        // TODO
        block()
    }

    actual fun isConnectionMetered(): Boolean {
        TODO("Not yet implemented")
    }

    private var screen_size: Dimension? by mutableStateOf(null)
    fun updateScreenSize() {
        screen_size = Window.getWindows().first().size
    }

    @Composable
    actual fun getScreenHeight(): Dp {
        return with (LocalDensity.current) { screen_size!!.height.toDp() }
    }

    @Composable
    actual fun getScreenWidth(): Dp {
        return with (LocalDensity.current) { screen_size!!.width.toDp() }
    }

    @Composable
    actual fun CopyShareButtons(name: String?, getText: () -> String) {
        // TODO
    }
}