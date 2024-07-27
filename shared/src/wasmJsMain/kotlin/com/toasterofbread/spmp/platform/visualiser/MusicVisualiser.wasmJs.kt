package com.toasterofbread.spmp.platform.visualiser

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

actual class MusicVisualiser {
    @Composable
    actual fun Visualiser(
        colour: Color,
        modifier: Modifier,
        opacity: Float
    ) {
        throw IllegalStateException()
    }

    actual companion object {
        actual fun isSupported(): Boolean = false
    }
}
