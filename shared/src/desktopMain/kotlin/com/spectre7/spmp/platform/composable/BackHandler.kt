package com.toasterofbread.spmp.platform.composable

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent

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
