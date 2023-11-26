package com.toasterofbread.spmp.ui.layout.apppage.mainpage

import LocalPlayerState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.isTertiaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import com.toasterofbread.composekit.utils.modifier.background
import com.toasterofbread.spmp.ui.layout.nowplaying.maintab.NowPlayingMainTabPage
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive

val MINIMISED_NOW_PLAYING_HEIGHT_DP: Float
    @Composable get() = NowPlayingMainTabPage.Mode.getCurrent(LocalPlayerState.current).getMinimisedPlayerHeight().value
val MINIMISED_NOW_PLAYING_V_PADDING_DP: Float
    @Composable get() = NowPlayingMainTabPage.Mode.getCurrent(LocalPlayerState.current).getMinimisedPlayerVPadding().value
const val MEDIAITEM_PREVIEW_SQUARE_SIZE_SMALL: Float = 100f
const val MEDIAITEM_PREVIEW_SQUARE_SIZE_LARGE: Float = 200f

@Composable
fun RootView(player: PlayerStateImpl) {
    val density: Density = LocalDensity.current
    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                with(density) {
                    player.screen_size = DpSize(
                        size.width.toDp(),
                        size.height.toDp()
                    )
                }
            }
    )

    val focus_requester: FocusRequester = remember { FocusRequester() }

    Column(
        Modifier
            .fillMaxSize()
            .background(player.theme.background_provider)
//            .pointerInput(Unit) {
//                while (currentCoroutineContext().isActive) {
//                    awaitPointerEventScope {
//                        val event = awaitPointerEvent(PointerEventPass.Final)
//                        if (event.type == PointerEventType.Press && (event.buttons.isPrimaryPressed || event.buttons.isSecondaryPressed || event.buttons.isTertiaryPressed)) {
//                            println(event.changes.any { it.isConsumed })
//                            focus_requester.requestFocus()
//                        }
//                    }
//                }
//            }
//            .focusRequester(focus_requester)
    ) {
        player.HomePage()
        player.NowPlaying()
    }

    player.PersistentContent()
}
