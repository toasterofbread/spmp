package com.toasterofbread.spmp.ui.layout.apppage.mainpage

import LocalPlayerState
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import androidx.compose.ui.layout.onSizeChanged
import com.toasterofbread.composekit.utils.composable.*
import com.toasterofbread.spmp.platform.*
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.component.WAVE_BORDER_HEIGHT_DP
import com.toasterofbread.spmp.ui.layout.apppage.AppPageSidebar
import com.toasterofbread.spmp.ui.layout.contentbar.*
import com.toasterofbread.spmp.ui.layout.contentbar.PortraitLayoutSlot

@Composable
fun MainPageDisplay(bottom_padding: Dp = 0.dp) {
    val player: PlayerState = LocalPlayerState.current
    val density: Density = LocalDensity.current
    val horizontal_padding: Dp by animateDpAsState(player.getDefaultHorizontalPadding())

    var start_bar_width: Dp by remember { mutableStateOf(0.dp) }
    var start_bar_showing: Boolean by remember { mutableStateOf(false) }
    var end_bar_width: Dp by remember { mutableStateOf(0.dp) }
    var end_bar_showing: Boolean by remember { mutableStateOf(false) }

    Row {
        val top_padding: Dp = WindowInsets.getTop()

        if (player.form_factor == FormFactor.LANDSCAPE) {
            start_bar_showing = LandscapeLayoutSlot.SIDE_LEFT.DisplayBar(
                Modifier.fillMaxHeight().zIndex(1f).onSizeChanged {
                    start_bar_width = with (density) {
                        it.width.toDp()
                    }
                }
            )
        }
        else {
            start_bar_width = 0.dp
            start_bar_showing = false
        }

        Crossfade(player.app_page, Modifier.fillMaxWidth().weight(1f)) { page ->
            Column {
                val vertical_padding: Dp = player.getDefaultVerticalPadding()

                // val top_bar_slot: LayoutSlot =
                //     if (player.form_factor == FormFactor.LANDSCAPE) LandscapeLayoutSlot.UPPER_TOP_BAR
                //     else PortraitLayoutSlot.UPPER_TOP_BAR

                // top_bar_slot.DisplayBar(Modifier.fillMaxWidth())
                if (page.showTopBar()) {
                    MainPageTopBar(PaddingValues(horizontal = horizontal_padding), Modifier.padding(top = top_padding).zIndex(1f))
                }

                with(page) {
                    Page(
                        player.main_multiselect_context,
                        Modifier,
                        PaddingValues(
                            top = if (page.showTopBar()) WAVE_BORDER_HEIGHT_DP.dp / 2 else (top_padding + vertical_padding),
                            bottom = player.nowPlayingBottomPadding(true) + vertical_padding + bottom_padding,
                            start = horizontal_padding + WindowInsets.getStart(),
                            end = horizontal_padding + WindowInsets.getEnd()
                        )
                    ) { player.navigateBack() }
                }
            }
        }

        if (player.form_factor == FormFactor.LANDSCAPE) {
            end_bar_showing = LandscapeLayoutSlot.SIDE_RIGHT.DisplayBar(
                Modifier.fillMaxHeight().zIndex(1f).onSizeChanged {
                    end_bar_width = with (density) {
                        it.width.toDp()
                    }
                }
            )
        }
        else {
            end_bar_width = 0.dp
            end_bar_showing = false
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .padding(
                start = if (start_bar_showing) start_bar_width else 0.dp,
                end = if (end_bar_showing) end_bar_width else 0.dp
            )
    ) {
        val layout_slot: LayoutSlot =
            when (player.form_factor) {
                FormFactor.LANDSCAPE -> LandscapeLayoutSlot.ABOVE_PLAYER
                FormFactor.PORTRAIT -> PortraitLayoutSlot.ABOVE_PLAYER
            }

        Box(player.nowPlayingTopOffset(Modifier, apply_spacing = false).fillMaxWidth().align(Alignment.BottomEnd)) {
            layout_slot.DisplayBar(Modifier.fillMaxWidth())
        }
    }
}
