package com.spectre7.spmp.platform.composable

import androidx.compose.runtime.Composable

@Composable
expect fun BackHandler(enabled: Boolean = true, action: () -> Unit)
