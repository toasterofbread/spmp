package com.toasterofbread.utils.composable

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.toasterofbread.utils.modifier.background
import com.toasterofbread.utils.setAlpha
import kotlin.random.Random

@Composable
fun SubtleLoadingIndicator(modifier: Modifier = Modifier, message: String? = null, size: Dp = 20.dp, getColour: (() -> Color)? = null) {
	val random_offset = remember { Random.nextFloat() }
	val inf_transition = rememberInfiniteTransition()
	val anim by inf_transition.animateFloat(
		initialValue = 0f,
		targetValue = 1f,
		animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutLinearInEasing),
            repeatMode = RepeatMode.Restart
        )
	)

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Box(Modifier.sizeIn(minWidth = size, minHeight = size).then(modifier), contentAlignment = Alignment.Center) {
            val current_anim = if (anim + random_offset > 1f) anim + random_offset - 1f else anim + random_offset
            val size_percent = if (current_anim < 0.5f) current_anim else 1f - current_anim
            val content_colour = LocalContentColor.current
            Spacer(
                Modifier
                    .background(CircleShape, getColour ?: { content_colour })
                    .size(size * size_percent)
            )
        }

        if (message != null) {
            Text(message, style = MaterialTheme.typography.labelLarge, color = LocalContentColor.current.setAlpha(0.85f))
        }
    }
}
