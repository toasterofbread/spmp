package com.toasterofbread.spmp.ui.layout.nowplaying

import LocalPlayerState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.toasterofbread.composekit.utils.composable.getTop
import com.toasterofbread.spmp.platform.isLargeFormFactor
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.PlayerState
import com.toasterofbread.spmp.ui.layout.nowplaying.maintab.NowPlayingMainTabPage
import com.toasterofbread.spmp.ui.layout.nowplaying.queue.NowPlayingQueuePage

const val NOW_PLAYING_MAIN_PADDING_DP = 10f
const val NOW_PLAYING_MAIN_PADDING_LARGE_DP = 30f

abstract class NowPlayingPage {
    @Composable
    abstract fun Page(page_height: Dp, top_bar: NowPlayingTopBar, content_padding: PaddingValues, swipe_modifier: Modifier, modifier: Modifier)
    abstract fun shouldShow(player: PlayerState): Boolean

    open fun getPlayerBackgroundColourOverride(player: PlayerState): Color? = null

    companion object {
        @Composable
        private fun getMainPadding(): Dp =
            if (LocalPlayerState.current.isLargeFormFactor()) NOW_PLAYING_MAIN_PADDING_LARGE_DP.dp
            else NOW_PLAYING_MAIN_PADDING_DP.dp

        val top_padding: Dp
            @Composable get() {
                if (WindowInsets.getTop() > 0.dp) {
                    return getMainPadding() / 2
                }
                return getMainPadding()
            }
        val bottom_padding: Dp @Composable get() = getMainPadding()

        val horizontal_padding: Dp @Composable get() = getMainPadding()
        val horizontal_padding_minimised: Dp @Composable get() = NOW_PLAYING_MAIN_PADDING_DP.dp

        val ALL: List<NowPlayingPage> =
            listOf(
                NowPlayingMainTabPage(),
                NowPlayingQueuePage()
            )
    }
}
