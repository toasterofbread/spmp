package com.toasterofbread.utils.composable

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.math.roundToInt

@Composable
fun pauseableInfiniteRepeatableAnimation(
    start: Float,
    end: Float,
    period: Int,
    getPlaying: () -> Boolean
): State<Float> {
    val playing = getPlaying()
    var animatable by remember { mutableStateOf(Animatable(start)) }
    var paused_animatable_position by remember { mutableStateOf(0) }

    LaunchedEffect(playing) {
        if (playing) {
            animatable.animateTo(
                end,
                infiniteRepeatable(
                    animation = tween(period, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                    initialStartOffset = StartOffset(
                        paused_animatable_position, 
                        StartOffsetType.FastForward
                    )
                )
            )
        }
        else {
            paused_animatable_position = ((animatable.value - start) / (end - start)).roundToInt() * period
            animatable = Animatable(0f)
        }
    }

    return animatable.asState()
}
