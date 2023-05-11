package com.spectre7.spmp.platform.composable

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.accompanist.swiperefresh.SwipeRefreshIndicator
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

@Composable
actual fun SwipeRefresh(
    state: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier,
    swipe_enabled: Boolean,
    indicator: Boolean,
    content: @Composable () -> Unit
) {
    com.google.accompanist.swiperefresh.SwipeRefresh(
        rememberSwipeRefreshState(state),
        onRefresh,
        modifier,
        swipe_enabled,
        content = content,
        indicator = { s, trigger ->
            if (indicator || s.isSwipeInProgress) {
                SwipeRefreshIndicator(s, trigger)
            }
        }
    )
}
