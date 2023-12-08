package com.toasterofbread.spmp.ui.layout.nowplaying.maintab

import LocalNowPlayingExpansion
import LocalPlayerState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import androidx.compose.ui.zIndex
import com.github.krottv.compose.sliders.lerp
import com.toasterofbread.composekit.platform.composable.composeScope
import com.toasterofbread.composekit.utils.common.amplify
import com.toasterofbread.composekit.utils.common.blendWith
import com.toasterofbread.composekit.utils.common.getContrasted
import com.toasterofbread.composekit.utils.common.thenIf
import com.toasterofbread.composekit.utils.common.toFloat
import com.toasterofbread.composekit.utils.composable.getTop
import com.toasterofbread.spmp.model.settings.category.NowPlayingQueueWaveBorderMode
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.MINIMISED_NOW_PLAYING_HEIGHT_DP
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.MINIMISED_NOW_PLAYING_V_PADDING_DP
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.PlayerState
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingExpansionState
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingPage.Companion.bottom_padding
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingPage.Companion.horizontal_padding
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingPage.Companion.horizontal_padding_minimised
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingPage.Companion.top_padding
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingTopBar
import com.toasterofbread.spmp.ui.layout.nowplaying.ThemeMode
import com.toasterofbread.spmp.ui.layout.nowplaying.getNPAltBackground
import com.toasterofbread.spmp.ui.layout.nowplaying.maintab.thumbnailrow.LargeThumbnailRow
import com.toasterofbread.spmp.ui.layout.nowplaying.queue.QueueTab
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

private enum class ControlsPosition {
    ABOVE_IMAGE, ABOVE_QUEUE
}

@Composable
private fun MainTabControls(
    onSeek: (Float) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val player: PlayerState = LocalPlayerState.current
    val expansion: NowPlayingExpansionState = LocalNowPlayingExpansion.current
    val absolute_expansion: Float = expansion.getAbsolute()

    Controls(
        player.status.m_song,
        { seek_progress ->
            player.withPlayer {
                seekTo((duration_ms * seek_progress).toLong())
            }
            onSeek(seek_progress)
        },
        modifier.padding(top = 30.dp),
        enabled = enabled,
        vertical_arrangement = Arrangement.spacedBy(10.dp),
        title_text_max_lines = 2,
        title_font_size = 25.sp,
        artist_font_size = 18.sp,
        seek_bar_next_to_buttons = true,
        text_align = TextAlign.Start,
        getBackgroundColour = { theme.background },
        getOnBackgroundColour = { theme.on_background },
        getAccentColour = { theme.accent }
    )
}

@Composable
internal fun NowPlayingMainTabPage.NowPlayingMainTabLarge(page_height: Dp, top_bar: NowPlayingTopBar, content_padding: PaddingValues, modifier: Modifier = Modifier) {
    val player: PlayerState = LocalPlayerState.current
    val expansion: NowPlayingExpansionState = LocalNowPlayingExpansion.current
    val layout_direction: LayoutDirection = LocalLayoutDirection.current
    val density: Density = LocalDensity.current

    val proportion: Float = WindowInsets.getTop() / player.screen_size.height
    val proportion_exp: Float by remember { derivedStateOf {
        (expansion.get().coerceIn(0f, 1f) * (1f - proportion)).coerceAtLeast(0f)
    } }

    val absolute_expansion: Float by remember { derivedStateOf {
        expansion.getAbsolute()
    } }
    val expanded: Boolean by remember { derivedStateOf { absolute_expansion >= 1f } }

    val min_height: Dp = MINIMISED_NOW_PLAYING_HEIGHT_DP.dp - (MINIMISED_NOW_PLAYING_V_PADDING_DP.dp * 2)
    val height: Dp = ((absolute_expansion * (page_height - min_height)) + min_height).coerceAtLeast(min_height)

    val current_horizontal_padding: Dp = lerp(horizontal_padding_minimised, horizontal_padding, absolute_expansion)

    val inner_padding: Dp = lerp(0.dp, 25.dp, absolute_expansion)
    val start_padding: Dp = maxOf(inner_padding, content_padding.calculateStartPadding(layout_direction) + current_horizontal_padding)
    val end_padding: Dp = maxOf(inner_padding, content_padding.calculateEndPadding(layout_direction) + current_horizontal_padding)

    val top_padding: Dp = top_padding
    val bottom_padding: Dp = bottom_padding

    val bottom_bar_height: Dp = (horizontal_padding * 2) + bottom_padding
    val inner_bottom_padding: Dp = horizontal_padding

    var controls_y_position: Float by remember { mutableStateOf(0f) }
    var thumbnail_y_position: Float by remember { mutableStateOf(0f) }
    var display_mode: ControlsPosition by remember { mutableStateOf(ControlsPosition.ABOVE_IMAGE) }
    val display_mode_transition: Float by animateFloatAsState((display_mode == ControlsPosition.ABOVE_IMAGE).toFloat())

    val bar_background_colour: Color = player.theme.card
    val stroke_width: Dp = 1.dp
    val stroke_colour: Color = bar_background_colour.amplify(255f)

    BoxWithConstraints(
        modifier = modifier.height(height)
    ) {
        LargeTopBar(
            Modifier
                .fillMaxWidth()
                .height(bottom_bar_height)
                .graphicsLayer { alpha = absolute_expansion }
                .align(Alignment.TopStart)
                .background(bar_background_colour)
                .drawWithContent {
                    drawContent()
                    drawLine(stroke_colour, Offset(0f, size.height), Offset(size.width, size.height), stroke_width.toPx())
                }
                .zIndex(1f)
        )

        Row(
            Modifier
                .fillMaxSize()
                .clipToBounds()
                .padding(
                    top = lerp(0.dp, top_padding, proportion_exp),
                    bottom = lerp(0.dp, bottom_padding, proportion_exp),
                    start = start_padding,
                    end = end_padding
                ),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            val extra_width: Dp = 0.dp//(page_width / 2) - page_height
            val parent_max_width: Dp = this@BoxWithConstraints.maxWidth
            val thumb_size: Dp = minOf(height, parent_max_width * 0.5f) - (inner_padding)

            Box(Modifier.requiredSize(0.dp).zIndex(1f)) {
                Box(
                    Modifier
                        .requiredSize(parent_max_width, bottom_bar_height)
                        .offset {
                            IntOffset(
                                ((parent_max_width / 2) - start_padding).roundToPx(),
                                (page_height - (bottom_bar_height * absolute_expansion / 2) - top_padding - 2.dp).roundToPx()
                            )
                        }
                        .pointerInput(Unit) {
                            detectTapGestures {
                                player.expansion.close()
                            }
                        }
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawLine(
                            stroke_colour,
                            start = Offset.Zero,
                            end = Offset(size.width, 0f),
                            strokeWidth = (stroke_width + 2.dp).toPx()
                        )

                        drawRect(bar_background_colour)
                    }

                    CompositionLocalProvider(LocalContentColor provides bar_background_colour.getContrasted()) {
                        LargeBottomBar(
                            Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxWidth()
                                .padding(horizontal = 15.dp)
                        )
                    }
                }
            }

            composeScope {
                Column(
                    Modifier.fillMaxHeight().zIndex(2f),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        Modifier.fillMaxHeight().weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Spacer(Modifier.requiredHeight(inner_bottom_padding).weight(1f, false))

                        var controls_height: Int by remember { mutableStateOf(0) }

                        MainTabControls(
                            { seek_state = it },
                            expanded && display_mode == ControlsPosition.ABOVE_IMAGE,
                            Modifier
                                .thenIf(!expanded) {
                                    requiredHeight(with (density) {
                                        controls_height.toDp() * ((absolute_expansion - 0.5f) * 2f)
                                    })
                                }
                                .onGloballyPositioned {
                                    controls_y_position = it.positionInParent().y
                                }
                                .onSizeChanged {
                                    if (expanded) {
                                        controls_height = it.height
                                    }
                                }
                                .graphicsLayer {
                                    alpha = display_mode_transition
                                }
                                .scale(1f, absolute_expansion)
                                .padding(bottom = 20.dp)
                                .width(lerp(parent_max_width, thumb_size, absolute_expansion))
                        )

                        LargeThumbnailRow(
                            Modifier
                                .height(thumb_size)
                                .padding(start = (extra_width * absolute_expansion / 2).coerceAtLeast(0.dp))
                                .width(lerp(parent_max_width, thumb_size, absolute_expansion))
                                .onGloballyPositioned {
                                    thumbnail_y_position = with (density) {(
                                        it.positionInParent().y
                                        + lerp(-controls_height / 2f, 0f, maxOf(display_mode_transition, 1f - absolute_expansion))
                                        - 50.dp.toPx()
                                    )}
                                }
                                .offset {
                                    IntOffset(
                                        0,
                                        lerp(-controls_height / 2f, 0f, maxOf(display_mode_transition, 1f - absolute_expansion)).roundToInt()
                                    )
                                },
                            onThumbnailLoaded = { song, image ->
                                onThumbnailLoaded(song, image)
                            },
                            setThemeColour = {
                                setThemeColour(it, true)
                            },
                            getSeekState = { seek_state },
                            disable_parent_scroll_while_menu_open = false
                        )

                        Spacer(
                            Modifier.onGloballyPositioned {
                                with (density) {
                                    if (expanded) {
                                        display_mode =
                                            if (it.positionInWindow().y.toDp() > (page_height - bottom_bar_height - inner_bottom_padding)) ControlsPosition.ABOVE_QUEUE
                                            else ControlsPosition.ABOVE_IMAGE
                                    }
                                }
                            }
                        )
                    }

                    Spacer(Modifier.requiredHeight(lerp(0.dp, inner_bottom_padding, proportion_exp)))
                }
            }

            val controls_height: Dp = page_height - top_padding - bottom_padding - inner_bottom_padding

            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(start = inner_padding)
            ) {
                composeScope(
                    top_bar,
                    page_height
                ) { top_bar, page_height ->
                    val player: PlayerState = LocalPlayerState.current

                    Column(
                        Modifier
                            .fillMaxSize()
                            .offset {
                                IntOffset(
                                    ((1f - player.expansion.getBounded()) * page_height).roundToPx() / 2,
                                    (controls_height * (1f - player.expansion.getBounded())).roundToPx()
                                )
                            }
                            .graphicsLayer {
                                alpha = 1f - (1f - player.expansion.getBounded()).absoluteValue
                            },
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(
                            Modifier
                                .weight(1f)
                                .fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {

                            AnimatedVisibility(
                                display_mode == ControlsPosition.ABOVE_QUEUE,
                                Modifier.offset {
                                    IntOffset(
                                        0,
                                        thumbnail_y_position.roundToInt()
                                    )
                                }
                            ) {
                                MainTabControls(
                                    { seek_state = it },
                                    expanded
                                )
                            }

                            val queue_shape: Shape = RoundedCornerShape(10.dp)
                            val getVerticalOffset: () -> Int = remember {{ lerp(thumbnail_y_position, controls_y_position, display_mode_transition).roundToInt() }}

                            QueueTab(
                                null,
                                Modifier
                                    .fillMaxHeight()
                                    .weight(1f)
                                    .offset {
                                        IntOffset(
                                            0,
                                            getVerticalOffset()
                                        )
                                    }
                                    .thenIf(player.np_theme_mode != ThemeMode.BACKGROUND) {
                                        border(
                                            stroke_width,
                                            stroke_colour,
                                            queue_shape
                                        )
                                    },
                                inline = true,
                                border_thickness = stroke_width,
                                wave_border_mode_override = NowPlayingQueueWaveBorderMode.SCROLL,
                                shape = queue_shape,
                                button_row_arrangement = Arrangement.spacedBy(5.dp),
                                content_padding = PaddingValues(
                                    bottom = inner_bottom_padding + with (density) { getVerticalOffset().toDp() }
                                ),
                                getBackgroundColour = {
                                    getNPAltBackground()
//                                    if (player.np_theme_mode == ThemeMode.BACKGROUND) getNPAltOnBackground()
//                                    else theme.background
                                },
                                getOnBackgroundColour = {
                                    theme.accent.blendWith(theme.background, 0.01f)
//                                    when (player.np_theme_mode) {
//                                        ThemeMode.BACKGROUND -> getNPBackground()
//                                        ThemeMode.ELEMENTS -> theme.accent
//                                        ThemeMode.NONE -> theme.on_background
//                                    }
                                },
                                getWaveBorderColour = { stroke_colour }
                            )
                        }
                    }
                }
            }
        }
    }
}