package com.toasterofbread.utils.composable

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.toasterofbread.utils.toFloat

fun Modifier.crossOut(
    crossed_out: Boolean,
    colourProvider: () -> Color,
    width: Dp = 2.dp,
    getSize: ((IntSize) -> IntSize)? = null,
): Modifier = composed {
	val line_visibility = remember { Animatable(crossed_out.toFloat()) }
    OnChangedEffect(crossed_out) {
        line_visibility.animateTo(crossed_out.toFloat())
    }

	var size by remember { mutableStateOf(IntSize.Zero) }
	var actual_size by remember { mutableStateOf(IntSize.Zero) }

	val density = LocalDensity.current

	this
		.onSizeChanged {
			size = getSize?.invoke(it) ?: it
			actual_size = it
		}
		.drawBehind {
			val offset = Offset(
                (actual_size.width - size.width) * 0.5f,
                (actual_size.height - size.height) * 0.5f
            )

			drawLine(
				colourProvider(),
				offset,
                Offset(
                    size.width * line_visibility.value + offset.x,
                    size.height * line_visibility.value + offset.y
                ),
				with (density) { width.toPx() }
			)
		}
}
