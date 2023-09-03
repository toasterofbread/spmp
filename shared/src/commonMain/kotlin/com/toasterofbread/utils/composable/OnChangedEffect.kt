package com.toasterofbread.utils.composable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

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

@Composable
fun OnChangedEffect(key1: Any?, key2: Any?, block: suspend () -> Unit) {
	var launched by remember { mutableStateOf(false) }
    LaunchedEffect(key1, key2) {
        if (!launched) {
            launched = true
        }
        else {
            block()
        }
    }
}
