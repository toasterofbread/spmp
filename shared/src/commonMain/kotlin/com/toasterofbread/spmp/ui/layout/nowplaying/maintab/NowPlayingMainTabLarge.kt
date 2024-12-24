package com.toasterofbread.spmp.ui.layout.nowplaying.maintab

import LocalNowPlayingExpansion
import LocalPlayerState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import androidx.compose.ui.zIndex
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.song.observeThumbnailRounding
import com.toasterofbread.spmp.model.settings.category.NowPlayingQueueWaveBorderMode
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.component.Thumbnail
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.MINIMISED_NOW_PLAYING_HEIGHT_DP
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.MINIMISED_NOW_PLAYING_V_PADDING_DP
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingPage.Companion.bottom_padding
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingPage.Companion.horizontal_padding
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingPage.Companion.horizontal_padding_minimised
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingPage.Companion.top_padding
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingTopBar
import com.toasterofbread.spmp.ui.layout.nowplaying.PlayerExpansionState
import com.toasterofbread.spmp.ui.layout.nowplaying.ThemeMode
import com.toasterofbread.spmp.ui.layout.nowplaying.getNPAltBackground
import com.toasterofbread.spmp.ui.layout.nowplaying.maintab.thumbnailrow.LargeThumbnailRow
import com.toasterofbread.spmp.ui.layout.nowplaying.maintab.thumbnailrow.songThumbnailShadow
import com.toasterofbread.spmp.ui.layout.nowplaying.queue.QueueTab
import dev.toastbits.composekit.components.platform.composable.composeScope
import dev.toastbits.composekit.components.utils.composable.getTop
import dev.toastbits.composekit.theme.core.vibrantAccent
import dev.toastbits.composekit.util.amplify
import dev.toastbits.composekit.util.composable.getValue
import dev.toastbits.composekit.util.getContrasted
import dev.toastbits.composekit.util.thenIf
import dev.toastbits.composekit.util.toInt
import dev.toastbits.ytmkt.model.external.ThumbnailProvider
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

val NOW_PLAYING_LARGE_BOTTOM_BAR_HEIGHT: Dp
    @Composable get() = MINIMISED_NOW_PLAYING_HEIGHT_DP.dp

private const val STROKE_WIDTH_DP: Float = 1f
private const val INNER_PADDING_DP: Float = 25f
private const val CONTROLS_IMAGE_SEPARATION_DP: Float = 20f

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
                if (duration_ms <= 0) {
                    return@withPlayer
                }

                seekTo((duration_ms * seek_progress).toLong())
                onSeek(seek_progress)
            }
        },
        modifier.padding(top = 30.dp),
        enabled = enabled,
        vertical_arrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
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
    val expansion: PlayerExpansionState = LocalNowPlayingExpansion.current
    val layout_direction: LayoutDirection = LocalLayoutDirection.current
    val density: Density = LocalDensity.current

    val swap_controls_and_image: Boolean by player.settings.Player.LANDSCAPE_SWAP_CONTROLS_AND_IMAGE.observe()

    val proportion: Float = WindowInsets.getTop() / page_height
    val proportion_exp: Float by remember { derivedStateOf {
        (expansion.get().coerceIn(0f, 1f) * (1f - proportion)).coerceAtLeast(0f)
    } }

    val absolute_expansion: Float by remember { derivedStateOf {
        expansion.getAbsolute()
    } }
    val expanded: Boolean by remember { derivedStateOf { absolute_expansion >= 1f } }

    val v_padding: Dp = MINIMISED_NOW_PLAYING_V_PADDING_DP.dp
    val min_height: Dp = MINIMISED_NOW_PLAYING_HEIGHT_DP.dp// - (MINIMISED_NOW_PLAYING_V_PADDING_DP.dp * 2)
    val height: Dp = ((absolute_expansion * (page_height - min_height)) + min_height).coerceAtLeast(min_height)

    val inner_padding: Dp = lerp(0.dp, INNER_PADDING_DP.dp, absolute_expansion)

    val current_horizontal_padding: Dp = lerp(horizontal_padding_minimised, horizontal_padding, absolute_expansion)

    val target_start_padding: Dp = maxOf(inner_padding, content_padding.calculateStartPadding(layout_direction) + horizontal_padding)
    val target_end_padding: Dp = maxOf(inner_padding, content_padding.calculateEndPadding(layout_direction) + horizontal_padding)

    val start_padding: Dp = maxOf(inner_padding, content_padding.calculateStartPadding(layout_direction) + current_horizontal_padding)
    val end_padding: Dp = maxOf(inner_padding, content_padding.calculateEndPadding(layout_direction) + current_horizontal_padding)

    val top_padding: Dp = top_padding + content_padding.calculateTopPadding()
    val bottom_padding: Dp = bottom_padding + content_padding.calculateBottomPadding()

    val bottom_bar_height: Dp = NOW_PLAYING_LARGE_BOTTOM_BAR_HEIGHT
    val inner_bottom_padding: Dp = horizontal_padding

    val bar_background_colour: Color = player.theme.card
    val stroke_colour: Color = bar_background_colour.amplify(255f)

    BoxWithConstraints(
        modifier = modifier
            .height(height)
            .padding(vertical = v_padding * (1f - absolute_expansion).coerceIn(0f..1f))
            .clipToBounds()
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

            var actual_thumb_size: DpSize by remember { mutableStateOf(DpSize.Zero) }
            var actual_thumb_position: DpOffset by remember { mutableStateOf(DpOffset.Zero) }

            val controls_target_height: Dp = 200.dp

            val main_column_target_width: Dp by remember(parent_max_width) { derivedStateOf {
                maxOf(
                    300.dp,
                    minOf(
                        page_height - controls_target_height,
                        minOf(page_height, parent_max_width * 0.5f) - (20.dp)
                    )
                )
            }}

            val thumb_space_v: Dp = page_height - top_padding - controls_target_height - inner_bottom_padding - bottom_padding
            val thumb_space_h: Dp = main_column_target_width - 5.dp
            val thumb_size: Dp = minOf(thumb_space_h, thumb_space_v)

            val compact_mode: Boolean = thumb_size < 250.dp

            // Bottom bar
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
                                player.expansion.toggle()
                            }
                        }
                ) {
                    Canvas(Modifier.fillMaxSize().offset(x = (-0.5).dp, y = 3.dp)) {
                        drawLine(
                            stroke_colour,
                            start = Offset.Zero,
                            end = Offset(size.width, 0f),
                            strokeWidth = (STROKE_WIDTH_DP.dp + 2.dp).toPx()
                        )

                        drawRect(bar_background_colour)
                    }

                    CompositionLocalProvider(LocalContentColor provides bar_background_colour.getContrasted()) {
                        var bottom_bar_position: DpOffset by remember { mutableStateOf(DpOffset.Zero) }

                        val inset_depth: Dp
                        if (swap_controls_and_image) {
                            inset_depth = 0.dp
                        }
                        else {
                            val thumb_pos: Dp = page_height - top_padding - controls_target_height - thumb_size - CONTROLS_IMAGE_SEPARATION_DP.dp
                            inset_depth = if (bottom_bar_height - thumb_pos > 4.dp) 4.dp else 0.dp
                        }

                        LargeBottomBar(
                            bar_background_colour,
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

            // Controls / image
            composeScope {
                Column(
                    Modifier.fillMaxHeight().zIndex(2f),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    BoxWithConstraints(
                        Modifier.fillMaxHeight().weight(1f)
                    ) {
                        Column(
                            Modifier.width(
                                lerp(
                                    parent_max_width,
                                    main_column_target_width,
                                    absolute_expansion
                                )
                            ),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(Modifier.requiredHeight(inner_bottom_padding).weight(1f, false))

                            Spacer(Modifier.fillMaxHeight().weight(1f))

                            @Composable
                            fun Controls(modifier: Modifier = Modifier) {
                                MainTabControls(
                                    { seek_state = it },
                                    enabled = expanded,
                                    button_size = 50.dp,
                                    seek_bar_next_to_buttons = !compact_mode,
                                    modifier = Modifier
                                        .requiredHeight((if (compact_mode) controls_target_height + 200.dp else controls_target_height) * ((absolute_expansion - 0.5f) * 2f))
                                        .scale(1f, absolute_expansion)
                                        .then(modifier)
                                        .graphicsLayer {
                                            alpha = if (compact_mode) (absolute_expansion - 0.5f).coerceAtLeast(0f) * 2f else 1f
                                        },
                                    textRowStartContent = {
                                        if (compact_mode) {
                                            val song: Song? by player.status.song_state

                                            val thumbnail_rounding: Int = song.observeThumbnailRounding()
                                            val thumbnail_shape: RoundedCornerShape = RoundedCornerShape(thumbnail_rounding)

                                            song?.Thumbnail(
                                                ThumbnailProvider.Quality.HIGH,
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
                            }

                            if (!swap_controls_and_image) {
                                Controls(Modifier.padding(bottom = CONTROLS_IMAGE_SEPARATION_DP.dp))
                            }

                            val max_thumbnail_row_height: Dp =
                                if (compact_mode) (1f - absolute_expansion) * thumb_size
                                else thumb_size

                            val thumbnail_row_height: Dp = (min_height + ((max_thumbnail_row_height - min_height) * absolute_expansion)).coerceAtLeast(min_height)

                            if (!compact_mode || absolute_expansion < 1f) {
                                LargeThumbnailRow(
                                    Modifier
                                        .height(thumbnail_row_height)
                                        .graphicsLayer {
                                            alpha = if (compact_mode) ((0.5f - absolute_expansion) * 2f).coerceAtLeast(0f) else 1f
                                        }
                                        .offset {
                                            IntOffset(
                                                (((main_column_target_width - actual_thumb_size.width).toPx() / 2) * absolute_expansion).roundToInt(),
                                                0
                                            )
                                        },
                                    onThumbnailLoaded = { song, image ->
                                        onThumbnailLoaded(song, image)
                                    },
                                    setThemeColour = {
                                        setThemeColour(it, true)
                                    },
                                    getSeekState = { seek_state },
                                    disable_parent_scroll_while_menu_open = false,
                                    thumbnail_modifier = Modifier
                                        .onGloballyPositioned {
                                            with (density) {
                                                val position: Offset = it.positionInRoot()
                                                actual_thumb_position = DpOffset(position.x.toDp(), position.y.toDp())
                                                actual_thumb_size = DpSize(it.size.width.toDp(), it.size.height.toDp())
                                            }
                                        }
                                )
                            }

                            if (swap_controls_and_image) {
                                Controls(Modifier.padding(top = CONTROLS_IMAGE_SEPARATION_DP.dp, bottom = 30.dp))
                            }

                            Spacer(Modifier.requiredHeight(animateDpAsState(inner_bottom_padding * compact_mode.toInt()).value).weight(1f, false))

                            Spacer(Modifier.fillMaxHeight().weight(1f))
                        }
                    }

                    Spacer(Modifier.requiredHeight(lerp(0.dp, inner_bottom_padding, proportion_exp)))
                }
            }

            PlayerQueueTab(
                width_state = remember(parent_max_width) { derivedStateOf {
                    parent_max_width - main_column_target_width - 5.dp - target_start_padding - target_end_padding - INNER_PADDING_DP.dp
                } },
                getHeight = remember(page_height) {{ page_height - top_padding - bottom_padding }},
                getCurrentControlsHeight = remember {{ page_height - top_padding - bottom_padding - inner_bottom_padding }},
                inner_bottom_padding = inner_bottom_padding,
                stroke_colour = stroke_colour,
                page_height = page_height,
                modifier =
                    Modifier
                        .padding(start = INNER_PADDING_DP.dp)
                        .graphicsLayer {
                            alpha = absolute_expansion
                        }
            )
        }
    }
}

@Composable
private fun PlayerQueueTab(
    width_state: State<Dp>,
    getHeight: () -> Dp,
    getCurrentControlsHeight: () -> Dp,
    inner_bottom_padding: Dp,
    stroke_colour: Color,
    page_height: Dp,
    modifier: Modifier = Modifier
) {
    val player: PlayerState = LocalPlayerState.current
    val queue_shape: Shape = RoundedCornerShape(10.dp)
    val width: Dp by width_state

    val default_background_opacity: Float by player.settings.Theme.NOWPLAYING_DEFAULT_LANDSCAPE_QUEUE_OPACITY.observe()
    val song_background_opacity: Float? by player.status.m_song?.LandscapeQueueOpacity?.observe(player.database)

    val background_opacity: Float by remember(player.status.m_song) { derivedStateOf { song_background_opacity ?: default_background_opacity } }
    val show_shadow: Boolean by remember(player.status.m_song) { derivedStateOf { background_opacity >= 1f } }

    Box(
        modifier
            .requiredSize(width, getHeight())
            .offset {
                IntOffset(
                    0,//-((1f - player.expansion.getBounded()) * (page_height - INNER_PADDING_DP.dp) / 2f).roundToPx(),
                    (getCurrentControlsHeight() * (1f - player.expansion.getBounded())).roundToPx()
                )
            }
            .thenIf(show_shadow) {
                songThumbnailShadow(
                    player.status.m_song,
                    queue_shape,
                    apply_expansion_to_colour = false
                ) {
                    alpha = 1f - (1f - player.expansion.getBounded()).absoluteValue
                }
            }
    ) {
        val np_theme_mode: ThemeMode by player.settings.Theme.NOWPLAYING_THEME_MODE.observe()

        QueueTab(
            null,
            Modifier
                .fillMaxSize()
                .thenIf(np_theme_mode != ThemeMode.BACKGROUND) {
                    border(
                        STROKE_WIDTH_DP.dp,
                        stroke_colour,
                        queue_shape
                    )
                },
            inline = true,
            border_thickness = STROKE_WIDTH_DP.dp + 1.dp,
            wave_border_mode_override = NowPlayingQueueWaveBorderMode.SCROLL,
            shape = queue_shape,
            button_row_arrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
            content_padding = PaddingValues(
                bottom = inner_bottom_padding.coerceAtLeast(0.dp) + 35.dp
            ),
            getBackgroundColour = {
                getNPAltBackground()
                // if (player.np_theme_mode == ThemeMode.BACKGROUND) getNPAltOnBackground()
                // else theme.background
            },
            getBackgroundOpacity = {
                background_opacity
            },
            getOnBackgroundColour = {
                when (np_theme_mode) {
                    ThemeMode.BACKGROUND -> theme.vibrantAccent
                    ThemeMode.ELEMENTS -> theme.accent
                    ThemeMode.NONE -> theme.onBackground
                }
            }
        )
    }
}
