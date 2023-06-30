package com.toasterofbread.spmp.platform.composable

import androidx.compose.runtime.Composable

@Composable
actual fun BackHandler(enabled: Boolean, action: () -> Unit) {
    androidx.activity.compose.BackHandler(enabled, action)
}
