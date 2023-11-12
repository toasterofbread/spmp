package com.toasterofbread.spmp.ui.layout.nowplaying.maintab

import LocalNowPlayingExpansion
import LocalPlayerState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.times
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.MINIMISED_NOW_PLAYING_HEIGHT_DP
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.MINIMISED_NOW_PLAYING_V_PADDING_DP
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.PlayerState
import com.toasterofbread.spmp.ui.layout.nowplaying.ExpansionState
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingExpansionState
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingPage.Companion.bottom_padding
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingPage.Companion.horizontal_padding
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingPage.Companion.horizontal_padding_minimised
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingPage.Companion.top_padding
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingTopBar
import com.toasterofbread.spmp.ui.layout.nowplaying.ThumbnailRow
import com.toasterofbread.spmp.ui.layout.nowplaying.getNPAltBackground
import com.toasterofbread.spmp.ui.layout.nowplaying.getNPOnBackground
import com.toasterofbread.spmp.ui.layout.nowplaying.overlay.DEFAULT_THUMBNAIL_ROUNDING
import com.toasterofbread.composekit.platform.composable.composeScope
import com.toasterofbread.composekit.utils.common.launchSingle
import com.toasterofbread.composekit.utils.composable.getTop
import kotlin.math.absoluteValue

private const val CONTROLS_MAX_HEIGHT_DP: Float = 400f
private const val QUEUE_OVERSCROLL_PADDING_DP: Float = 200f

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

    val start_padding: Dp = content_padding.calculateStartPadding(layout_direction) + current_horizontal_padding
    val end_padding: Dp = content_padding.calculateEndPadding(layout_direction) + current_horizontal_padding

    val page_width: Dp = player.screen_size.width - start_padding - end_padding

    val inner_bottom_padding: Dp = horizontal_padding
    val bottom_bar_height: Dp = (horizontal_padding * 3) + bottom_padding

    BoxWithConstraints(
        modifier = modifier.height(height)
    ) {
        Row(
            Modifier
                .offset {
                    IntOffset(
                        0,
                        (bottom_bar_height * (1f - absolute_expansion)).roundToPx()
                    )
                }
                .fillMaxWidth()
                .height(bottom_bar_height)
                .background(player.getNPAltBackground())
                .padding(
                    start = start_padding,
                    end = end_padding,
                    bottom = bottom_padding
                )
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.End
        ) {
            Spacer(Modifier.fillMaxWidth(0.5f))
        }

        Row(
            Modifier
                .fillMaxSize()
                .clipToBounds()
                .padding(
                    top = lerp(0.dp, top_padding, proportion_exp),
                    bottom = lerp(0.dp, bottom_padding + inner_bottom_padding, proportion_exp),
                    start = start_padding,
                    end = end_padding
                ),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            val extra_width: Dp = 0.dp//(page_width / 2) - page_height
            val parent_max_width: Dp = this@BoxWithConstraints.maxWidth
            val thumb_size: Dp = minOf(height, parent_max_width * 0.5f)

            composeScope {
                ThumbnailRow(
                    Modifier
                        .height(thumb_size)
                        .padding(start = ((extra_width / 2) * absolute_expansion).coerceAtLeast(0.dp))
                        .width(lerp(parent_max_width, thumb_size, absolute_expansion))
                        .offset {
                            IntOffset(
                                0,
                                (height - thumb_size).roundToPx() / 4
                            )
                        },
                    horizontal_arrangement = Arrangement.Start,
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

            val controls_height: Dp = page_height - top_padding - bottom_padding - inner_bottom_padding

            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
//                    // Fix size of controls to reduce compositions
//                    .requiredSize(
//                        minOf(page_height, parent_max_width / 2),
//                        controls_height
//                    )
            ) {
                composeScope(
                    remember { { it: Float -> seek_state = it } },
                    top_bar,
                    page_height
                ) { setSeekState, top_bar, page_height ->
                    val player = LocalPlayerState.current

                    Column(
                        Modifier
                            .fillMaxSize()
                            .offset {
                                IntOffset(
                                    ((1f - player.expansion.getBounded()) * page_height).roundToPx() / 2,
                                    (controls_height * 0.5f * (1f - player.expansion.getBounded())).roundToPx()
                                )
                            }
                            .graphicsLayer {
                                alpha = 1f - (1f - player.expansion.getBounded()).absoluteValue
                            },
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        composeScope {
                            top_bar.NowPlayingTopBar(expansion = ExpansionState.getStatic(1f))
                        }

                        val controls_visible by remember { derivedStateOf { player.expansion.getAbsolute() > 0.0f } }
                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (controls_visible) {
                                Controls(
                                    player.status.m_song,
                                    { seek_progress ->
                                        player.withPlayer {
                                            seekTo((duration_ms * seek_progress).toLong())
                                        }
                                        setSeekState(seek_progress)
                                    },
                                    Modifier.fillMaxWidth().height(CONTROLS_MAX_HEIGHT_DP.dp),
                                    vertical_arrangement = Arrangement.SpaceEvenly,
                                    font_size_multiplier = 2f,
                                    button_row_arrangement = Arrangement.spacedBy(5.dp, Alignment.Start),
                                    text_align = TextAlign.Start
                                )
                            }

                            Row(
                                Modifier.fillMaxWidth().align(Alignment.BottomEnd),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                var show_volume_slider: Boolean by remember { mutableStateOf(false) }
                                val bottom_row_colour = player.getNPOnBackground().copy(alpha = 0.5f)

                                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.CenterEnd) {
                                    this@Row.AnimatedVisibility(
                                        show_volume_slider,
                                        enter = expandHorizontally(),
                                        exit = shrinkHorizontally()
                                    ) {
                                        VolumeSlider(bottom_row_colour, reverse = true)
                                    }
                                }

                                IconButton({ show_volume_slider = !show_volume_slider }) {
                                    Icon(Icons.Default.VolumeUp, null, tint = bottom_row_colour)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
