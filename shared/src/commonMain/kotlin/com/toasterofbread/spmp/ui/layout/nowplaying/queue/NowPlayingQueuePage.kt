package com.toasterofbread.spmp.ui.layout.nowplaying.queue

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.toasterofbread.spmp.platform.FormFactor
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingPage
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingTopBar

class NowPlayingQueuePage: NowPlayingPage() {
    override fun shouldShow(player: PlayerState, form_factor: FormFactor): Boolean =
        form_factor == FormFactor.PORTRAIT

    @Composable
    override fun Page(page_height: Dp, top_bar: NowPlayingTopBar, content_padding: PaddingValues, swipe_modifier: Modifier, modifier: Modifier) {
        QueueTab(page_height, modifier, top_bar = top_bar, padding_modifier = swipe_modifier, content_padding = content_padding)
    }
}
