package com.toasterofbread.spmp.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

actual fun isVideoPlaybackSupported(): Boolean = false

@Composable
actual fun VideoPlayback(
    url: String,
    getPositionMs: () -> Long,
    modifier: Modifier,
    fill: Boolean,
    getAlpha: () -> Float
): Boolean {
    throw IllegalStateException()
}
