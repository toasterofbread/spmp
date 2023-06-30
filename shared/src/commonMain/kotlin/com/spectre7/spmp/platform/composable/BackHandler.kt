package com.spectre7.spmp.platform.composable

import androidx.compose.runtime.Composable

@Composable
expect fun BackHandler(enabled: Boolean = true, action: () -> Unit)

//@Composable
//fun BackHandlerL(getEnabled: @Composable () -> Boolean, action: () -> Unit) {
//    BackHandler(getEnabled(), action)
//}
