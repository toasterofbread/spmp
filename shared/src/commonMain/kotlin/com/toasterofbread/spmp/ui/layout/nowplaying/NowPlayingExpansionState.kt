@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
package com.toasterofbread.spmp.ui.layout.nowplaying

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeableState
import androidx.compose.runtime.*
import com.toasterofbread.spmp.model.MusicTopBarMode
import com.toasterofbread.spmp.ui.layout.mainpage.PlayerState
import com.toasterofbread.spmp.ui.layout.nowplaying.maintab.MIN_EXPANSION
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterialApi::class)
class NowPlayingExpansionState(swipe_state: State<SwipeableState<Int>>, private val player: PlayerState) {
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
        val expansion: Float

        val anchors: Map<Float, Int> = swipe_state.anchors
        if (anchors.isEmpty()) {
            expansion = 0f
        }
        else {
            val half_screen_height: Float = player.screen_size.height.value * 0.5f
            val anchor: Float = anchors.entries.first { it.value == 1 }.key + half_screen_height

            expansion = (swipe_state.offset.value + half_screen_height) / anchor
        }

        if (expansion < 1f) {
            return (1f / (1f - MIN_EXPANSION)) * (expansion - MIN_EXPANSION)
        }
        else {
            return expansion
        }
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
