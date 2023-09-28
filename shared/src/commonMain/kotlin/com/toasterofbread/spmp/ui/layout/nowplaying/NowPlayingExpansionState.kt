@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
package com.toasterofbread.spmp.ui.layout.nowplaying

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeableState
import androidx.compose.runtime.*
import androidx.compose.ui.unit.Density
import com.toasterofbread.spmp.model.MusicTopBarMode
import com.toasterofbread.spmp.platform.PlatformContext
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterialApi::class)
class NowPlayingExpansionState(swipe_state: State<SwipeableState<Int>>) {
    private val swipe_state by swipe_state
    private var switch_to_page: Int by mutableStateOf(-1)

    val top_bar_mode: MutableState<MusicTopBarMode> = mutableStateOf(MusicTopBarMode.default)
    val page_range: IntRange get() = 0 .. NOW_PLAYING_VERTICAL_PAGE_COUNT

    @Composable
    fun init() {
        LaunchedEffect(switch_to_page) {
            if (switch_to_page >= 0) {
                swipe_state.animateTo(switch_to_page)
                switch_to_page = -1
            }
        }
    }

    fun scroll(pages: Int) {
        switch_to_page = (swipe_state.targetValue + pages).coerceIn(page_range)
    }

    fun scrollTo(page: Int) {
        require(page in page_range)
        switch_to_page = page
    }

    fun close() {
        switch_to_page = if (swipe_state.targetValue == 0) 1 else 0
    }

    fun getPage(): Int {
        return get().roundToInt().coerceIn(page_range)
    }

    fun get(): Float {
        val anchors: Map<Float, Int> = swipe_state.anchors
        if (anchors.isEmpty()) {
            return 0f
        }
        assert(anchors.size == NOW_PLAYING_VERTICAL_PAGE_COUNT + 1)

        val offset: Float = swipe_state.offset.value

        var low_index: Int? = null
        var low: Float? = null
        var high: Float? = null

        for (anchor in anchors) {
            if (offset < anchor.key) {
                low_index = (anchor.value - 1).coerceAtLeast(page_range.first)
                low = anchors.entries.firstOrNull { it.value == low_index }?.key ?: return low_index.toFloat()
                high = anchor.key
                break
            }
        }

        if (low_index == null) {
            low_index = page_range.last
            low = anchors.entries.firstOrNull { it.value == low_index }?.key ?: return low_index.toFloat()
            high = low
        }
        else {
            check(low != null)
            check(high != null)
        }

        val progress: Float
        if (offset <= low) {
            progress = 0f
        }
        else if (offset >= high) {
            progress = 1f
        }
        else {
            progress = (offset - low) / (high - low)
        }

        return low_index + progress
    }

    fun getBounded(): Float = get().coerceIn(page_range.first.toFloat(), page_range.last.toFloat())
    fun getAbsolute(): Float {
        val bounded = getBounded()
        if (bounded <= 1f) {
            return bounded
        }
        else {
            return 2f - getBounded()
        }
    }

    fun getAppearing(): Float {
        val absolute = getAbsolute()
        return minOf(
            1f,
            if (absolute > 0.5f) 1f
            else (absolute * 2f)
        )
    }
    fun getDisappearing(): Float {
        val absolute = getAbsolute()
        return minOf(
            1f,
            if (absolute < 0.5f) 1f
            else (1f - ((absolute - 0.5f) * 2f))
        )
    }
}

fun WindowInsets.getAdjustedKeyboardHeight(density: Density, context: PlatformContext): Int {
    val bottom: Int = getBottom(density)
    if (bottom > 0) {
        val navbar_height: Int = context.getNavigationBarHeight()
        return bottom.coerceAtMost(
            (bottom - navbar_height).coerceAtLeast(0)
        )
    }
    return bottom
}
