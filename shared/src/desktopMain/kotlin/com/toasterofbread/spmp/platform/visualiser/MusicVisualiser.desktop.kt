package com.toasterofbread.spmp.platform.visualiser

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier

actual class MusicVisualiser {
    @Composable
    actual fun Visualiser(colour: Color, modifier: Modifier, opacity: Float) {}

    actual companion object {
        actual fun isSupported(): Boolean = false
    }
}
