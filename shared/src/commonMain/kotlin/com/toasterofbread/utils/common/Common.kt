@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.toasterofbread.utils.common

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeableState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.*
import androidx.compose.ui.unit.*
import com.google.gson.Gson
import com.toasterofbread.spmp.ProjectBuildConfig
import com.toasterofbread.spmp.resources.*
import com.toasterofbread.spmp.youtubeapi.fromJson
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

fun Boolean.toInt() = if (this) 1 else 0
fun Boolean.toFloat() = if (this) 1f else 0f

fun isDebugBuild(): Boolean = ProjectBuildConfig.IS_DEBUG

fun getInnerSquareSizeOfCircle(radius: Float, corner_percent: Int): Float {
	val C = 1.0 - (corner_percent * 0.02)
	val E = (sqrt(8.0 * radius * radius) / 2.0) - radius
	val I = radius + (E * C)
	return sqrt(I * I * 0.5).toFloat()
}

inline fun lazyAssert(noinline getMessage: (() -> String)? = null, condition: () -> Boolean) {
	if (_Assertions.ENABLED && !condition()) {
		throw AssertionError(getMessage?.invoke() ?: "Assertion failed")
	}
}

expect fun log(message: Any?)

fun printJson(data: String, base_gson: Gson? = null) {
	val gson = base_gson ?: Gson()

	try {
		log(
			gson.toJson(
				gson.fromJson<Map<String, Any?>>(data.reader())
			)
		)
	}
	catch (_: Exception) {
		try {
			log(
				gson.toJson(
					gson.fromJson<Array<Any?>>(data.reader())
				)
			)
		}
		catch (e: Exception) {
			log(data)
			throw e
		}
	}
}

@Composable
fun PaddingValues.copy(
	start: Dp? = null,
	top: Dp? = null,
	end: Dp? = null,
	bottom: Dp? = null,
): PaddingValues {
	return PaddingValues(
		start ?: calculateStartPadding(LocalLayoutDirection.current),
		top ?: calculateTopPadding(),
		end ?: calculateEndPadding(LocalLayoutDirection.current),
		bottom ?: calculateBottomPadding()
	)
}

fun PaddingValues.copy(
	layout_direction: LayoutDirection,
	start: Dp? = null,
	top: Dp? = null,
	end: Dp? = null,
	bottom: Dp? = null,
): PaddingValues {
	return PaddingValues(
		start ?: calculateStartPadding(layout_direction),
		top ?: calculateTopPadding(),
		end ?: calculateEndPadding(layout_direction),
		bottom ?: calculateBottomPadding()
	)
}

fun Modifier.thenIf(condition: Boolean, modifier: Modifier): Modifier = if (condition) then(modifier) else this
@Composable
fun Modifier.thenIf(condition: Boolean, modifierProvider: @Composable Modifier.() -> Modifier): Modifier = if (condition) modifierProvider() else this

fun <T> MutableList<T>.addUnique(item: T): Boolean {
	if (!contains(item)) {
		add(item)
		return true
	}
	return false
}

@OptIn(ExperimentalMaterialApi::class)
fun <T> SwipeableState<T>.init(anchors: Map<Float, T>) {
	ensureInit(anchors)
}

operator fun IntSize.times(other: Float): IntSize =
	IntSize(width = (width * other).toInt(), height = (height * other).toInt())

fun formatElapsedTime(seconds: Long): String {
	val hours = TimeUnit.SECONDS.toHours(seconds)
	val minutes = TimeUnit.SECONDS.toMinutes(seconds) % 60
	val remaining_seconds = seconds % 60
	return if (hours > 0) {
		String.format("%d:%02d:%02d", hours, minutes, remaining_seconds)
	} else {
		String.format("%02d:%02d", minutes, remaining_seconds)
	}
}

fun <K, V : Any> MutableMap<K, V>.putIfAbsent(key: K, getValue: () -> V): V {
	var v = this[key]
	if (v == null) {
		v = getValue()
		put(key, v)
	}
	return v
}

fun String.indexOfOrNull(string: String, start_index: Int = 0, ignore_case: Boolean = false): Int? =
	indexOf(string, start_index, ignore_case).takeIf { it != -1 }

fun String.indexOfOrNull(char: Char, start_index: Int = 0, ignore_case: Boolean = false): Int? =
	indexOf(char, start_index, ignore_case).takeIf { it != -1 }

fun String.indexOfFirstOrNull(start: Int = 0, predicate: (Char) -> Boolean): Int? {
	for (i in start until length) {
		if (predicate(elementAt(i))) {
			return i
		}
	}
	return null
}

fun CoroutineScope.launchSingle(
	context: CoroutineContext = EmptyCoroutineContext,
	start: CoroutineStart = CoroutineStart.DEFAULT,
	block: suspend CoroutineScope.() -> Unit
): Job {
	synchronized(this) {
		coroutineContext.cancelChildren()
		return launch(context, start, block)
	}
}

fun Float.roundTo(decimals: Int): Float {
	val multiplier = 10f.pow(decimals)
	return (this * multiplier).roundToInt() / multiplier
}

inline fun synchronizedBlock(lock: Any, block: () -> Unit) {
	synchronized(lock, block)
}

fun String.substringBetween(start: String, end: String, ignore_case: Boolean = false): String? {
	val start_index = indexOf(start, ignoreCase = ignore_case) + start.length
	if (start_index < start.length) {
		return null
	}
	val end_index = indexOf(end, start_index, ignoreCase = ignore_case)
	if (end_index == -1) {
		return null
	}

	return substring(start_index, end_index)
}
