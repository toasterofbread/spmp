package com.toasterofbread.spmp.ui.layout.nowplaying.maintab

import LocalNowPlayingExpansion
import LocalPlayerState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.zIndex
import com.github.krottv.compose.sliders.lerp
import com.toasterofbread.composekit.platform.composable.composeScope
import com.toasterofbread.composekit.utils.common.*
import com.toasterofbread.composekit.utils.composable.getTop
import com.toasterofbread.spmp.model.mediaitem.MediaItemThumbnailProvider
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.song.observeThumbnailRounding
import com.toasterofbread.spmp.model.settings.category.NowPlayingQueueWaveBorderMode
import com.toasterofbread.spmp.ui.component.Thumbnail
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
import com.toasterofbread.spmp.ui.layout.nowplaying.overlay.DEFAULT_THUMBNAIL_ROUNDING
import com.toasterofbread.spmp.ui.layout.nowplaying.queue.QueueTab
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

@Composable
private fun MainTabControls(
    onSeek: (Float) -> Unit,
    enabled: Boolean,
    button_size: Dp,
    seek_bar_next_to_buttons: Boolean,
    modifier: Modifier = Modifier,
    textRowStartContent: @Composable RowScope.() -> Unit = {}
) {
    val player: PlayerState = LocalPlayerState.current

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
        button_size = button_size,
        seek_bar_next_to_buttons = seek_bar_next_to_buttons,
        button_row_arrangement = Arrangement.Start,
        text_align = TextAlign.Start,
        textRowStartContent = textRowStartContent
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

    val bottom_bar_height: Dp = MINIMISED_NOW_PLAYING_HEIGHT_DP.dp
    val inner_bottom_padding: Dp = horizontal_padding

    var thumbnail_y_position: Float by remember { mutableStateOf(0f) }

    val bar_background_colour: Color = player.theme.card
    val stroke_width: Dp = 1.dp
    val stroke_colour: Color = bar_background_colour.amplify(255f)

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
            val parent_max_width: Dp = this@BoxWithConstraints.maxWidth
            val thumb_size: Dp = minOf(height, parent_max_width * 0.5f) - (inner_padding)

            var actual_thumb_size: DpSize by remember { mutableStateOf(DpSize.Zero) }
            var actual_thumb_position: DpOffset by remember { mutableStateOf(DpOffset.Zero) }

            val controls_target_height: Dp = 200.dp
            var controls_height: Dp by remember { mutableStateOf(0.dp) }

            val column_min_width: Dp = 300.dp
            val column_width: Dp =
                lerp(
                    parent_max_width,
                    maxOf(
                        column_min_width,
                        minOf(
                            this@BoxWithConstraints.maxHeight - controls_height,
                            thumb_size
                        )
                    ),
                    absolute_expansion
                )

            val current_thumb_size: Dp = this@BoxWithConstraints.maxHeight - controls_height
            val compact_mode: Boolean = absolute_expansion > 0.9f && (
                 (current_thumb_size / column_width) < 0.75f
            )

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
                    Canvas(Modifier.fillMaxSize().offset(x = (-0.5).dp, y = 3.dp)) {
                        drawLine(
                            stroke_colour,
                            start = Offset.Zero,
                            end = Offset(size.width, 0f),
                            strokeWidth = (stroke_width + 2.dp).toPx()
                        )

                        drawRect(bar_background_colour)
                    }

                    CompositionLocalProvider(LocalContentColor provides bar_background_colour.getContrasted()) {
                        var bottom_bar_position: DpOffset by remember { mutableStateOf(DpOffset.Zero) }
                        val inset_depth: Dp = (actual_thumb_position.y + actual_thumb_size.height - bottom_bar_position.y).coerceAtLeast(0.dp)

                        LargeBottomBar(
                            Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxWidth()
                                .padding(horizontal = 15.dp)
                                .onGloballyPositioned {
                                    bottom_bar_position = with (density) {
                                        val position: Offset = it.positionInRoot()
                                        DpOffset(position.x.toDp(), position.y.toDp())
                                    }
                                },
                            inset_start = actual_thumb_position.x - bottom_bar_position.x,
                            inset_end = actual_thumb_position.x + actual_thumb_size.width - bottom_bar_position.x,
                            inset_depth = if (compact_mode) 0.dp else inset_depth
                        )
                    }
                }
            }

            composeScope {
                Column(
                    Modifier.fillMaxHeight().zIndex(2f),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    BoxWithConstraints(
                        Modifier.fillMaxHeight().weight(1f)
                    ) {
                        Column(
                            Modifier.width(column_width),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {

                            Spacer(Modifier.requiredHeight(inner_bottom_padding).weight(1f, false))

                            Spacer(Modifier.fillMaxHeight().weight(1f))

                            MainTabControls(
                                { seek_state = it },
                                enabled = expanded,
                                button_size = 50.dp,
                                seek_bar_next_to_buttons = !compact_mode,
                                modifier = Modifier
                                    .thenIf(!compact_mode) {
                                        requiredHeight(controls_target_height * ((absolute_expansion - 0.5f) * 2f))
                                    }
                                    .scale(1f, absolute_expansion)
                                    .padding(bottom = 20.dp)
                                    .onSizeChanged {
                                        controls_height = with (density) {
                                            it.height.toDp()
                                        }
                                    },
                                textRowStartContent = {
                                    if (compact_mode) {
                                        val song: Song? by player.status.song_state

                                        val thumbnail_rounding: Int = song.observeThumbnailRounding(DEFAULT_THUMBNAIL_ROUNDING)
                                        val thumbnail_shape: RoundedCornerShape = RoundedCornerShape(thumbnail_rounding)

                                        song?.Thumbnail(
                                            MediaItemThumbnailProvider.Quality.HIGH,
                                            Modifier
                                                .padding(end = 10.dp)
                                                .size(controls_target_height - 100.dp)
                                                .clip(thumbnail_shape),
                                            onLoaded = {
                                                onThumbnailLoaded(song, it)
                                            }
                                        )
                                    }
                                }
                            )

                            val thumbnail_row_height: Dp =
                                if (compact_mode) (1f - absolute_expansion) * thumb_size
                                else thumb_size
                            
                            if (!compact_mode || absolute_expansion < 1f) {
                                LargeThumbnailRow(
                                    Modifier
                                        .height(thumbnail_row_height)
                                        .graphicsLayer {
                                            alpha = if (compact_mode) 1f - absolute_expansion else 1f
                                        }
                                        .offset {
                                            IntOffset(
                                                (((column_width - actual_thumb_size.width).toPx() / 2) * absolute_expansion).roundToInt(),
                                                0
                                            )
                                        }
                                        .onGloballyPositioned {
                                            thumbnail_y_position = with (density) {(
                                                it.positionInParent().y
                                                + lerp(-controls_target_height.toPx() / 2f, 0f, 1f - absolute_expansion)
                                                - 50.dp.toPx()
                                            )}
                                        },
                                    onThumbnailLoaded = { song, image ->
                                        onThumbnailLoaded(song, image)
                                    },
                                    setThemeColour = {
                                        setThemeColour(it, true)
                                    },
                                    getSeekState = { seek_state },
                                    disable_parent_scroll_while_menu_open = false,
                                    thumbnail_modifier = Modifier.onGloballyPositioned {
                                        with (density) {
                                            val position: Offset = it.positionInRoot()
                                            actual_thumb_position = DpOffset(position.x.toDp(), position.y.toDp())
                                            actual_thumb_size = DpSize(it.size.width.toDp(), it.size.height.toDp())
                                        }
                                    }
                                )
                            }

                            Spacer(Modifier.requiredHeight(animateDpAsState(inner_bottom_padding * compact_mode.toInt()).value).weight(1f, false))

                            Spacer(Modifier.fillMaxHeight().weight(1f))
                        }
                    }

                    Spacer(Modifier.requiredHeight(lerp(0.dp, inner_bottom_padding, proportion_exp)))
                }
            }

            val current_controls_height: Dp = page_height - top_padding - bottom_padding - inner_bottom_padding

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
                                    (current_controls_height * (1f - player.expansion.getBounded())).roundToPx()
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
                            val queue_shape: Shape = RoundedCornerShape(10.dp)

                            QueueTab(
                                null,
                                Modifier
                                    .fillMaxHeight()
                                    .weight(1f)
                                    .thenIf(player.np_theme_mode != ThemeMode.BACKGROUND) {
                                        border(
                                            stroke_width,
                                            stroke_colour,
                                            queue_shape
                                        )
                                    },
                                inline = true,
                                border_thickness = stroke_width + 1.dp,
                                wave_border_mode_override = NowPlayingQueueWaveBorderMode.SCROLL,
                                shape = queue_shape,
                                button_row_arrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
                                content_padding = PaddingValues(
                                    bottom = inner_bottom_padding.coerceAtLeast(0.dp) + 35.dp
                                ),
                                getBackgroundColour = {
                                    getNPAltBackground()
//                                    if (player.np_theme_mode == ThemeMode.BACKGROUND) getNPAltOnBackground()
//                                    else theme.background
                                },
                                getOnBackgroundColour = {
                                    when (player.np_theme_mode) {
                                        ThemeMode.BACKGROUND -> theme.vibrant_accent
                                        ThemeMode.ELEMENTS -> theme.accent
                                        ThemeMode.NONE -> theme.on_background
                                    }
                                },
//                                getWaveBorderColour = { stroke_colour }
                            )
                        }
                    }
                }
            }
        }
    }
}
