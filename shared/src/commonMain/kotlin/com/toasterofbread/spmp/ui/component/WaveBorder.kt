package com.toasterofbread.spmp.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.material.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.ui.theme.Theme
import kotlin.math.ceil
import kotlin.math.roundToInt

const val WAVE_BORDER_DEFAULT_HEIGHT: Float = 20f

@Composable
fun WaveBorder(
    modifier: Modifier = Modifier,
    colour: Color = Theme.current.background,
    height: Dp = WAVE_BORDER_DEFAULT_HEIGHT.dp,
    getOffset: ((height: Int) -> Int)? = null,
    waves: Int = 3,
    getWaveOffset: (DrawScope.() -> Float)? = null,
    border_thickness: Dp = 0.dp,
    border_colour: Color = LocalContentColor.current
) {
    Canvas(
        modifier
            .fillMaxWidth()
            .offset {
                IntOffset(
                    0,
                    getOffset?.invoke(height.toPx().roundToInt()) ?: 0
                )
            }
    ) {
        val path = Path()

        // Above equilibrium (cut out from rect)
        wavePath(path, -1, getWaveOffset, height, waves)
        clipPath(
            path,
            ClipOp.Difference
        ) {
            drawRect(
                colour,
                topLeft = Offset(0f, height.toPx() / 2)
            )
        }

        val border_stroke = if (border_thickness > 0.dp) Stroke(border_thickness.toPx()) else null

        // Upper border
        if (border_stroke != null) {
            drawPath(path, border_colour, style = border_stroke)
        }

        // Below equilibrium
        wavePath(path, 1, getWaveOffset, height, waves)
        drawPath(path, colour)

        // Lower border
        if (border_stroke != null) {
            drawPath(path, border_colour, style = border_stroke)
        }
    }
}

private fun DrawScope.wavePath(
    path: Path,
    direction: Int,
    getOffset: (DrawScope.() -> Float)?,
    height: Dp,
    waves: Int,
): Path {
    path.reset()

    val y_offset = height.toPx() / 2
    val half_period = (size.width / (waves - 1)) / 2
    val offset_px = getOffset?.invoke(this)?.let { offset ->
        offset % size.width - (if (offset > 0f) size.width else 0f)
    } ?: 0f

    path.moveTo(x = -half_period / 2 + offset_px, y = y_offset)

    for (i in 0 until ceil((size.width * 2) / half_period + 1).toInt()) {
        if ((i % 2 == 0) != (direction == 1)) {
            path.relativeMoveTo(half_period, 0f)
            continue
        }

        path.relativeQuadraticBezierTo(
            dx1 = half_period / 2,
            dy1 = height.toPx() / 2 * direction,
            dx2 = half_period,
            dy2 = 0f,
        )
    }

    return path
}
