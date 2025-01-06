package com.toasterofbread.spmp.ui.util

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.ceil

@Deprecated("Legacy")
data class WaveShape(
    val waves: Int,
    val offset: Float,
    val invert: Boolean = false,
    val width_multiplier: Float = 1f
): Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path: Path = Path()

        path.addRect(Rect(0f, 0f, size.width, size.height / 2))

        wavePath(
            path = path,
            size = size,
            waves = waves,
            width_multiplier = width_multiplier,
            offset = offset
        )

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

@Deprecated("Legacy")
fun wavePath(
    path: Path,
    size: Size,
    waves: Int,
    width_multiplier: Float,
    offset: Float = 0f,
    fill_direction: Int = 0
): Path {
    val y_offset: Float = size.height / 2
    val half_period: Float = size.width / waves
    val offset_px: Float = (offset % (size.width)) - (if (offset > 0f) size.width else 0f)

    if (fill_direction != 0) {
        path.moveTo(x = offset_px, y = if (fill_direction == 1) 0f else size.height)
        path.lineTo(x = offset_px, y = y_offset)
    }

    path.moveTo(x = offset_px, y = y_offset)

    for (i in 0 until ceil((size.width * width_multiplier) / (half_period + 1)).toInt()) {
        for (direction in listOf(-1, 1)) {
            path.relativeQuadraticTo(
                dx1 = half_period / 2,
                dy1 = size.height / 2 * direction,
                dx2 = half_period,
                dy2 = 0f
            )
        }
    }

    if (fill_direction != 0) {
        path.relativeLineTo(0f, if (fill_direction == 1) -size.height else size.height)
        path.lineTo(x = offset_px, y = if (fill_direction == 1) 0f else size.height)
    }

    return path
}
