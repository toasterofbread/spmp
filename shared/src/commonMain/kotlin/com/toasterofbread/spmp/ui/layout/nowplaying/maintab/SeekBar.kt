package com.toasterofbread.spmp.ui.layout.nowplaying.maintab

import LocalPlayerState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.krottv.compose.sliders.DefaultThumb
import com.github.krottv.compose.sliders.SliderValueHorizontal
import com.toasterofbread.composekit.utils.common.formatElapsedTime
import com.toasterofbread.composekit.utils.composable.RecomposeOnInterval
import com.toasterofbread.composekit.utils.composable.SubtleLoadingIndicator
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.PlayerState
import com.toasterofbread.spmp.ui.layout.nowplaying.POSITION_UPDATE_INTERVAL_MS
import com.toasterofbread.spmp.ui.layout.nowplaying.getNPAltOnBackground
import com.toasterofbread.spmp.ui.layout.nowplaying.getNPOnBackground

@Composable
fun SeekBar(
    seek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    getColour: PlayerState.() -> Color = { getNPOnBackground() },
    getTrackColour: PlayerState.() -> Color = { getNPAltOnBackground() }
) {
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

        Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Box(Modifier.padding(horizontal = 7.dp).requiredHeight(12.dp), contentAlignment = Alignment.BottomCenter) {
                Row(
                    Modifier.requiredHeight(30.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SeekBarTimeText(player.status.getPositionMillis(), getColour(player))
                    SeekBarTimeText(player.status.m_duration_ms, getColour(player))
                }
            }

            SliderValueHorizontal(
                value = getSliderValue(),
                onValueChange = {
                    position_override = it
                },
                onValueChangeFinished = {
                    position_override?.also {
                        seek(it)
                    }
                    old_position = player.status.getProgress()
                    cancel_area_side = null
                },
                thumbSizeInDp = DpSize(12.dp, 12.dp),
                track = { a, b, _, _, e -> SeekTrack(a, b, e, getTrackColour(player), getColour(player)) },
                thumb = { a, b, c, d, e -> DefaultThumb(a, b, c, d, e, getColour(player), 1f) }
            )
        }
    }
}

@Composable
private fun SeekBarTimeText(time: Long, colour: Color, modifier: Modifier = Modifier) {
    if (time < 0) {
        SubtleLoadingIndicator(modifier)
    }
    else {
        val seconds = time / 1000f
        Text(
            remember(seconds) { if (seconds < 0f) "" else formatElapsedTime(seconds.toLong()) },
            fontSize = 12.sp,
            fontWeight = FontWeight.Light,
            color = colour,
            modifier = modifier,
            softWrap = false
        )
    }
}

@Composable
private fun SeekTrack(
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
