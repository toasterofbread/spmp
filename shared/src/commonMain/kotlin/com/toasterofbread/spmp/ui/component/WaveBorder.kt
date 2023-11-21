package com.toasterofbread.spmp.ui.component

import LocalPlayerState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.material.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
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
import kotlin.math.roundToInt

const val WAVE_BORDER_HEIGHT_DP: Float = 20f

class WaveShape(val waves: Int, val offset: Float): Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path: Path = Path()

        path.addRect(Rect(0f, 0f, size.width, size.height / 2))

        wavePath(path, size, 1, waves) { offset }
        wavePath(path, size, -1, waves) { offset }

        return Outline.Generic(path)
    }
}

@Composable
fun WaveBorder(
    modifier: Modifier = Modifier,
    getColour: Theme.() -> Color = { background },
    height: Dp = WAVE_BORDER_HEIGHT_DP.dp,
    getOffset: ((height: Int) -> Int)? = null,
    waves: Int = 3,
    getWaveOffset: (Density.() -> Float)? = null,
    border_thickness: Dp = 0.dp,
    border_colour: Color = LocalContentColor.current
) {
    val player: PlayerState = LocalPlayerState.current
    val density: Density = LocalDensity.current

    val colour: Color = getColour(player.theme)
    val offset: Float? = getWaveOffset?.invoke(density)
    val shape: WaveShape = remember(waves, offset) {
        WaveShape(waves, offset ?: 0f)
    }

    Box(modifier.fillMaxWidth().requiredHeight(0.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .requiredHeight(height)
                .offset(
                    0.dp,
                    with(density) {
                        (getOffset?.invoke(height.toPx().roundToInt())?.toDp() ?: 0.dp) + (height / 2) - 1.toDp() + border_thickness
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
    getWaveOffset: () -> Float,
    getColour: () -> Color,
) {
    val path = Path()
    val colour = getColour()
    val stroke = Stroke(stroke_width)

    // Above equilibrium
    wavePath(path, wave_size, -1, waves, getWaveOffset)
    drawPath(path, colour, style = stroke)
    path.reset()

    // Below equilibrium
    wavePath(path, wave_size, 1, waves, getWaveOffset)
    drawPath(path, colour, style = stroke)
}

inline fun wavePath(
    path: Path,
    size: Size,
    direction: Int,
    waves: Int,
    getOffset: () -> Float
): Path {
    val y_offset: Float = size.height / 2
    val half_period: Float = (size.width / (waves - 1)) / 2
    val offset_px: Float = getOffset().let { offset ->
        offset % size.width - (if (offset > 0f) size.width else 0f)
    }

    path.moveTo(x = -half_period / 2 + offset_px, y = y_offset)

    for (i in 0 until ceil((size.width * 2) / half_period + 1).toInt()) {
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
