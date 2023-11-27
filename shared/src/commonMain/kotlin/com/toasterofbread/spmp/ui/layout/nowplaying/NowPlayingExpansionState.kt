@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
package com.toasterofbread.spmp.ui.layout.nowplaying

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeableState
import androidx.compose.runtime.*
import com.toasterofbread.spmp.model.settings.category.MusicTopBarMode
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.PlayerState
import kotlinx.coroutines.CoroutineScope
import kotlin.math.roundToInt

interface ExpansionState {
    val top_bar_mode: MutableState<MusicTopBarMode>
    fun get(): Float
    fun getPageRange(): IntRange

    fun getBounded(): Float = get().coerceIn(getPageRange().first.toFloat(), getPageRange().last.toFloat())
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

    companion object {
        fun getStatic(expansion_value: Float) =
            object : ExpansionState {
                override val top_bar_mode: MutableState<MusicTopBarMode> =
                    mutableStateOf(MusicTopBarMode.default)

                override fun get(): Float =
                    expansion_value

                override fun getPageRange(): IntRange =
                    0 .. 1
            }
    }
}

@OptIn(ExperimentalMaterialApi::class)
class NowPlayingExpansionState(
    val player: PlayerState,
    swipe_state: State<SwipeableState<Int>>,
    private val coroutine_scope: CoroutineScope
): ExpansionState {
    val swipe_state by swipe_state

    override val top_bar_mode: MutableState<MusicTopBarMode> = mutableStateOf(MusicTopBarMode.default)
    
    override fun getPageRange(): IntRange =
        0 .. getNowPlayingVerticalPageCount(player)

    fun scroll(pages: Int) {
        scrollTo((swipe_state.targetValue + pages).coerceIn(getPageRange()))
    }

    fun scrollTo(page: Int) {
        require(page in getPageRange())
        player.switchNowPlayingPage(page)
    }

    fun close() {
        scrollTo(if (swipe_state.targetValue == 0) 1 else 0)
    }

    fun getPage(): Int {
        return get().roundToInt().coerceIn(getPageRange())
    }

    override fun get(): Float {
        val anchors: Map<Float, Int> = swipe_state.anchors
        if (anchors.isEmpty()) {
            return 0f
        }
//        assert(anchors.size == getNowPlayingVerticalPageCount(player) + 1) {
//            "${anchors.size} == ${getNowPlayingVerticalPageCount(player)} + 1"
//        }

        val offset: Float = swipe_state.offset.value

        var low_index: Int? = null
        var low: Float? = null
        var high: Float? = null

        for (anchor in anchors) {
            if (offset < anchor.key) {
                low_index = (anchor.value - 1).coerceAtLeast(getPageRange().first)
                low = anchors.entries.firstOrNull { it.value == low_index }?.key ?: return low_index.toFloat()
                high = anchor.key
                break
            }
        }

        if (low_index == null) {
            low_index = getPageRange().last
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
            if (low_index >= anchors.size) {
                progress = 1f
            }
            else {
                progress = 0f
            }
        }
        else {
            progress = (offset - low) / (high - low)
        }

        return low_index + progress
    }
}
