package com.spectre7.utils.composable

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.spectre7.utils.modifier.background
import kotlin.random.Random

@Composable
fun SubtleLoadingIndicator(modifier: Modifier = Modifier, colourProvider: (() -> Color)? = null, size: Dp = 20.dp) {
	val inf_transition = rememberInfiniteTransition()
	val anim by inf_transition.animateFloat(
		initialValue = 0f,
		targetValue = 1f,
		animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutLinearInEasing),
            repeatMode = RepeatMode.Restart
        )
	)

	val rand_offset = remember { Random.nextFloat() }

    Box(Modifier.sizeIn(minWidth = size, minHeight = size).then(modifier), contentAlignment = Alignment.Center) {
        val current_anim = if (anim + rand_offset > 1f) anim + rand_offset - 1f else anim + rand_offset
        val size_percent = if (current_anim < 0.5f) current_anim else 1f - current_anim
        val content_colour = LocalContentColor.current
        Spacer(
            Modifier
                .background(CircleShape, colourProvider ?: { content_colour })
                .size(size * size_percent)
        )
    }
}
