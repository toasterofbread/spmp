package com.toasterofbread.utils.composable

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun Marquee(modifier: Modifier = Modifier, arrangement: Arrangement.Horizontal = Arrangement.Start, content: @Composable () -> Unit) {
    MeasureUnconstrainedView(content) { content_width: Int, _ ->
        val scroll_state = rememberScrollState()

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
