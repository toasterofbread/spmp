package com.toasterofbread.spmp.platform.visualiser

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier

expect class MusicVisualiser {
    @Composable
    fun Visualiser(colour: Color, modifier: Modifier, opacity: Float = 1f)

    companion object {
        fun isSupported(): Boolean
    }
}
