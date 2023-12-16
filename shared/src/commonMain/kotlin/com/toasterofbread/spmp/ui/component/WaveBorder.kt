package com.toasterofbread.spmp.ui.component

import LocalPlayerState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.material.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.toasterofbread.composekit.settings.ui.Theme
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.PlayerState
import kotlin.math.ceil

const val WAVE_BORDER_HEIGHT_DP: Float = 20f

data class WaveShape(
    val waves: Int,
    val offset: Float,
    val invert: Boolean = false,
    val width_multiplier: Float = 1f
): Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path: Path = Path()

        path.addRect(Rect(0f, 0f, size.width, size.height / 2))

        wavePath(path, size, 1, waves, width_multiplier) { offset }
        wavePath(path, size, -1, waves, width_multiplier) { offset }

        if (invert) {
            path.transform(
                Matrix().apply {
                    scale(y = -1f)
                    translate(y = -size.height)
                }
            )
        }

        return Outline.Generic(path)
    }
}

@Composable
fun WaveBorder(
    modifier: Modifier = Modifier,
    getColour: Theme.() -> Color = { background },
    height: Dp = WAVE_BORDER_HEIGHT_DP.dp,
    getOffset: ((height: Float) -> Float)? = null,
    waves: Int = 3,
    invert: Boolean = false,
    width_multiplier: Float = 1f,
    getWaveOffset: (Density.() -> Float)? = null,
    border_thickness: Dp = 0.dp,
    border_colour: Color = LocalContentColor.current
) {
    val player: PlayerState = LocalPlayerState.current
    val density: Density = LocalDensity.current

    val colour: Color = getColour(player.theme)
    val offset: Float? = getWaveOffset?.invoke(density)
    val shape: WaveShape = remember(waves, offset, width_multiplier) {
        WaveShape(waves, offset ?: 0f, invert = invert, width_multiplier = width_multiplier)
    }

    Box(modifier.requiredHeight(0.dp)) {
        Box(
            Modifier
                .matchParentSize()
                .requiredHeight(height)
                .offset(
                    0.dp,
                    with(density) {
                        val user_offset: Dp = getOffset?.invoke(height.toPx())?.toDp()
                            ?: (
                                if (invert) (-height / 2) + 0.2.dp
                                else (height / 2) - 0.2.dp
                            )
                        user_offset + border_thickness
                    }
                )
                .background(border_colour, shape)
                .offset(0.dp, -border_thickness)
                .background(colour, shape)
        )
    }
}

inline fun DrawScope.drawWave(
    waves: Int,
    wave_size: Size = size,
    stroke_width: Float = 2f,
    width_multiplier: Float = 1f,
    getWaveOffset: () -> Float,
    getColour: () -> Color,
) {
    val path: Path = Path()
    val colour: Color = getColour()
    val stroke: Stroke = Stroke(stroke_width)

    // Above equilibrium
    wavePath(path, wave_size, -1, waves, width_multiplier, getWaveOffset)
    drawPath(path, colour, style = stroke)
    path.reset()

    // Below equilibrium
    wavePath(path, wave_size, 1, waves, width_multiplier, getWaveOffset)
    drawPath(path, colour, style = stroke)
}

inline fun wavePath(
    path: Path,
    size: Size,
    direction: Int,
    waves: Int,
    width_multiplier: Float,
    getOffset: () -> Float
): Path {
    val y_offset: Float = size.height / 2
    val half_period: Float = size.width / waves
    val offset_px: Float = getOffset().let { offset ->
        offset % size.width - (if (offset > 0f) size.width else 0f)
    }

    path.moveTo(x = offset_px, y = y_offset)

    for (i in 0 until ceil((size.width * width_multiplier) / (half_period + 1)).toInt()) {
        if ((i % 2 == 0) != (direction == 1)) {
            path.relativeMoveTo(half_period, 0f)
            continue
        }

        path.relativeQuadraticBezierTo(
            dx1 = half_period / 2,
            dy1 = size.height / 2 * direction,
            dx2 = half_period,
            dy2 = 0f,
        )
    }

    return path
}
