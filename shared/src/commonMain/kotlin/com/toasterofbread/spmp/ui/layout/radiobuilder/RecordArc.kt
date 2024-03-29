package com.toasterofbread.spmp.ui.layout.radiobuilder

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.toastbits.ytmkt.endpoint.RadioBuilderModifier

@Composable
internal fun RecordArc(size: Dp, arc_angle: Float, offset: Float = 0f, colour: Color, anim: Animatable<Float, AnimationVector1D>? = null) {
    val density = LocalDensity.current
    for (direction in listOf(-1, 1)) {
        val o = if (direction == -1) 180f else 0f
        Box(Modifier
            .size(size)
            .drawBehind {
                drawArc(
                    colour.copy(alpha = 0.5f),
                    (arc_angle * -0.5f) + o + ((anim?.value ?: 0f) * direction) + offset,
                    arc_angle,
                    false,
                    style = Stroke(with(density) { 2.dp.toPx() })
                )
            })
    }
}

internal fun getRecordArcValues(type: RadioBuilderModifier.SelectionType, i: Int): Triple<Dp, Float, Float> {
    return when (type) {
        RadioBuilderModifier.SelectionType.FAMILIAR -> Triple((20f + 10f * i).dp, 40f + 20f * i, 0f)
        RadioBuilderModifier.SelectionType.BLEND -> Triple((20f + 10f * i).dp, 35f + 15f * i, -20f + i * 20f)
        RadioBuilderModifier.SelectionType.DISCOVER -> Triple((20f + 10f * i).dp, 40f + 25f * i, -25f + i * 35f)
    }
}
