package com.spectre7.utils.composable

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun Marquee(modifier: Modifier = Modifier, autoscroll: Boolean = false, arrangement: Arrangement.Horizontal = Arrangement.Start, content: @Composable () -> Unit) {
    MeasureUnconstrainedView(content) { content_width: Int, _ ->
        val scroll_state = rememberScrollState()

        if (autoscroll) {
            var animation_state by remember {
                mutableStateOf(true)
            }

            LaunchedEffect(key1 = animation_state) {
                scroll_state.animateScrollTo(
                    scroll_state.maxValue,
                    animationSpec = tween(1000000 / content_width, 200, easing = CubicBezierEasing(0f, 0f, 0f, 0f))
                )
                delay(2000)
                scroll_state.animateScrollTo(
                    0,
                    animationSpec = tween(500, 200, easing = CubicBezierEasing(0f, 0f, 0f, 0f))
                )
                delay(2000)
                animation_state = !animation_state
            }

            Row(modifier.horizontalScroll(scroll_state, false), horizontalArrangement = arrangement) {
                content()
            }
        } else {
            val density = LocalDensity.current
            var container_width by remember { mutableStateOf(0) }

            LaunchedEffect(scroll_state.isScrollInProgress) {
                val max_scroll = content_width - container_width
                if (scroll_state.value > max_scroll) {
                    scroll_state.scrollTo(max_scroll)
                }
            }

            val scroll_value by remember {
                derivedStateOf {
                    with(density) {
                        if (container_width >= content_width) {
                            0.dp
                        } else {
                            (-scroll_state.value).coerceIn(container_width - content_width, 0).toDp()
                        }
                    }
                }
            }

            Row(
                modifier
                    .scrollable(
                        scroll_state,
                        Orientation.Horizontal,
                        reverseDirection = true
                    )
                    .fillMaxWidth()
                    .clipToBounds()
                    .onSizeChanged {
                        container_width = it.width
                    }
            ) {
                Row(
                    Modifier
                        .requiredWidth(with(density) { container_width.toDp() - scroll_value })
                        .offset { IntOffset((scroll_value / 2).toPx().roundToInt(), 0) },
                    horizontalArrangement = arrangement
                ) {
                    content()
                }
            }
        }
    }
}
