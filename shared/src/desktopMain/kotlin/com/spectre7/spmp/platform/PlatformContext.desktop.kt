package com.spectre7.spmp.platform

import androidx.compose.material.Colors
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.unit.Dp
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream

actual open class PlatformContext {
    actual fun getPrefs(): ProjectPreferences {
        TODO("Not yet implemented")
    }

    actual fun getAppName(): String {
        TODO("Not yet implemented")
    }

    actual fun getFilesDir(): File {
        TODO("Not yet implemented")
    }

    actual fun getCacheDir(): File {
        TODO("Not yet implemented")
    }

    actual fun isAppInForeground(): Boolean {
        TODO("Not yet implemented")
    }

    @Composable
    actual fun getStatusBarHeight(): Dp {
        TODO("Not yet implemented")
    }

    actual fun setStatusBarColour(colour: Color, dark_icons: Boolean) {
    }

    actual fun getLightColorScheme(): ColorScheme {
        TODO("Not yet implemented")
    }

    actual fun getDarkColorScheme(): ColorScheme {
        TODO("Not yet implemented")
    }

    actual fun canShare(): Boolean {
        TODO("Not yet implemented")
    }

    actual fun shareText(text: String, title: String?) {
    }

    actual fun canOpenUrl(): Boolean {
        TODO("Not yet implemented")
    }

    actual fun openUrl(url: String) {
    }

    actual fun canSendNotifications(): Boolean {
        TODO("Not yet implemented")
    }

    actual fun sendNotification(title: String, body: String) {
    }

    actual fun sendNotification(throwable: Throwable) {
    }

    actual fun sendToast(text: String, long: Boolean) {
    }

    actual fun vibrate(duration: Double) {
    }

    actual fun openFileInput(name: String): FileInputStream {
        TODO("Not yet implemented")
    }

    actual fun openFileOutput(name: String, append: Boolean): FileOutputStream {
        TODO("Not yet implemented")
    }

    actual fun openResourceFile(path: String): InputStream {
        TODO("Not yet implemented")
    }

    actual fun listResourceFiles(path: String): List<String>? {
        TODO("Not yet implemented")
    }

    actual fun loadFontFromFile(path: String): Font {
        TODO("Not yet implemented")
    }

    actual fun mainThread(block: () -> Unit) {
    }

    actual fun networkThread(block: () -> Unit) {
    }

    actual fun isConnectionMetered(): Boolean {
        TODO("Not yet implemented")
    }

    @Composable
    actual fun getScreenHeight(): Dp {
        TODO("Not yet implemented")
    }

    @Composable
    actual fun getScreenWidth(): Dp {
        TODO("Not yet implemented")
    }

    @Composable
    actual fun CopyShareButtons(name: String?, getText: () -> String) {
    }

}