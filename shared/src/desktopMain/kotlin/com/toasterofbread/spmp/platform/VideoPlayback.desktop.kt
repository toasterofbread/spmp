package com.toasterofbread.spmp.platform

import com.toasterofbread.spmp.platform.ffmpeg.VideoPlayerFFmpeg
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable

actual fun doesPlatformSupportVideoPlayback(): Boolean = true

@Composable
actual fun VideoPlayback(
    url: String,
    getPositionMs: () -> Long,
    modifier: Modifier,
    fill: Boolean,
    getAlpha: () -> Float
): Boolean {
    return VideoPlayerFFmpeg(
        url = url,
        getPositionMs = getPositionMs,
        modifier = modifier,
        fill = fill,
        getAlpha = getAlpha
    )
}
