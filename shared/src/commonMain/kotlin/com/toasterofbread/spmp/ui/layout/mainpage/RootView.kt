package com.toasterofbread.spmp.ui.layout.mainpage

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.toasterofbread.spmp.ui.theme.Theme
import com.toasterofbread.utils.modifier.background

const val MINIMISED_NOW_PLAYING_HEIGHT_DP: Int = 64
const val MINIMISED_NOW_PLAYING_V_PADDING_DP: Int = 7
const val MEDIAITEM_PREVIEW_SQUARE_SIZE_SMALL: Float = 100f
const val MEDIAITEM_PREVIEW_SQUARE_SIZE_LARGE: Float = 200f

@Composable
fun RootView(player: PlayerStateImpl) {
    player.LongPressMenu()

    Column(
        Modifier
            .fillMaxSize()
            .background(Theme.background_provider)
    ) {
        player.HomePage()
        player.NowPlaying()
    }
}
