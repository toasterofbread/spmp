package com.toasterofbread.spmp.ui.layout.mainpage

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
