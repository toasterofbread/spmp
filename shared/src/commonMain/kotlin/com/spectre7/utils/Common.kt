@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.toasterofbread.utils

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
import com.beust.klaxon.Klaxon
import com.toasterofbread.spmp.ProjectBuildConfig
import com.toasterofbread.spmp.resources.*
import kotlinx.coroutines.*
import java.io.InterruptedIOException
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

fun areAssertionsEnabled(): Boolean {
	try {
		assert(false)
		return false
	} catch (e: AssertionError) {
		return true
	}
}

fun lazyAssert(message: String? = "Assertion failed", condition: () -> Boolean) {
	if (areAssertionsEnabled() && !condition()) {
		throw AssertionError(message)
	}
}

expect fun log(message: Any?)

fun printJson(data: String, klaxon: Klaxon? = null) {
	try {
		log((klaxon ?: Klaxon()).parseJsonObject(data.reader()).toJsonString(true))
	}
	catch (_: Exception) {
		try {
			log((klaxon ?: Klaxon()).parseJsonArray(data.reader()).toJsonString(true))
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

fun Modifier.thenIf(condition: Boolean, modifier: Modifier): Modifier = if (condition) then(modifier) else this
fun Modifier.thenIf(condition: Boolean, modifierProvider: () -> Modifier): Modifier = if (condition) then(modifierProvider()) else this

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

class ValueListeners<T>(private val list: MutableList<(T) -> Unit> = mutableListOf()) {
	fun call(value: T) {
		list.forEach { it(value) }
	}

	fun add(value: (T) -> Unit) {
		list.add(value)
	}

	fun remove(value: (T) -> Unit) {
		val iterator = list.iterator()
		while (iterator.hasNext()) {
			val item = iterator.next()
			if (item == value) {
				iterator.remove()
				break
			}
		}
	}
}

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

fun catchInterrupts(action: () -> Unit): Boolean {
	try {
		action()
	}
	catch (error: Exception) {
		for (e in listOf(error, error.cause)) {
			if (e is InterruptedException || e is InterruptedIOException) {
				return true
			}
		}
		throw RuntimeException(error)
	}
	return false
}

fun String.indexOfOrNull(string: String, start_index: Int = 0, ignore_case: Boolean = false): Int? {
	val index = indexOf(string, start_index, ignore_case)
	return if (index == -1) null else index
}

fun String.indexOfFirstOrNull(start: Int = 0, predicate: (Char) -> Boolean): Int? {
	for (i in start until length) {
		if (predicate(elementAt(i))) {
			return i
		}
	}
	return null
}

fun <T> MutableList<T>.swap(index_a: Int, index_b: Int){
	val a = this[index_a]
	this[index_a] = this[index_b]
	this[index_b] = a
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
