package com.toasterofbread.spmp.model.mediaitem.db

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.cash.sqldelight.Query

@Composable
fun <T, Q: Query<*>> Q.observeAsState(
    key: Any,
    mapValue: (Q) -> T = { it as T },
    onExternalChange: (suspend (T) -> Unit)?
): MutableState<T> {
    val state: MutableState<T> = remember(key) { mutableStateOf(mapValue(this)) }
    var current_value: T by remember(state) { mutableStateOf(state.value) }

    DisposableEffect(state) {
        val listener: Query.Listener = Query.Listener {
            current_value = mapValue(this@observeAsState)
            state.value = current_value
        }

        addListener(listener)
        onDispose {
            removeListener(listener)
        }
    }

    LaunchedEffect(state.value) {
        if (state.value != current_value) {
            current_value = state.value

            if (onExternalChange != null) {
                try {
                    onExternalChange(current_value)
                }
                catch (e: Throwable) {
                    if (e::class.qualifiedName != "androidx.compose.runtime.LeftCompositionCancellationException") {
                        e.printStackTrace()
                        throw RuntimeException("onExternalChange failed for observed query (${this@observeAsState}, $current_value)", e)
                    }
                }
            }
            else {
                throw IllegalStateException("onExternalChange has not been defined (${this@observeAsState}, $current_value)")
            }
        }
    }

    return state
}
