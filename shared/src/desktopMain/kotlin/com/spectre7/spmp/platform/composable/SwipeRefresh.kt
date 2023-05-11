package com.spectre7.spmp.platform.composable

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun SwipeRefresh(
    state: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier,
    swipe_enabled: Boolean,
    indicator: Boolean,
    content: @Composable () -> Unit
) {
    Box(modifier) {
        content()
    }
}
