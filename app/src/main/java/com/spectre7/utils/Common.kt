package com.spectre7.utils

// TODO | Move to separate repository

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import android.os.VibrationEffect
import android.os.VibratorManager
import android.widget.Toast
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material.ripple.RippleTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import com.spectre7.spmp.MainActivity
import kotlinx.coroutines.delay

fun Boolean.toInt() = if (this) 1 else 0

fun vibrate(duration: Double) {
    val vibrator = (MainActivity.context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
    vibrator.vibrate(VibrationEffect.createOneShot((duration * 1000.0).toLong(), VibrationEffect.DEFAULT_AMPLITUDE))
}

fun vibrateShort() {
    vibrate(0.01)
}

fun sendToast(text: String, length: Int = Toast.LENGTH_SHORT, context: Context = MainActivity.context) {
    try {
        Toast.makeText(context, text, length).show()
    }
    catch (_: NullPointerException) {
        Looper.prepare()
        Toast.makeText(context, text, length).show()
    }
}

fun getString(id: Int, context: Context = MainActivity.context): String {
    return context.resources.getString(id)
}

@Composable
fun NoRipple(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalRippleTheme provides object : RippleTheme {
        @Composable
        override fun defaultColor() = Color.Unspecified

        @Composable
        override fun rippleAlpha(): RippleAlpha = RippleAlpha(0.0f,0.0f,0.0f,0.0f)
    }) {
        content()
    }
}

@Composable
fun OnChangedEffect(key: Any?, block: suspend () -> Unit) {
    var launched by remember { mutableStateOf(false) }
    LaunchedEffect(key) {
        if (!launched) {
            launched = true
        }
        else {
            block()
        }
    }
}

fun getAppName(context: Context): String {
    val info = context.applicationInfo
    val string_id = info.labelRes
    return if (string_id == 0) info.nonLocalizedLabel.toString() else context.getString(string_id)
}

@Composable
fun MeasureUnconstrainedView(
    viewToMeasure: @Composable () -> Unit,
    content: @Composable (width: Int, height: Int) -> Unit,
) {
    SubcomposeLayout { constraints ->
        val measurement = subcompose("viewToMeasure", viewToMeasure)[0].measure(Constraints())

        val contentPlaceable = subcompose("content") {
            content(measurement.width, measurement.height)
        }[0].measure(constraints)

        layout(contentPlaceable.width, contentPlaceable.height) {
            contentPlaceable.place(0, 0)
        }
    }
}

@SuppressLint("InternalInsetResource")
@Composable
fun getStatusBarHeight(context: Context): Dp {
    val resource_id: Int = context.resources.getIdentifier("status_bar_height", "dimen", "android")
    if (resource_id > 0) {
        with(LocalDensity.current) {
            return context.resources.getDimensionPixelSize(resource_id).toDp()
        }
    }
    throw RuntimeException()
}

@Composable
fun Marquee(autoscroll: Boolean = true, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    MeasureUnconstrainedView(viewToMeasure = content) { content_width: Int, _ ->
        val scroll_state = rememberScrollState()

        if (autoscroll) {
            var animation_state by remember {
                mutableStateOf(true)
            }

            LaunchedEffect(key1 = animation_state){
                scroll_state.animateScrollTo(
                    scroll_state.maxValue,
                    animationSpec = tween(1000000 / content_width, 200, easing = CubicBezierEasing(0f,0f,0f,0f))
                )
                delay(2000)
                scroll_state.animateScrollTo(
                    0,
                    animationSpec = tween(500, 200, easing = CubicBezierEasing(0f,0f,0f,0f))
                )
                delay(2000)
                animation_state = !animation_state
            }

            Row(modifier.horizontalScroll(scroll_state, false)) {
                content()
            }
        }
        else {
            val density = LocalDensity.current
            var container_width by remember { mutableStateOf(0) }

            LaunchedEffect(scroll_state.isScrollInProgress) {
                val max_scroll = content_width - container_width
                if (scroll_state.value > max_scroll) {
                    scroll_state.scrollTo(max_scroll)
                }
            }

            val scroll_value by derivedStateOf { with (density) {
                if (container_width >= content_width) {
                    0.dp
                }
                else {
                    (-scroll_state.value).coerceIn(container_width - content_width, 0).toDp()
                }
            } }

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
                        .requiredWidth(with (density) { container_width.toDp() - scroll_value })
                        .offset(scroll_value / 2)
                ) {
                    content()
                }
            }
        }
    }
}
