package com.toasterofbread.spmp.platform

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.toasterofbread.Database
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.PlayerState
import com.toasterofbread.spmp.youtubeapi.YoutubeApi
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

private const val MIN_PORTRAIT_RATIO: Float = 1f / 1.2f

expect class PlatformContext {
    val database: Database
    val download_manager: PlayerDownloadManager
    val ytapi: YoutubeApi

    fun getPrefs(): PlatformPreferences

    fun getFilesDir(): File
    fun getCacheDir(): File

    fun promptForUserDirectory(persist: Boolean = false, callback: (uri: String?) -> Unit)
    fun getUserDirectoryFile(uri: String): PlatformFile

    fun isAppInForeground(): Boolean

    @Composable
    fun getStatusBarHeightDp(): Dp
    fun setStatusBarColour(colour: Color)

    fun getNavigationBarHeight(): Int
    fun setNavigationBarColour(colour: Color?)
    fun isDisplayingAboveNavigationBar(): Boolean

    @Composable
    fun getImeInsets(): WindowInsets?
    @Composable
    fun getSystemInsets(): WindowInsets?

    fun getLightColorScheme(): ColorScheme
    fun getDarkColorScheme(): ColorScheme

    fun canShare(): Boolean
    fun shareText(text: String, title: String? = null)

    fun canOpenUrl(): Boolean
    fun openUrl(url: String)

    fun canSendNotifications(): Boolean
    fun sendNotification(title: String, body: String)
    fun sendNotification(throwable: Throwable)

    fun sendToast(text: String, long: Boolean = false)

    fun vibrate(duration: Double)

    fun deleteFile(name: String): Boolean
    fun openFileInput(name: String): FileInputStream
    fun openFileOutput(name: String, append: Boolean = false): FileOutputStream

    fun openResourceFile(path: String): InputStream
    fun listResourceFiles(path: String): List<String>?

    fun loadFontFromFile(path: String): Font

    fun isConnectionMetered(): Boolean

    @Composable
    fun CopyShareButtons(name: String? = null, getText: () -> String)
}

expect class PlatformFile {
    val uri: String
    val name: String
    val path: String
    val absolute_path: String

    val exists: Boolean
    val is_directory: Boolean
    val is_file: Boolean

    fun getRelativePath(relative_to: PlatformFile): String
    fun inputStream(): InputStream
    fun outputStream(append: Boolean = false): OutputStream

    fun listFiles(): List<PlatformFile>?
    fun resolve(relative_path: String): PlatformFile
    fun getSibling(sibling_name: String): PlatformFile

    fun delete(): Boolean
    fun createFile(): Boolean
    fun mkdirs(): Boolean
    fun renameTo(new_name: String): PlatformFile
//    fun copyTo(destination: PlatformFile)
//    fun delete()
    fun moveDirContentTo(destination: PlatformFile): Result<PlatformFile>

    companion object {
        fun fromFile(file: File, context: PlatformContext): PlatformFile
    }
}

fun PlatformContext.vibrateShort() {
    vibrate(0.01)
}

@Composable
fun PlayerState.isPortrait(): Boolean {
    return (screen_size.width / screen_size.height) <= MIN_PORTRAIT_RATIO
}

@Composable
fun PlayerState.isScreenLarge(): Boolean {
    if (screen_size.width < 900.dp) {
        return false
    }
    return screen_size.height >= 600.dp && (screen_size.width / screen_size.height) > MIN_PORTRAIT_RATIO
}

@Composable
fun PlayerState.getDefaultHorizontalPadding(): Dp = if (isScreenLarge()) 30.dp else 10.dp
@Composable
fun PlayerState.getDefaultVerticalPadding(): Dp = if (isScreenLarge()) 30.dp else 10.dp // TODO

@Composable
fun PlayerState.getDefaultPaddingValues(): PaddingValues = PaddingValues(horizontal = getDefaultHorizontalPadding(), vertical = getDefaultVerticalPadding())

@Composable
fun PlatformContext.getNavigationBarHeightDp(): Dp = with(LocalDensity.current) {
    getNavigationBarHeight().toDp()
}
