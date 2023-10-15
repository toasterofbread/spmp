package com.toasterofbread.spmp.platform

import SpMp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.platform.playerservice.PlatformPlayerService
import com.toasterofbread.spmp.ui.theme.Theme
import com.toasterofbread.spmp.youtubeapi.YoutubeApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.skiko.OS
import org.jetbrains.skiko.hostOs
import java.awt.Desktop
import java.awt.Dimension
import java.awt.Window
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.URI
import java.nio.file.FileSystem
import java.nio.file.FileSystemNotFoundException
import java.nio.file.FileSystems
import java.nio.file.Files
import kotlin.io.path.name

private fun getHomeDir(): File = File(System.getProperty("user.home"))
private var instantiated: Boolean = false

actual open class PlatformContext() {
    actual val database = createDatabase()
    actual val download_manager = PlayerDownloadManager(this)
    actual val ytapi: YoutubeApi
    actual val theme: Theme by lazy { Theme(this) }

    init {
        check(!instantiated)
        instantiated = true

        val prefs = getPrefs()
        val youtubeapi_type: YoutubeApi.Type = Settings.KEY_YOUTUBEAPI_TYPE.getEnum(prefs)
        ytapi = youtubeapi_type.instantiate(this, Settings.KEY_YOUTUBEAPI_URL.get(prefs))
    }

    suspend fun init(): PlatformContext {
        ytapi.init()
        return this
    }

    actual fun getPrefs(): PlatformPreferences = PlatformPreferences.getInstance(this)

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
    actual fun getStatusBarHeightDp(): Dp = 0.dp

    actual fun setStatusBarColour(colour: Color) {}

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
        return PlatformPlayerService::class.java.getResourceAsStream(resource_path)!!
    }

    @Suppress("NewApi")
    actual fun listResourceFiles(path: String): List<String>? {
        val resource_path: String = getResourceDir().resolve(path).path

        val resource = PlatformPlayerService::class.java.getResource(resource_path)!!

        val file_system: FileSystem =
            try {
                FileSystems.getFileSystem(resource.toURI())
            }
            catch (_: FileSystemNotFoundException) {
                FileSystems.newFileSystem(resource.toURI(), emptyMap<String, Any>())
            }

        return Files.list(file_system.getPath(resource_path)).toList().map { it.name }
    }

    actual fun loadFontFromFile(path: String): Font {
        val resource_path = getResourceDir().resolve(path).path

        val stream = PlatformPlayerService::class.java.getResourceAsStream(resource_path)!!
        val bytes = stream.readBytes()
        stream.close()

        return Font(path, bytes)
    }

    actual fun isConnectionMetered(): Boolean {
        TODO("Not yet implemented")
    }

    private var screen_size: Dimension? by mutableStateOf(null)
    fun updateScreenSize() {
        screen_size = Window.getWindows().first().size
    }

    @Composable
    actual fun CopyShareButtons(name: String?, getText: () -> String) {
        // TODO
    }

    actual fun promptForUserDirectory(persist: Boolean, callback: (uri: String?) -> Unit) {
        TODO()
    }

    actual fun getUserDirectoryFile(uri: String): PlatformFile {
        TODO("Not yet implemented")
    }

    actual fun getNavigationBarHeight(): Int = 0

    actual fun setNavigationBarColour(colour: Color?) {}

    actual fun isDisplayingAboveNavigationBar(): Boolean = false

    @Composable
    actual fun getImeInsets(): WindowInsets? = null

    @Composable
    actual fun getSystemInsets(): WindowInsets? = null

    actual fun deleteFile(name: String): Boolean {
        TODO("Not yet implemented")
    }
}

actual class PlatformFile {
    actual val uri: String
        get() = TODO("Not yet implemented")
    actual val name: String
        get() = TODO("Not yet implemented")
    actual val path: String
        get() = TODO("Not yet implemented")
    actual val absolute_path: String
        get() = TODO("Not yet implemented")
    actual val exists: Boolean
        get() = TODO("Not yet implemented")
    actual val is_directory: Boolean
        get() = TODO("Not yet implemented")
    actual val is_file: Boolean
        get() = TODO("Not yet implemented")

    actual fun getRelativePath(relative_to: PlatformFile): String {
        TODO("Not yet implemented")
    }

    actual fun inputStream(): InputStream {
        TODO("Not yet implemented")
    }

    actual fun outputStream(append: Boolean): OutputStream {
        TODO("Not yet implemented")
    }

    actual fun listFiles(): List<PlatformFile>? {
        TODO("Not yet implemented")
    }

    actual fun resolve(relative_path: String): PlatformFile {
        TODO("Not yet implemented")
    }

    actual fun getSibling(sibling_name: String): PlatformFile {
        TODO("Not yet implemented")
    }

    actual fun delete(): Boolean {
        TODO("Not yet implemented")
    }

    actual fun createFile(): Boolean {
        TODO("Not yet implemented")
    }

    actual fun mkdirs(): Boolean {
        TODO("Not yet implemented")
    }

    actual fun renameTo(new_name: String): PlatformFile {
        TODO("Not yet implemented")
    }

    actual fun moveDirContentTo(destination: PlatformFile): Result<PlatformFile> {
        TODO("Not yet implemented")
    }

    actual companion object {
        actual fun fromFile(
            file: File,
            context: PlatformContext,
        ): PlatformFile {
            TODO("Not yet implemented")
        }
    }
}
