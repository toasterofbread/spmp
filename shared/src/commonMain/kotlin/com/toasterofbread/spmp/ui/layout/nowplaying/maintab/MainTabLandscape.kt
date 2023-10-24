package com.toasterofbread.spmp.ui.layout.nowplaying.maintab

import LocalNowPlayingExpansion
import LocalPlayerState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.swipeable
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.times
import com.toasterofbread.spmp.platform.BackHandler
import com.toasterofbread.spmp.platform.composeScope
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.MINIMISED_NOW_PLAYING_HEIGHT_DP
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.MINIMISED_NOW_PLAYING_V_PADDING_DP
import com.toasterofbread.spmp.ui.layout.nowplaying.ExpansionState
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingPage.Companion.bottom_padding
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingPage.Companion.horizontal_padding
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingPage.Companion.top_padding
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingTopBar
import com.toasterofbread.spmp.ui.layout.nowplaying.ThumbnailRow
import com.toasterofbread.spmp.ui.layout.nowplaying.getNPOnBackground
import com.toasterofbread.spmp.ui.layout.nowplaying.overlay.DEFAULT_THUMBNAIL_ROUNDING
import com.toasterofbread.spmp.ui.layout.nowplaying.queue.QueueTab
import com.toasterofbread.utils.common.launchSingle
import com.toasterofbread.utils.common.setAlpha
import kotlin.math.absoluteValue

private const val CONTROLS_MAX_HEIGHT_DP: Float = 400f
private const val QUEUE_OVERSCROLL_PADDING_DP: Float = 200f

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun NowPlayingMainTabPage.NowPlayingMainTabLandscape(page_height: Dp, top_bar: NowPlayingTopBar, modifier: Modifier = Modifier) {
    val player = LocalPlayerState.current
    val expansion = LocalNowPlayingExpansion.current

    val proportion: Float = player.context.getStatusBarHeightDp() / player.screen_size.height
    val proportion_exp: Float by remember { derivedStateOf {
        (expansion.get().coerceIn(0f, 1f) * (1f - proportion)).coerceAtLeast(0f)
    } }

    val absolute_expansion: Float by remember { derivedStateOf {
        expansion.getAbsolute()
    } }
    val min_height = MINIMISED_NOW_PLAYING_HEIGHT_DP.dp - (MINIMISED_NOW_PLAYING_V_PADDING_DP.dp * 2)
    val height = ((absolute_expansion * (page_height - min_height)) + min_height).coerceAtLeast(min_height)

    val queue_swipe_coroutine_scope = rememberCoroutineScope()
    val queue_swipe_state = rememberSwipeableState(1)

    Row(
        modifier = modifier
            .height(height)
            .padding(horizontal = horizontal_padding)
            .padding(
                bottom = lerp(0.dp, bottom_padding, proportion_exp),
                top = lerp(0.dp, top_padding, proportion_exp)
            ),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        val extra_width: Dp = (player.screen_size.width / 2) - page_height

        val thumbnail_rounding: Int? = player.status.m_song?.ThumbnailRounding?.observe(player.context.database)?.value
        val thumbnail_shape: Shape = RoundedCornerShape(thumbnail_rounding ?: DEFAULT_THUMBNAIL_ROUNDING)

        BoxWithConstraints(
            Modifier
                .fillMaxWidth(0.5f + 0.5f * (1f - absolute_expansion))
                .padding(start = ((extra_width / 2) * absolute_expansion).coerceAtLeast(0.dp))
                .height(height),
            contentAlignment = Alignment.Center
        ) {
            val queue_swipe_modifier = Modifier.swipeable(
                queue_swipe_state,
                anchors = mapOf(
                    page_height.value to 0,
                    0f to 1
                ),
                orientation = Orientation.Vertical,
                enabled = absolute_expansion >= 1f,
                reverseDirection = false
            )

            LaunchedEffect(absolute_expansion <= 0f) {
                if (absolute_expansion > 0f) {
                    return@LaunchedEffect
                }
                queue_swipe_state.snapTo(0)
            }

            val thumb_size = minOf(height, maxWidth)

            composeScope {
                ThumbnailRow(
                    queue_swipe_modifier.height(thumb_size),
                    horizontal_arrangement = Arrangement.Start,
                    onThumbnailLoaded = { song, image ->
                        onThumbnailLoaded(song, image)
                    },
                    setThemeColour = {
                        setThemeColour(it, true)
                    },
                    getSeekState = { seek_state },
                    disable_parent_scroll_while_menu_open = false
                ) {
                    BackHandler({ queue_swipe_state.targetValue == 1 }) {
                        queue_swipe_coroutine_scope.launchSingle {
                            queue_swipe_state.animateTo(0)
                        }
                    }

                    val hide: Boolean by remember { derivedStateOf {
                        absolute_expansion <= 0f || (queue_swipe_state.offset.value == page_height.value && !queue_swipe_state.isAnimationRunning)
                    } }

                    if (!hide) {
                        QueueTab(
                            thumb_size + QUEUE_OVERSCROLL_PADDING_DP.dp,
                            top_bar,
                            Modifier
                                .graphicsLayer { alpha = (absolute_expansion * 2f).coerceAtMost(1f) }
                                .offset {
                                    IntOffset(
                                        0,
                                        ((QUEUE_OVERSCROLL_PADDING_DP.dp / 2) + queue_swipe_state.offset.value.dp).roundToPx()
                                    )
                                }
                                .then(queue_swipe_modifier),
                            inline = true,
                            shape = thumbnail_shape,
                            padding = PaddingValues(top = 10.dp)
                        )
                    }
                }
            }
        }

        Box(Modifier.requiredSize(page_height)) {
            composeScope(
                remember { { it: Float -> seek_state = it } },
                top_bar,
                remember {{
                    queue_swipe_coroutine_scope.launchSingle {
                        queue_swipe_state.animateTo(
                            if (queue_swipe_state.targetValue == 0) 1
                            else 0
                        )
                    }
                }},
                page_height
            ) { setSeekState, top_bar, toggleQueue, page_height ->
                val player = LocalPlayerState.current

                Column(
                    Modifier
                        .fillMaxSize()
                        .offset {
                            IntOffset(
                                ((1f - player.expansion.getBounded()) * page_height).roundToPx() / 2,
                                0
                            )
                        }
                        .graphicsLayer {
                            alpha = 1f - (1f - player.expansion.getBounded()).absoluteValue
                        }
                ) {
                    composeScope {
                        top_bar.NowPlayingTopBar(expansion = ExpansionState.getStatic(1f))
                    }

                    val controls_visible by remember { derivedStateOf { player.expansion.getAbsolute() > 0.0f } }
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .padding(horizontal = horizontal_padding),
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
                                font_size_multiplier = 1.2f,
                                button_row_arrangement = Arrangement.spacedBy(5.dp, Alignment.Start),
                                text_align = TextAlign.Start
                            )
                        }
//
                        Row(
                            Modifier.fillMaxWidth().align(Alignment.BottomEnd),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            var show_volume_slider: Boolean by remember { mutableStateOf(false) }
                            val bottom_row_colour = player.getNPOnBackground().setAlpha(0.5f)

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

                            IconButton({ toggleQueue() }) {
                                Icon(Icons.Default.QueueMusic, null, tint = bottom_row_colour)
                            }
                        }
                    }
                }
            }
        }
    }
}
