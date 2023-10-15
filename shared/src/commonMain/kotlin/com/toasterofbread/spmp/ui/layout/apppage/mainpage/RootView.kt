package com.toasterofbread.spmp.ui.layout.apppage.mainpage

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import com.toasterofbread.spmp.ui.theme.Theme
import com.toasterofbread.utils.common.isDebugBuild
import com.toasterofbread.utils.modifier.background
import kotlinx.coroutines.delay

const val MINIMISED_NOW_PLAYING_HEIGHT_DP: Int = 64
const val MINIMISED_NOW_PLAYING_V_PADDING_DP: Int = 7
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

    player.LongPressMenu()

    LaunchedEffect(Unit) {
        if (!isDebugBuild()) {
            return@LaunchedEffect
        }

        var prev: String? = null
        while (true) {
            delay(500)

            val status: String = player.status.toString()
            if (status != prev) {
                println("Status: $status")
                prev = status
            }
        }
    }
}
