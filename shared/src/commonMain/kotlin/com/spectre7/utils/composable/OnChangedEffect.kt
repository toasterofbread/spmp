package com.toasterofbread.utils.composable

import androidx.compose.runtime.*

@Composable
fun OnChangedEffect(key: Any?, block: suspend () -> Unit) {
	var launched by remember { mutableStateOf(false) }
    LaunchedEffect(key) {
        if (!launched) {
            launched = true
        } else {
            block()
        }
    }
}

@Composable
fun OnChangedEffect(key1: Any?, key2: Any?, block: suspend () -> Unit) {
	var launched by remember { mutableStateOf(false) }
    LaunchedEffect(key1, key2) {
        if (!launched) {
            launched = true
        } else {
            block()
        }
    }
}
