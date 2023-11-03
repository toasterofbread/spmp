package com.toasterofbread.spmp.ui.layout.nowplaying.queue

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.toasterofbread.spmp.platform.isPortrait
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.PlayerState
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingPage
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingTopBar

class NowPlayingQueuePage: NowPlayingPage() {
    override fun shouldShow(player: PlayerState): Boolean = player.isPortrait()

    @Composable
    override fun Page(page_height: Dp, top_bar: NowPlayingTopBar, content_padding: PaddingValues, swipe_modifier: Modifier, modifier: Modifier) {
        QueueTab(page_height, top_bar, swipe_modifier, modifier, content_padding = content_padding)
    }
}
