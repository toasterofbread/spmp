package com.toasterofbread.spmp.ui.layout.nowplaying.maintab

@Composable
private fun SeekBarTimeText(time: Long, colour: Color) {
    val seconds = time / 1000f
    Text(
        remember(seconds) { if (seconds < 0f) "" else formatElapsedTime(seconds.toLong()) },
        fontSize = 10.sp,
        fontWeight = FontWeight.Light,
        color = colour
    )
}

@Composable
private fun SeekBar(seek: (Float) -> Unit) {
    val player = LocalPlayerState.current

    var position_override by remember { mutableStateOf<Float?>(null) }
    var old_position by remember { mutableStateOf<Float?>(null) }
    var cancel_area_side: Int? by remember { mutableStateOf(null) }

    fun getSliderValue(): Float {
        if (position_override != null && old_position != null) {
            if (player.status.getProgress() != old_position) {
                old_position = null
                position_override = null
            }
            else {
                return position_override!!
            }
        }
        return position_override ?: player.status.getProgress()
    }

    RecomposeOnInterval(POSITION_UPDATE_INTERVAL_MS, player.status.m_playing) { state ->
        state

        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 7.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                SeekBarTimeText(player.status.getPositionMillis(), getNPOnBackground())
                SeekBarTimeText(player.status.m_duration_ms, getNPOnBackground())
            }

            SliderValueHorizontal(
                value = getSliderValue(),
                onValueChange = {
                    position_override = it
                },
                onValueChangeFinished = {
                    seek(position_override!!)
                    old_position = player.status.getProgress()
                    cancel_area_side = null
                },
                thumbSizeInDp = DpSize(12.dp, 12.dp),
                track = { a, b, _, _, e -> SeekTrack(a, b, e, getNPAltOnBackground(), getNPOnBackground()) },
                thumb = { a, b, c, d, e -> DefaultThumb(a, b, c, d, e, getNPOnBackground(), 1f) }
            )
        }
    }
}

@Composable
fun SeekTrack(
    modifier: Modifier,
    progress: Float,
    enabled: Boolean,
    track_colour: Color,
    progress_colour: Color,
    height: Dp = 4.dp
) {
    val visual_progress by animateFloatAsState(progress, spring(stiffness = Spring.StiffnessLow))

    Canvas(
        Modifier
            .then(modifier)
            .height(height)
    ) {
        val left = Offset(0f, center.y)
        val right = Offset(size.width, center.y)
        val start = if (layoutDirection == LayoutDirection.Rtl) right else left
        val end = if (layoutDirection == LayoutDirection.Rtl) left else right

        val progress_width = (end.x - start.x) * visual_progress

        drawLine(
            Brush.horizontalGradient(
                listOf(progress_colour, track_colour),
                startX = progress_width,
                endX = progress_width + ((size.width - progress_width) * SEEK_BAR_GRADIENT_OVERFLOW_RATIO)
            ),
            start,
            end,
            size.height,
            StrokeCap.Round,
            alpha = if (enabled) 1f else 0.6f
        )

        drawLine(
            progress_colour,
            Offset(
                start.x,
                center.y
            ),
            Offset(
                start.x + progress_width,
                center.y
            ),
            size.height,
            StrokeCap.Round,
            alpha = if (enabled) 1f else 0.6f
        )
    }
}
