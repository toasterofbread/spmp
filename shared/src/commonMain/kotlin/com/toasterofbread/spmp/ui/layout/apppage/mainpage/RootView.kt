package com.toasterofbread.spmp.ui.layout.apppage.mainpage

import LocalPlayerState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import com.toasterofbread.composekit.utils.modifier.background
import com.toasterofbread.spmp.ui.layout.nowplaying.maintab.NowPlayingMainTabPage

val MINIMISED_NOW_PLAYING_HEIGHT_DP: Float
    @Composable get() = NowPlayingMainTabPage.Mode.getCurrent(LocalPlayerState.current).getMinimisedPlayerHeight().value
val MINIMISED_NOW_PLAYING_V_PADDING_DP: Float
    @Composable get() = NowPlayingMainTabPage.Mode.getCurrent(LocalPlayerState.current).getMinimisedPlayerVPadding().value
const val MEDIAITEM_PREVIEW_SQUARE_SIZE_SMALL: Float = 100f
const val MEDIAITEM_PREVIEW_SQUARE_SIZE_LARGE: Float = 200f

@Composable
fun RootView(player: PlayerStateImpl) {
    val density = LocalDensity.current
    Box(Modifier.fillMaxSize().onSizeChanged { size ->
        with(density) {
            player.screen_size = DpSize(
                size.width.toDp(),
                size.height.toDp()
            )
        }
    })

    Column(
        Modifier
            .fillMaxSize()
            .background(player.theme.background_provider)
    ) {
        player.HomePage()
        player.NowPlaying()
    }

    player.PersistentContent()
}
