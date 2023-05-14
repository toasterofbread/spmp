package com.spectre7.spmp.platform

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream

private const val MIN_PORTRAIT_RATIO: Float = 1f / 1.2f

fun PlatformContext.vibrateShort() {
    vibrate(0.01)
}

@Composable
fun PlatformContext.isPortrait(): Boolean {
    return (getScreenWidth() / getScreenHeight()) <= MIN_PORTRAIT_RATIO
}

@Composable
fun PlatformContext.isScreenLarge(): Boolean {
    val width = getScreenWidth()
    if (width < 900.dp) {
        return false
    }

    val height = getScreenHeight()
    return height >= 600.dp && (width / height) > MIN_PORTRAIT_RATIO
}

@Composable
fun PlatformContext.getDefaultHorizontalPadding(): Dp = if (isScreenLarge()) 30.dp else 10.dp

expect class PlatformContext {
    fun getPrefs(): ProjectPreferences

    fun getFilesDir(): File
    fun getCacheDir(): File

    fun isAppInForeground(): Boolean

    @Composable
    fun getStatusBarHeight(): Dp
    fun setStatusBarColour(colour: Color, dark_icons: Boolean)

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

    fun openFileInput(name: String): FileInputStream
    fun openFileOutput(name: String, append: Boolean = false): FileOutputStream

    fun openResourceFile(path: String): InputStream
    fun listResourceFiles(path: String): List<String>?

    fun loadFontFromFile(path: String): Font

    fun mainThread(block: () -> Unit)
    fun networkThread(block: () -> Unit): Thread

    fun isConnectionMetered(): Boolean

    @Composable
    fun getScreenHeight(): Dp
    @Composable
    fun getScreenWidth(): Dp

    @Composable
    fun CopyShareButtons(name: String? = null, getText: () -> String)
}