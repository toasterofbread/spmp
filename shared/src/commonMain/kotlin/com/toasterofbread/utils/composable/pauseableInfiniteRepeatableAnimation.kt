package com.toasterofbread.utils.composable

@Composable
fun pauseableInfiniteRepeatableAnimation(
    start: Float,
    end: Float,
    period: Long,
    getPlaying: () -> Boolean
): State<Float> {
    val playing = getPlaying()
    var animatable by remember { mutableStateOf(Animatable(0f)) }
    var paused_animatable_position by remember { mutableStateOf(0f) }

    LaunchedEffect(playing) {
        if (playing) {
            animatable.animateTo(
                start,
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
            paused_animatable_position = ((animatable.value - start) / (end - start)) * period
            animatable = Animatable(0f)
        }
    }

    return animatable.asState()
}
