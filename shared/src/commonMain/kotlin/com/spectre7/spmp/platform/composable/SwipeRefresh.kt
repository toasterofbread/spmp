package com.spectre7.spmp.platform.composable

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun SwipeRefresh(
    state: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    swipe_enabled: Boolean = true,
    indicator: Boolean = true,
    content: @Composable () -> Unit
)