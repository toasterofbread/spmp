package com.toasterofbread.spmp.platform.ffmpeg

// Thanks to timo-drick
// https://github.com/timo-drick/cfd_video

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import dev.toastbits.composekit.util.platform.launchSingle

@Composable
fun VideoPlayerFFmpeg(
    url: String,
    getPositionMs: () -> Long,
    modifier: Modifier = Modifier,
    fill: Boolean = false,
    getAlpha: () -> Float = { 1f },
    state: FFmpegVideoPlayerState = remember { FFmpegVideoPlayerState() },
): Boolean {
    var image_state: Boolean by remember { mutableStateOf(false) }
    var current_image: ImageBitmap? by remember { mutableStateOf(null) }
    val coroutine_scope: CoroutineScope = rememberCoroutineScope()

    DisposableEffect(url) {
        coroutine_scope.launchSingle(Dispatchers.Default) {
            try {
                state.open(url)

                val stream: KVideoStream = state.streams().first()
                val codec: KAVCodec = state.codec(stream)

                state.play(
                    stream = stream,
                    hwDecoder = codec.hwDecoder.getBestDecoder(),
                    targetSize = IntSize(stream.width, stream.height)
                )

                val frame_grabber: KFrameGrabber =
                    requireNotNull(state.frame_grabber) { "Frame grabber not initialized!" }

                var previous_position: Long = -1L

                while (isActive) {
                    withFrameMillis {
                        val position: Long = getPositionMs()
                        if (position != previous_position) {
                            with (frame_grabber) {
                                grabNextFrame(getPositionMs())
                            }
                            current_image = frame_grabber.composeImage
                            previous_position = position
                            image_state = !image_state
                        }
                    }
                }
            }
            catch (e: Throwable) {
                if (e !is CancellationException) {
                    RuntimeException("Error during VideoPlayerFFmpeg playback, aborting", e).printStackTrace()
                }
            }
        }

        onDispose {
            state.stop()
            state.close()
        }
    }

    Canvas(modifier) {
        image_state

        val image: ImageBitmap = current_image ?: return@Canvas

        val scale_x: Float = size.width / image.width.toFloat()
        val scale_y: Float = size.height / image.height.toFloat()

        val scale: Float =
            if (fill) maxOf(scale_x, scale_y)
            else minOf(scale_x, scale_y)

        translate(
            left = if (!fill) 0f else (size.width - (image.width * scale)) / 2,
            top = if (!fill) 0f else (size.height - (image.height * scale)) / 2
        ) {
            scale(
                scale = scale,
                pivot = Offset.Zero
            ) {
                try {
                    drawImage(image, alpha = getAlpha())
                }
                catch (_: Throwable) {}
            }
        }
    }

    return remember { derivedStateOf { current_image != null } }.value
}

class FFmpegVideoPlayerState {
    private val kContext = KAVFormatContext()

    var frame_grabber: KFrameGrabber? = null
        private set

    fun open(file: String) {
        kContext.openInput(file)
    }

    fun close() {
        kContext.closeInput()
    }

    fun streams(): List<KVideoStream> = kContext.findVideoStreams()
    fun codec(stream: KVideoStream): KAVCodec = kContext.findCodec(stream)

    fun play(
        stream: KVideoStream,
        hwDecoder: KHWDecoder? = null,
        targetSize: IntSize? = null
    ) {
        frame_grabber?.close()
        frame_grabber = KFrameGrabber(stream, kContext, hwDecoder, targetSize)
    }

    fun stop() {
        frame_grabber?.close()
        frame_grabber = null
    }
}

private fun List<KHWDecoder>.getBestDecoder(): KHWDecoder? {
    val vulkan: KHWDecoder? = firstOrNull { it.name == "vulkan" }
    if (vulkan != null) {
        return vulkan
    }

    return firstOrNull()
}
