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
import androidx.compose.ui.zIndex
import com.toasterofbread.composekit.utils.composable.*
import com.toasterofbread.composekit.utils.common.thenIf
import com.toasterofbread.spmp.platform.*
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.component.WAVE_BORDER_HEIGHT_DP
import com.toasterofbread.spmp.ui.layout.apppage.AppPageSidebar
import com.toasterofbread.spmp.ui.layout.contentbar.*
import com.toasterofbread.spmp.ui.layout.contentbar.PortraitLayoutSlot
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingTopOffsetSection

@Composable
fun MainPageDisplay(bottom_padding: Dp = 0.dp) {
    val player: PlayerState = LocalPlayerState.current
    val density: Density = LocalDensity.current
    val horizontal_padding: Dp by animateDpAsState(player.getDefaultHorizontalPadding())

    val side_bar_modifier: Modifier = Modifier.zIndex(1f)

    Row {
        val top_padding: Dp = WindowInsets.getTop()

        if (player.form_factor == FormFactor.LANDSCAPE) {
            LandscapeSideBars(true, side_bar_modifier)
        }

        Crossfade(player.app_page, Modifier.fillMaxWidth().weight(1f)) { page ->
            Box {
                val upper_bar_slot: LayoutSlot
                val lower_bar_slot: LayoutSlot
                when (player.form_factor) {
                    FormFactor.LANDSCAPE -> {
                        upper_bar_slot = LandscapeLayoutSlot.UPPER_TOP_BAR
                        lower_bar_slot = LandscapeLayoutSlot.LOWER_TOP_BAR
                    }
                    FormFactor.PORTRAIT -> {
                        upper_bar_slot = PortraitLayoutSlot.UPPER_TOP_BAR
                        lower_bar_slot = PortraitLayoutSlot.LOWER_TOP_BAR
                    }
                }

                var upper_bar_height: Dp by remember { mutableStateOf(0.dp) }
                var lower_bar_height: Dp by remember { mutableStateOf(0.dp) }

                var upper_bar_displaying: Boolean by remember { mutableStateOf(false) }
                var lower_bar_displaying: Boolean by remember { mutableStateOf(false) }

                Column(
                    Modifier
                        .padding(top = top_padding)
                        .zIndex(1f)
                ) {
                    upper_bar_displaying = upper_bar_slot.DisplayBar(
                        if (lower_bar_displaying) lower_bar_height else 0.dp,
                        Modifier
                            .fillMaxWidth()
                            .onSizeChanged {
                                upper_bar_height = with (density) { it.height.toDp() }
                            },
                        container_modifier = Modifier.zIndex(1f)
                    )
                    lower_bar_displaying = lower_bar_slot.DisplayBar(
                        0.dp,
                        Modifier
                            .fillMaxWidth()
                            .onSizeChanged {
                                lower_bar_height = with (density) { it.height.toDp() }
                            }
                    )
                }

                val vertical_padding: Dp = player.getDefaultVerticalPadding()
                val top_bar_padding: Dp = (
                    (if (upper_bar_displaying) upper_bar_height else 0.dp)
                    + (if (lower_bar_displaying) lower_bar_height else 0.dp)
                )

                Box(Modifier.zIndex(1f)) {
                    Column {
                        with(page) {
                            Page(
                                player.main_multiselect_context,
                                Modifier,
                                PaddingValues(
                                    top = top_padding + vertical_padding + top_bar_padding,
                                    bottom = player.nowPlayingBottomPadding(true) + vertical_padding + bottom_padding,
                                    start = horizontal_padding + WindowInsets.getStart(),
                                    end = horizontal_padding + WindowInsets.getEnd()
                                )
                            ) { player.navigateBack() }
                        }
                    }

                    val above_player_slot: LayoutSlot =
                        when (player.form_factor) {
                            FormFactor.LANDSCAPE -> LandscapeLayoutSlot.ABOVE_PLAYER
                            FormFactor.PORTRAIT -> PortraitLayoutSlot.ABOVE_PLAYER
                        }

                    Box(
                        player
                            .nowPlayingTopOffset(Modifier, NowPlayingTopOffsetSection.LAYOUT_SLOT, apply_spacing = false)
                            .fillMaxWidth()
                            .align(Alignment.BottomEnd)
                    ) {
                        above_player_slot.DisplayBar(0.dp, Modifier.fillMaxWidth())
                    }
                }
            }
        }

        if (player.form_factor == FormFactor.LANDSCAPE) {
            LandscapeSideBars(false, side_bar_modifier)
        }
    }
}

@Composable
private fun RowScope.LandscapeSideBars(
    start: Boolean,
    container_modifier: Modifier = Modifier
): Pair<Boolean, Boolean> {
    val density: Density = LocalDensity.current

    val first: LayoutSlot =
        if (start) LandscapeLayoutSlot.OUTER_SIDE_LEFT
        else LandscapeLayoutSlot.INNER_SIDE_RIGHT
    val second: LayoutSlot =
        if (start) LandscapeLayoutSlot.INNER_SIDE_LEFT
        else LandscapeLayoutSlot.OUTER_SIDE_RIGHT

    val modifier: Modifier = Modifier.fillMaxHeight()
    var inner_bar_width: Dp by remember { mutableStateOf(0.dp) }

    val first_displaying: Boolean = first.DisplayBar(
        if (start) inner_bar_width else 0.dp,
        modifier.onSizeChanged {
            if (!start) {
                inner_bar_width = with (density) { it.width.toDp() }
            }
        },
        container_modifier.thenIf(start) { zIndex(1f) }
    )

    val second_displaying: Boolean = second.DisplayBar(
        if (!start) inner_bar_width else 0.dp,
        modifier.onSizeChanged {
            if (start) {
                inner_bar_width = with (density) { it.width.toDp() }
            }
        },
        container_modifier.thenIf(!start) { zIndex(1f) }
    )

    return Pair(first_displaying, second_displaying)
}
