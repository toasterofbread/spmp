package com.toasterofbread.spmp.ui.layout.nowplaying

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.PlayerState
import com.toasterofbread.spmp.ui.layout.nowplaying.maintab.NowPlayingMainTabPage
import com.toasterofbread.spmp.ui.layout.nowplaying.queue.NowPlayingQueuePage
import com.toasterofbread.toastercomposetools.utils.composable.getTop

const val NOW_PLAYING_MAIN_PADDING = 10f

abstract class NowPlayingPage {
    @Composable
    abstract fun Page(page_height: Dp, top_bar: NowPlayingTopBar, content_padding: PaddingValues, swipe_modifier: Modifier, modifier: Modifier)
    abstract fun shouldShow(player: PlayerState): Boolean

    companion object {
        val top_padding: Dp
            @Composable get() {
                if (WindowInsets.getTop() > 0.dp) {
                    return NOW_PLAYING_MAIN_PADDING.dp / 2
                }
                return NOW_PLAYING_MAIN_PADDING.dp
            }

        val horizontal_padding: Dp = NOW_PLAYING_MAIN_PADDING.dp
        val bottom_padding: Dp = NOW_PLAYING_MAIN_PADDING.dp

        val ALL: List<NowPlayingPage> =
            listOf(
                NowPlayingMainTabPage(),
                NowPlayingQueuePage()
            )
    }
}
