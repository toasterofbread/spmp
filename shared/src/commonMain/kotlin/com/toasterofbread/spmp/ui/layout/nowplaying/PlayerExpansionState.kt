@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
package com.toasterofbread.spmp.ui.layout.nowplaying

import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.runtime.*
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import kotlinx.coroutines.CoroutineScope
import kotlin.math.roundToInt

interface ExpansionState {
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
}

abstract class PlayerExpansionState(
    val player: PlayerState,
    private val coroutine_scope: CoroutineScope
): ExpansionState {
    abstract val swipe_state: AnchoredDraggableState<Int>

    override fun getPageRange(): IntRange =
        0 .. NowPlayingPage.ALL.count { it.shouldShow(player, player.form_factor) }

    fun scroll(pages: Int) {
        scrollTo((swipe_state.targetValue + pages).coerceIn(getPageRange()))
    }

    fun scrollTo(page: Int) {
        require(page in getPageRange())
        player.switchNowPlayingPage(page)
    }

    fun toggle() {
        scrollTo(if (swipe_state.targetValue == 0) 1 else 0)
    }

    fun getPage(): Int {
        return get().roundToInt().coerceIn(getPageRange())
    }

    override fun get(): Float {
        val anchors: DraggableAnchors<Int> = swipe_state.anchors
        if (anchors.size == 0) {
            return 0f
        }

        val offset: Float = swipe_state.offset

        var low_index: Int? = null
        var low: Float? = null
        var high: Float? = null

        for (anchor in 0 until anchors.size) {
            val anchor_position: Float = anchors.positionOf(anchor)
            if (offset < anchor_position) {
                low_index = (anchor - 1).coerceAtLeast(getPageRange().first)
                if (!anchors.hasPositionFor(low_index)) {
                    return low_index.toFloat()
                }
                low = anchors.positionOf(low_index)
                high = anchor_position
                break
            }
        }

        if (low_index == null) {
            low_index = getPageRange().last
            if (!anchors.hasPositionFor(low_index)) {
                return low_index.toFloat()
            }
            low = anchors.positionOf(low_index)
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
