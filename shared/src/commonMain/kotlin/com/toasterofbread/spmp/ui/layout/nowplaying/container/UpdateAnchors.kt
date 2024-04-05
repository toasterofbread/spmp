package com.toasterofbread.spmp.ui.layout.nowplaying.container

import androidx.compose.foundation.gestures.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.*
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.MINIMISED_NOW_PLAYING_HEIGHT_DP
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingPage
import kotlinx.coroutines.*

@Composable
internal fun UpdateAnchors(
    swipe_state: AnchoredDraggableState<Int>,
    pages: List<NowPlayingPage>,
    page_height: Dp,
    coroutine_scope: CoroutineScope
) {
    require(pages.isNotEmpty())

    val minimised_now_playing_height: Dp = MINIMISED_NOW_PLAYING_HEIGHT_DP.dp

    LaunchedEffect(page_height, pages.size, minimised_now_playing_height) {
        val anchors: DraggableAnchors<Int> =
            DraggableAnchors {
                val base_position: Dp = minimised_now_playing_height
                0 at base_position.value

                for (page in 1 .. pages.size) {
                    val position: Dp =
                        page_height * page

                    page at position.value
                }
            }

        val initial_position: Int = swipe_state.currentValue
        swipe_state.updateAnchors(anchors)

        if (initial_position == 0) {
            coroutine_scope.launch {
                swipe_state.snapTo(initial_position)
            }
        }
    }
}