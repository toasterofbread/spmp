package com.toasterofbread.spmp.platform

import LocalPlayerState
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.alpha
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.util.Clock
import androidx.media3.common.util.HandlerWrapper
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.MediaClock
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.StandaloneMediaClock
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.video.MediaCodecVideoRenderer
import androidx.media3.exoplayer.video.VideoRendererEventListener
import androidx.media3.ui.PlayerView
import androidx.media3.ui.AspectRatioFrameLayout
import kotlinx.coroutines.delay
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.Player
import kotlin.math.absoluteValue
import android.graphics.PixelFormat
import android.view.SurfaceView

actual fun doesPlatformSupportVideoPlayback(): Boolean = true

@OptIn(UnstableApi::class)
@Composable
actual fun VideoPlayback(
    url: String,
    getPositionMs: () -> Long,
    modifier: Modifier,
    fill: Boolean,
    getAlpha: () -> Float
): Boolean {
    val context: Context = LocalPlayerState.current.context.ctx

    val exoplayer: ExoPlayer =
        remember {
            ExoPlayer.Builder(context)
                .setUsePlatformDiagnostics(false)
                .build().apply {
                    volume = 0f
                }
        }

    val player_view: PlayerView =
        remember {
            PlayerView(context).apply {
                useController = false
                player = exoplayer
                resizeMode =
                    if (fill) AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    else AspectRatioFrameLayout.RESIZE_MODE_FIT

                val surface: SurfaceView = videoSurfaceView!! as SurfaceView
                surface.forceHasOverlappingRendering(true)
                forceHasOverlappingRendering(true)
                surface.getHolder().setFormat(PixelFormat.TRANSPARENT)
            }
        }

    AndroidView(
        modifier = modifier,
        factory = { player_view }
    )

    LaunchedEffect(getAlpha()) {
        player_view.videoSurfaceView?.alpha = getAlpha()
    }

    LaunchedEffect(url) {
        exoplayer.setMediaItems(listOf(ExoMediaItem.Builder().setUri(Uri.parse(url)).build()))
        exoplayer.prepare()
        exoplayer.play()

        var previous: Long = 0

        while (true) {
            delay(50)

            val position: Long = getPositionMs()

            if (position == previous) {
                exoplayer.seekTo(position)
                exoplayer.pause()
            }
            else {
                if (exoplayer.playbackState != Player.STATE_BUFFERING && (exoplayer.currentPosition - position).absoluteValue > 300) {
                    exoplayer.seekTo(position)
                }

                exoplayer.play()
                previous = position
            }
        }
    }

    return true
}
