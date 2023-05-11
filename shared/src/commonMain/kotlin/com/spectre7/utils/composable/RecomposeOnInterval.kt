package com.spectre7.utils.composable

import androidx.compose.runtime.*
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
