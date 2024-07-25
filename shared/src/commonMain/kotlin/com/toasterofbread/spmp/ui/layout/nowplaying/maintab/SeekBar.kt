package com.toasterofbread.spmp.ui.layout.nowplaying.maintab

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
import dev.toastbits.composekit.utils.common.formatElapsedTime
import dev.toastbits.composekit.utils.composable.RecomposeOnInterval
import dev.toastbits.composekit.utils.composable.SubtleLoadingIndicator
import LocalAppState
import LocalSessionState
import LocalUiState
import com.toasterofbread.spmp.model.state.SessionState
import com.toasterofbread.spmp.model.state.UiState
import com.toasterofbread.spmp.model.state.UiStateImpl
import com.toasterofbread.spmp.ui.layout.nowplaying.POSITION_UPDATE_INTERVAL_MS
import com.toasterofbread.spmp.ui.layout.nowplaying.getNPAltOnBackground
import com.toasterofbread.spmp.ui.layout.nowplaying.getNPOnBackground
import dev.toastbits.ytmkt.uistrings.UiString

@Composable
fun SeekBar(
    seek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    getColour: UiState.() -> Color = { getNPOnBackground() },
    getTrackColour: UiState.() -> Color = { getNPAltOnBackground() }
) {
    val session_state: SessionState = LocalSessionState.current
    val ui_state: UiState = LocalUiState.current

    var position_override: Float? by remember { mutableStateOf<Float?>(null) }
    var old_position: Float? by remember { mutableStateOf<Float?>(null) }

    fun getSliderValue(): Float {
        if (position_override != null && old_position != null) {
            if (session_state.status.getProgress() != old_position) {
                old_position = null
                position_override = null
            }
            else {
                return position_override!!
            }
        }
        return position_override ?: session_state.status.getProgress()
    }

    RecomposeOnInterval(POSITION_UPDATE_INTERVAL_MS, session_state.status.m_playing) { state ->
        state

        Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Box(Modifier.padding(horizontal = 7.dp).requiredHeight(12.dp), contentAlignment = Alignment.BottomCenter) {
                Row(
                    Modifier.requiredHeight(30.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SeekBarTimeText(session_state.status.getPositionMs(), getColour(ui_state))
                    SeekBarTimeText(session_state.status.m_duration_ms, getColour(ui_state))
                }
            }

            SliderValueHorizontal(
                value = getSliderValue(),
                enabled = enabled,
                onValueChange = {
                    position_override = it
                },
                onValueChangeFinished = {
                    position_override?.also {
                        seek(it)
                    }
                    old_position = session_state.status.getProgress()
                },
                thumbSizeInDp = DpSize(12.dp, 12.dp),
                track = { a, b, _, _, e -> SeekTrack(a, b, e, getTrackColour(ui_state), getColour(ui_state)) },
                thumb = { a, b, c, d, e -> DefaultThumb(a, b, c, d, e, getColour(ui_state), 1f) }
            )
        }
    }
}

@Composable
private fun SeekBarTimeText(time: Long, colour: Color, modifier: Modifier = Modifier) {
    if (time < 0) {
        SubtleLoadingIndicator(modifier, getColour = { colour })
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
    val state: SpMp.State = LocalAppState.current
    val visual_progress by animateFloatAsState(progress, spring(stiffness = Spring.StiffnessLow))
    val show_gradient: Boolean by state.settings.state.SHOW_SEEK_BAR_GRADIENT.observe()

    Canvas(
        Modifier
            .then(modifier)
            .height(height)
    ) {
        val left: Offset = Offset(0f, center.y)
        val right: Offset = Offset(size.width, center.y)
        val start: Offset = if (layoutDirection == LayoutDirection.Rtl) right else left
        val end: Offset = if (layoutDirection == LayoutDirection.Rtl) left else right

        val progress_width: Float = (end.x - start.x) * visual_progress

        drawLine(
            Brush.horizontalGradient(
                listOf(progress_colour, track_colour),
                startX = progress_width,
                endX = progress_width + (if (show_gradient) (size.width - progress_width) * SEEK_BAR_GRADIENT_OVERFLOW_RATIO else 0f)
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
