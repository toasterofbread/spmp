package com.toasterofbread.utils.composable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

@Composable
fun RecomposeOnInterval(interval_ms: Long, enabled: Boolean = true, content: @Composable (Boolean) -> Unit) {
    var recomposition_state by remember { mutableStateOf(false) }

    LaunchedEffect(enabled) {
        if (enabled) {
            while (true) {
                delay(interval_ms)
                recomposition_state = !recomposition_state
            }
        } else {
            recomposition_state = !recomposition_state
        }
    }

	content(recomposition_state)
}
