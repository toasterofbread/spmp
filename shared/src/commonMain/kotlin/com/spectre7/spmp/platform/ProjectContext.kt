package com.spectre7.spmp.platform

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.unit.Dp
import java.io.InputStream

const val TOAST_LENGTH_SHORT = 0
const val TOAST_LENGTH_LONG = 1

expect class ProjectContext {
    fun getPrefs(): ProjectPreferences

    @Composable
    fun getStatusBarHeight(): Dp
    fun getLightColorScheme(): ColorScheme
    fun getDarkColorScheme(): ColorScheme

    fun sendToast(text: String, length: Int = TOAST_LENGTH_SHORT)
    fun vibrate(duration: Double)

    fun openResourceFile(path: String): InputStream
    fun listResourceFiles(path: String): List<String>?

    fun loadFontFromFile(path: String): Font
}