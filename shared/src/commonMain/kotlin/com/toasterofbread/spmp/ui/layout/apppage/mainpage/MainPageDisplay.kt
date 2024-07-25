package com.toasterofbread.spmp.ui.layout.apppage.mainpage

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import dev.toastbits.composekit.utils.common.*
import dev.toastbits.composekit.utils.common.getContrasted
import dev.toastbits.composekit.utils.composable.*
import dev.toastbits.composekit.utils.composable.getTop
import dev.toastbits.composekit.utils.modifier.background
import com.toasterofbread.spmp.platform.*
import LocalAppState
import com.toasterofbread.spmp.ui.layout.BarColourState
import com.toasterofbread.spmp.ui.layout.contentbar.*
import com.toasterofbread.spmp.ui.layout.contentbar.layoutslot.*
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingTopOffsetSection

@Composable
fun MainPageDisplay(bottom_padding: Dp = 0.dp) {
    val state: SpMp.State = LocalAppState.current
    val density: Density = LocalDensity.current
    val form_factor: FormFactor by FormFactor.observe()
    val horizontal_padding: Dp by animateDpAsState(state.getDefaultHorizontalPadding())

    val side_bar_modifier: Modifier = Modifier.zIndex(1f)

    Row {
        val top_padding: Dp = WindowInsets.getTop()

        if (form_factor == FormFactor.LANDSCAPE) {
            LandscapeSideBars(true, side_bar_modifier)
        }

        Crossfade(state.ui.app_page, Modifier.fillMaxWidth().weight(1f)) { page ->
            Box {
                val upper_bar_slot: LayoutSlot
                val lower_bar_slot: LayoutSlot
                when (form_factor) {
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

                var _upper_bar_displaying: Boolean by remember { mutableStateOf(false) }
                val upper_bar_displaying = _upper_bar_displaying || state.ui.main_multiselect_context.is_active
                var lower_bar_displaying: Boolean by remember { mutableStateOf(false) }

                val highest_slot: LayoutSlot? =
                    if (upper_bar_displaying) upper_bar_slot
                    else if (lower_bar_displaying) lower_bar_slot
                    else null

                val highest_colour: ColourSource? by highest_slot?.rememberColourSource()

                LaunchedEffect(highest_colour) {
                    state.ui.bar_colour_state.status_bar.setLevelColour(highest_colour, BarColourState.StatusBarLevel.BAR)
                }

                DisposableEffect(Unit) {
                    onDispose {
                        state.ui.bar_colour_state.status_bar.setLevelColour(null, BarColourState.StatusBarLevel.BAR)
                    }
                }

                Column(Modifier.zIndex(1f)) {
                    CompositionLocalProvider(LocalContentColor provides (highest_colour?.get(state.ui)?.getContrasted() ?: state.theme.on_background)) {
                        state.ui.main_multiselect_context.InfoDisplay(
                            Modifier
                                .fillMaxWidth()
                                .onSizeChanged {
                                    upper_bar_height = with (density) { it.height.toDp() }
                                }
                                .zIndex(1f),
                            content_modifier =
                                Modifier
                                    .background { highest_colour?.get(state.ui) ?: state.theme.background }
                                    .padding(top = top_padding),
                            show_alt_content = true,
                            altContent = {
                                _upper_bar_displaying = upper_bar_slot.DisplayBar(
                                    if (lower_bar_displaying) lower_bar_height else 0.dp,
                                    Modifier.fillMaxWidth(),
                                    content_padding = PaddingValues(top = top_padding)
                                )

                                DisposableEffect(Unit) {
                                    onDispose {
                                        _upper_bar_displaying = false
                                    }
                                }
                            }
                        )
                    }

                    lower_bar_displaying = lower_bar_slot.DisplayBar(
                        0.dp,
                        Modifier
                            .fillMaxWidth()
                            .onSizeChanged {
                                lower_bar_height = with (density) { it.height.toDp() }
                            },
                        content_padding = PaddingValues(top = if (!upper_bar_displaying) top_padding else 0.dp)
                    )
                }

                val vertical_padding: Dp = state.getDefaultVerticalPadding()
                val top_bar_padding: Dp = (
                    if (!upper_bar_displaying && !lower_bar_displaying) top_padding
                    else (
                        (if (upper_bar_displaying) upper_bar_height else 0.dp)
                        + (if (lower_bar_displaying) lower_bar_height else 0.dp)
                    )
                )

                Box {
                    Column {
                        with(page) {
                            Page(
                                state.ui.main_multiselect_context,
                                Modifier,
                                PaddingValues(
                                    top = vertical_padding + top_bar_padding,
                                    bottom =
                                        state.player.bottomPadding(
                                            include_np = true,
                                            include_top_items = true
                                        ) + vertical_padding + bottom_padding,
                                    start = horizontal_padding + WindowInsets.getStart(),
                                    end = horizontal_padding + WindowInsets.getEnd()
                                )
                            ) { state.ui.navigateBack() }
                        }
                    }

                    val above_player_slot: LayoutSlot =
                        when (form_factor) {
                            FormFactor.LANDSCAPE -> LandscapeLayoutSlot.ABOVE_PLAYER
                            FormFactor.PORTRAIT -> PortraitLayoutSlot.ABOVE_PLAYER
                        }

                    Box(
                        state.player
                            .topOffset(
                                Modifier,
                                NowPlayingTopOffsetSection.LAYOUT_SLOT,
                                apply_spacing = false,
                                displaying = true
                            )
                            .fillMaxWidth()
                            .align(Alignment.BottomEnd)
                    ) {
                        above_player_slot.DisplayBar(0.dp, Modifier.fillMaxWidth())
                    }
                }
            }
        }

        if (form_factor == FormFactor.LANDSCAPE) {
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

    val content_padding: PaddingValues =
        PaddingValues(
            top = WindowInsets.getTop()
        )

    val first_displaying: Boolean = first.DisplayBar(
        if (start) inner_bar_width else 0.dp,
        modifier.onSizeChanged {
            if (!start) {
                inner_bar_width = with (density) { it.width.toDp() }
            }
        },
        container_modifier = container_modifier.thenIf(start) { zIndex(1f) },
        content_padding = content_padding
    )

    val second_displaying: Boolean = second.DisplayBar(
        if (!start) inner_bar_width else 0.dp,
        modifier.onSizeChanged {
            if (start) {
                inner_bar_width = with (density) { it.width.toDp() }
            }
        },
        container_modifier = container_modifier.thenIf(!start) { zIndex(1f) },
        content_padding = content_padding
    )

    return Pair(first_displaying, second_displaying)
}
