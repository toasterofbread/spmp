package com.toasterofbread.spmp.ui.layout.nowplaying.maintab

import LocalNowPlayingExpansion
import LocalPlayerState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import androidx.compose.ui.zIndex
import com.toasterofbread.composekit.platform.composable.composeScope
import com.toasterofbread.composekit.utils.common.getContrasted
import com.toasterofbread.composekit.utils.common.thenIf
import com.toasterofbread.composekit.utils.composable.getTop
import com.toasterofbread.spmp.model.NowPlayingQueueWaveBorderMode
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
import com.toasterofbread.spmp.ui.layout.nowplaying.getNPAltOnBackground
import com.toasterofbread.spmp.ui.layout.nowplaying.getNPBackground
import com.toasterofbread.spmp.ui.layout.nowplaying.maintab.thumbnailrow.LargeThumbnailRow
import com.toasterofbread.spmp.ui.layout.nowplaying.queue.QUEUE_CORNER_RADIUS_DP
import com.toasterofbread.spmp.ui.layout.nowplaying.queue.QueueTab
import kotlin.math.absoluteValue

@Composable
internal fun NowPlayingMainTabPage.NowPlayingMainTabLarge(page_height: Dp, top_bar: NowPlayingTopBar, content_padding: PaddingValues, modifier: Modifier = Modifier) {
    val player: PlayerState = LocalPlayerState.current
    val expansion: NowPlayingExpansionState = LocalNowPlayingExpansion.current
    val layout_direction: LayoutDirection = LocalLayoutDirection.current

    val proportion: Float = WindowInsets.getTop() / player.screen_size.height
    val proportion_exp: Float by remember { derivedStateOf {
        (expansion.get().coerceIn(0f, 1f) * (1f - proportion)).coerceAtLeast(0f)
    } }

    val absolute_expansion: Float by remember { derivedStateOf {
        expansion.getAbsolute()
    } }

    val min_height: Dp = MINIMISED_NOW_PLAYING_HEIGHT_DP.dp - (MINIMISED_NOW_PLAYING_V_PADDING_DP.dp * 2)
    val height: Dp = ((absolute_expansion * (page_height - min_height)) + min_height).coerceAtLeast(min_height)

    val current_horizontal_padding: Dp = lerp(horizontal_padding_minimised, horizontal_padding, absolute_expansion)

    val top_padding: Dp = top_padding
    val bottom_padding: Dp = bottom_padding
    val start_padding: Dp = content_padding.calculateStartPadding(layout_direction) + current_horizontal_padding
    val end_padding: Dp = content_padding.calculateEndPadding(layout_direction) + current_horizontal_padding

    val bottom_bar_height: Dp = (horizontal_padding * 2) + bottom_padding
    val inner_bottom_padding: Dp = horizontal_padding

    BoxWithConstraints(
        modifier = modifier.height(height)
    ) {
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
            val thumb_size: Dp = minOf(height, parent_max_width * 0.5f)

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
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            player.expansion.close()
                        }
                ) {
                    val background_colour: Color =
                        when (player.np_theme_mode) {
                            ThemeMode.BACKGROUND -> player.theme.accent
                            else -> player.theme.background
                        }

                    Canvas(Modifier.fillMaxSize()) {
                        drawLine(
                            when (player.np_theme_mode) {
                                ThemeMode.BACKGROUND -> player.getNPAltBackground()
                                ThemeMode.ELEMENTS -> player.theme.accent
                                ThemeMode.NONE -> player.theme.on_background
                            },
                            start = Offset.Zero,
                            end = Offset(size.width, 0f),
                            strokeWidth = 5.dp.toPx()
                        )

                        drawRect(background_colour)
                    }

                    CompositionLocalProvider(LocalContentColor provides background_colour.getContrasted()) {
                        LargeBottomBar(
                            Modifier
                                .fillMaxWidth(0.5f)
                                .fillMaxHeight()
                                .align(Alignment.CenterEnd)
                                .padding(horizontal = 10.dp)
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

                        LargeThumbnailRow(
                            Modifier
                                .height(thumb_size)
                                .padding(start = (extra_width * absolute_expansion / 2).coerceAtLeast(0.dp))
                                .width(lerp(parent_max_width, thumb_size, absolute_expansion)),
                            onThumbnailLoaded = { song, image ->
                                onThumbnailLoaded(song, image)
                            },
                            setThemeColour = {
                                setThemeColour(it, true)
                            },
                            getSeekState = { seek_state },
                            disable_parent_scroll_while_menu_open = false
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
                    .padding(start = 10.dp)
            ) {
                composeScope(
                    remember { { it: Float -> seek_state = it } },
                    top_bar,
                    page_height
                ) { setSeekState, top_bar, page_height ->
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
                        val controls_visible by remember { derivedStateOf { player.expansion.getAbsolute() > 0.0f } }
                        Column(
                            Modifier
                                .weight(1f)
                                .fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(Modifier.fillMaxHeight(0.075f))

                            if (controls_visible) {
                                Controls(
                                    player.status.m_song,
                                    { seek_progress ->
                                        player.withPlayer {
                                            seekTo((duration_ms * seek_progress).toLong())
                                        }
                                        setSeekState(seek_progress)
                                    },
                                    Modifier.fillMaxWidth(),
                                    vertical_arrangement = Arrangement.spacedBy(10.dp),
                                    title_text_max_lines = 2,
                                    title_font_size = 35.sp,
                                    artist_font_size = 18.sp,
                                    seek_bar_next_to_buttons = true,
                                    text_align = TextAlign.Start,
                                    getBackgroundColour = { theme.background },
                                    getOnBackgroundColour = { theme.on_background },
                                    getAccentColour = { theme.accent }
                                )
                            }

                            QueueTab(
                                null,
                                Modifier
                                    .fillMaxHeight()
                                    .weight(1f)
                                    .thenIf(player.np_theme_mode != ThemeMode.BACKGROUND) {
                                        border(
                                            2.dp,
                                            if (player.np_theme_mode == ThemeMode.ELEMENTS) player.theme.accent
                                            else player.theme.on_background,
                                            RoundedCornerShape(QUEUE_CORNER_RADIUS_DP.dp)
                                        )
                                    },
                                inline = true,
                                border_thickness = 2.dp,
                                wave_border_mode_override = NowPlayingQueueWaveBorderMode.SCROLL,
                                button_row_arrangement = Arrangement.spacedBy(5.dp),
                                content_padding = PaddingValues(bottom = inner_bottom_padding),
                                getBackgroundColour = {
                                    if (player.np_theme_mode == ThemeMode.BACKGROUND) getNPAltOnBackground()
                                    else theme.background
                                },
                                getOnBackgroundColour = {
                                    when (player.np_theme_mode) {
                                        ThemeMode.BACKGROUND -> getNPBackground()
                                        ThemeMode.ELEMENTS -> theme.accent
                                        ThemeMode.NONE -> theme.on_background
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
