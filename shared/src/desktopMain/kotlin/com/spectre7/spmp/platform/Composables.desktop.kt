@file:Suppress("UnnecessaryOptInAnnotation")

package com.spectre7.spmp.platform

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.material.*
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive

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

@OptIn(ExperimentalFoundationApi::class)
@Composable
actual fun Modifier.platformClickable(onClick: () -> Unit, onAltClick: (() -> Unit)?, indication: Indication?): Modifier {
    val ret: Modifier = this.onClick(onClick = onClick)
    if (onAltClick == null) {
        return ret
    }
    return ret.onClick(
        matcher = PointerMatcher.mouse(PointerButton.Secondary),
        onClick = onAltClick
    )
}

@OptIn(ExperimentalMaterialApi::class)
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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun BackHandler(enabled: Boolean, action: () -> Unit) {
    Box(Modifier.onKeyEvent { event ->
        if (event.key == Key.Escape) {
            action()
            true
        } else false
    })
}

@Composable
actual fun PlatformDialog(
    onDismissRequest: () -> Unit,
    use_platform_default_width: Boolean,
    dim_behind: Boolean,
    content: @Composable () -> Unit
) {
    TODO()
}

@Composable
actual fun PlatformAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier,
    dismissButton: @Composable() (() -> Unit)?,
    icon: @Composable() (() -> Unit)?,
    title: @Composable() (() -> Unit)?,
    text: @Composable() (() -> Unit)?,
    shape: Shape,
    containerColor: Color,
    iconContentColor: Color,
    titleContentColor: Color,
    textContentColor: Color
) {
    TODO()
}

@Composable
actual fun PlatformAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier,
    dismissButton: @Composable() (() -> Unit)?,
    icon: @Composable() (() -> Unit)?,
    title: @Composable() (() -> Unit)?,
    text: @Composable() (() -> Unit)?
) {
    TODO()
}

@Composable
actual fun SwipeRefresh(
    state: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier,
    swipe_enabled: Boolean,
    content: @Composable () -> Unit
) {
    Box(modifier) {
        content()
    }
}

@Composable
actual fun rememberImagePainter(url: String): Painter = com.lt.load_the_image.rememberImagePainter(url)
