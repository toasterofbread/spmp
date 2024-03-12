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
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingTopOffsetSection

@Composable
fun MainPageDisplay(bottom_padding: Dp = 0.dp) {
    val player: PlayerState = LocalPlayerState.current
    val density: Density = LocalDensity.current
    val horizontal_padding: Dp by animateDpAsState(player.getDefaultHorizontalPadding())

    var start_bar_widths: Pair<Dp, Dp> by remember { mutableStateOf(Pair(0.dp, 0.dp)) }
    var end_bar_widths: Pair<Dp, Dp> by remember { mutableStateOf(Pair(0.dp, 0.dp)) }

    var start_bars_displaying: Pair<Boolean, Boolean> by remember { mutableStateOf(Pair(false, false)) }
    var end_bars_displaying: Pair<Boolean, Boolean> by remember { mutableStateOf(Pair(false, false)) }

    val side_bar_modifier: Modifier = Modifier.zIndex(1f)

    Row {
        val top_padding: Dp = WindowInsets.getTop()

        if (player.form_factor == FormFactor.LANDSCAPE) {
            start_bars_displaying =
                LandscapeSideBars(true, side_bar_modifier) { width: Dp, outer: Boolean ->
                    start_bar_widths =
                        if (outer) start_bar_widths.copy(first = width)
                        else start_bar_widths.copy(second = width)
                }
        }
        else {
            start_bar_widths = Pair(0.dp, 0.dp)
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
                        Modifier
                            .fillMaxWidth()
                            .onSizeChanged {
                                upper_bar_height = with (density) { it.height.toDp() }
                            }
                    )
                    lower_bar_displaying = lower_bar_slot.DisplayBar(
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
            }
        }

        if (player.form_factor == FormFactor.LANDSCAPE) {
            end_bars_displaying =
                LandscapeSideBars(false, side_bar_modifier) { width: Dp, outer: Boolean ->
                    end_bar_widths =
                        if (outer) end_bar_widths.copy(first = width)
                        else end_bar_widths.copy(second = width)
                }
        }
        else {
            end_bar_widths = Pair(0.dp, 0.dp)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .padding(
                start =
                    (if (start_bars_displaying.first) start_bar_widths.first else 0.dp)
                    + if (start_bars_displaying.second) start_bar_widths.second else 0.dp,
                end =
                    (if (end_bars_displaying.first) end_bar_widths.first else 0.dp)
                    + if (end_bars_displaying.second) end_bar_widths.second else 0.dp
            )
    ) {
        val layout_slot: LayoutSlot =
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
            layout_slot.DisplayBar(Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun RowScope.LandscapeSideBars(
    start: Boolean,
    container_modifier: Modifier = Modifier,
    onSizeChanged: (Dp, Boolean) -> Unit
): Pair<Boolean, Boolean> {
    val density: Density = LocalDensity.current

    val first: LayoutSlot =
        if (start) LandscapeLayoutSlot.OUTER_SIDE_LEFT
        else LandscapeLayoutSlot.INNER_SIDE_RIGHT
    val second: LayoutSlot =
        if (start) LandscapeLayoutSlot.INNER_SIDE_LEFT
        else LandscapeLayoutSlot.OUTER_SIDE_RIGHT

    val modifier: Modifier = Modifier.fillMaxHeight()

    val first_displaying: Boolean = first.DisplayBar(
        modifier.onSizeChanged {
            with (density) {
                onSizeChanged(it.width.toDp(), true)
            }
        },
        container_modifier
    )

    val second_displaying: Boolean = second.DisplayBar(
        modifier.onSizeChanged {
            with (density) {
                onSizeChanged(it.width.toDp(), false)
            }
        },
        container_modifier
    )

    return Pair(first_displaying, second_displaying)
}
