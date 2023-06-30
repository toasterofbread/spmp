@file:OptIn(ExperimentalMaterialApi::class)

package com.toasterofbread.spmp.platform.composable

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeableState
import androidx.compose.material.ThresholdConfig
import androidx.compose.material.swipeable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive

actual fun Modifier.scrollWheelSwipeable(
    state: SwipeableState<Int>,
    anchors: Map<Float, Int>,
    thresholds: (from: Int, to: Int) -> ThresholdConfig,
    orientation: Orientation,
    reverseDirection: Boolean
): Modifier = composed {
    return@composed mouseWheelInput { direction ->
        val target = state.targetValue + (if (reverseDirection) -direction else direction)
        if (anchors.values.contains(target)) {
            state.animateTo(target)
        }
        return@mouseWheelInput true
    }.swipeable(state = state, anchors = anchors, thresholds = thresholds, orientation = orientation, reverseDirection = reverseDirection)
}

private inline val PointerEvent.isConsumed: Boolean get() = changes.any { c: PointerInputChange -> c.isConsumed }
private inline fun PointerEvent.consume() = changes.forEach { c: PointerInputChange -> c.consume() }

private suspend fun AwaitPointerEventScope.awaitScrollEvent(): PointerEvent {
    var event: PointerEvent
    do {
        event = awaitPointerEvent()
    } while ((event.type as PointerEventType) != PointerEventType.Scroll)
    return event
}

private fun Modifier.mouseWheelInput(
    onMouseWheel: suspend (direction: Int) -> Boolean
) = pointerInput(Unit) {
    coroutineScope {
        while (isActive) {
            val event = awaitPointerEventScope {
                awaitScrollEvent()
            }
            if (!event.isConsumed) {
                val change: PointerInputChange = event.changes.first()
                val consumed = onMouseWheel(change.scrollDelta.y.toInt())
                if (consumed) {
                    event.consume()
                }
            }
        }
    }
}
