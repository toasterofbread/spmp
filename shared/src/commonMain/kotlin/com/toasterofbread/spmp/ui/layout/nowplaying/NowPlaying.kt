package com.toasterofbread.spmp.ui.layout.nowplaying

import LocalPlayerState
import SpMp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.SwipeableState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.model.mediaitem.observeAsState
import com.toasterofbread.spmp.platform.BackHandler
import com.toasterofbread.spmp.platform.composable.scrollWheelSwipeable
import com.toasterofbread.spmp.platform.composeScope
import com.toasterofbread.spmp.platform.getNavigationBarHeightDp
import com.toasterofbread.spmp.ui.layout.nowplaying.maintab.NowPlayingMainTab
import com.toasterofbread.spmp.ui.layout.nowplaying.queue.QueueTab
import com.toasterofbread.spmp.ui.theme.Theme
import com.toasterofbread.utils.*
import com.toasterofbread.utils.composable.OnChangedEffect
import com.toasterofbread.utils.composable.RecomposeOnInterval
import com.toasterofbread.utils.modifier.brushBackground

enum class ThemeMode { BACKGROUND, ELEMENTS, NONE }

private enum class NowPlayingVerticalPage { MAIN, QUEUE }
val NOW_PLAYING_VERTICAL_PAGE_COUNT = NowPlayingVerticalPage.values().size

const val EXPANDED_THRESHOLD = 0.1f
const val POSITION_UPDATE_INTERVAL_MS: Long = 100
private const val GRADIENT_BOTTOM_PADDING_DP = 100
private const val GRADIENT_TOP_START_RATIO = 0.7f

internal fun getNPBackground(): Color {
    return when (SpMp.context.player_state.np_theme_mode) {
        ThemeMode.BACKGROUND -> Theme.accent
        ThemeMode.ELEMENTS -> Theme.background
        ThemeMode.NONE -> Theme.background
    }
}

internal fun getNPOnBackground(): Color {
    return when (SpMp.context.player_state.np_theme_mode) {
        ThemeMode.BACKGROUND -> Theme.on_accent
        ThemeMode.ELEMENTS -> Theme.accent
        ThemeMode.NONE -> Theme.on_background
    }
}

internal fun getNPAltBackground(): Color {
    return when (SpMp.context.player_state.np_theme_mode) {
        ThemeMode.BACKGROUND -> getNPBackground().amplifyPercent(-0.4f, opposite_percent = -0.2f)
        else -> Theme.background
    }
}

internal fun getNPAltOnBackground(): Color =
    getNPBackground().amplifyPercent(-0.4f, opposite_percent = -0.1f)

val LocalNowPlayingExpansion: ProvidableCompositionLocal<NowPlayingExpansionState> = staticCompositionLocalOf { SpMp.context.player_state.expansion_state }

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun NowPlaying(swipe_state: SwipeableState<Int>, swipe_anchors: Map<Float, Int>) {
    LocalNowPlayingExpansion.current.init()

    CompositionLocalProvider(LocalNowPlayingExpansion provides SpMp.context.player_state.expansion_state) {
        LocalNowPlayingExpansion.current.init()
        AnimatedVisibility(
            LocalPlayerState.current.session_started,
            exit = slideOutVertically(),
            enter = slideInVertically(),
        ) {
            val density = LocalDensity.current
            val player = LocalPlayerState.current
            val expansion = LocalNowPlayingExpansion.current

            val keyboard_insets = SpMp.context.getImeInsets()
            val bottom_padding = player.nowPlayingBottomPadding()
            val screen_height = SpMp.context.getScreenHeight()
            val default_gradient_depth: Float by Settings.KEY_NOWPLAYING_DEFAULT_GRADIENT_DEPTH.rememberMutableState()

            val half_screen_height = screen_height.value * 0.5f
            val is_shut by remember { derivedStateOf { swipe_state.targetValue == 0 } }

            var switch_to_page: Int by remember { mutableStateOf(-1) }
            OnChangedEffect(switch_to_page) {
                if (switch_to_page >= 0) {
                    swipe_state.animateTo(switch_to_page)
                    switch_to_page = -1
                }
            }

            val song_gradient_depth: Float? =
                player.status.m_song?.PlayerGradientDepth?.observe(SpMp.context.database)?.value

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .requiredHeight(screen_height * (NOW_PLAYING_VERTICAL_PAGE_COUNT + 1))
                    .offset {
                        IntOffset(
                            0,
                            with(density) {
                                val keyboard_bottom_padding = if (keyboard_insets == null || swipe_state.targetValue != 0) 0 else keyboard_insets.getBottom(density)
                                ((half_screen_height.dp * NOW_PLAYING_VERTICAL_PAGE_COUNT) - swipe_state.offset.value.dp).toPx().toInt() - keyboard_bottom_padding - bottom_padding.toPx().toInt()
                            }
                        )
                    }
                    .scrollWheelSwipeable(
                        state = swipe_state,
                        anchors = swipe_anchors,
                        thresholds = { _, _ -> FractionalThreshold(0.2f) },
                        orientation = Orientation.Vertical,
                        reverseDirection = true,
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (is_shut) {
                            switch_to_page = if (swipe_state.targetValue == 0) 1 else 0
                        }
                    }
                    .brushBackground {
                        with(density) {
                            val screen_height_px = screen_height.toPx()
                            val v_offset = (expansion.get() - 1f).coerceAtLeast(0f) * screen_height_px

                            val gradient_depth = 1f - (song_gradient_depth ?: default_gradient_depth)
                            check(gradient_depth in 0f .. 1f)

                            Brush.verticalGradient(
                                listOf(getNPBackground(), getNPAltBackground()),
                                startY = v_offset + (screen_height.toPx() * GRADIENT_TOP_START_RATIO),
                                endY = v_offset - GRADIENT_BOTTOM_PADDING_DP.dp.toPx() + (
                                    screen_height_px * (1.2f + (gradient_depth * 2f))
                                )
                            )
                        }
                    }
            ) {
                BackHandler({ !is_shut }) {
                    switch_to_page = swipe_state.targetValue - 1
                }

                CompositionLocalProvider(LocalContentColor provides getNPOnBackground()) {
                    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                        NowPlayingCardContent(screen_height)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBarColourHandler(page_height: Dp) {
    val expansion = LocalNowPlayingExpansion.current
    val status_bar_height = SpMp.context.getStatusBarHeight()
    val background_colour = getNPBackground()

    val status_bar_height_percent = (
        status_bar_height.value * (if (SpMp.context.isDisplayingAboveNavigationBar()) 1f else 0.75f)
    ) / page_height.value
    val under_status_bar by remember { derivedStateOf { 1f - expansion.get() < status_bar_height_percent } }

    LaunchedEffect(key1 = under_status_bar, key2 = background_colour) {
        val colour = if (under_status_bar) background_colour else Theme.background
        SpMp.context.setStatusBarColour(colour, !colour.isDark())
    }
}

@Composable
private fun NowPlayingCardContent(page_height: Dp) {
    val player = LocalPlayerState.current
    val expansion = LocalNowPlayingExpansion.current

    StatusBarColourHandler(page_height)
    MinimisedProgressBar()

    val screen_width_dp = SpMp.context.getScreenWidth()

    Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.Top) {
        Column(
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .requiredHeight(page_height)
                .requiredWidth(screen_width_dp)
        ) {
            composeScope {
                Spacer(Modifier.height(
                    SpMp.context.getStatusBarHeight() * expansion.get().coerceIn(0f, 1f))
                )
            }

            CompositionLocalProvider(LocalPlayerState provides remember {
                player.copy(
                    onClickedOverride = { item, _ ->
                        player.onMediaItemClicked(item)
                        expansion.close()
                    }
                )
            }) {
                NowPlayingMainTab()
            }
        }

        QueueTab(page_height)
    }
}

@Composable
fun MinimisedProgressBar(modifier: Modifier = Modifier) {
    val expansion = LocalNowPlayingExpansion.current
    RecomposeOnInterval(POSITION_UPDATE_INTERVAL_MS) { state ->
        state

        LinearProgressIndicator(
            progress = LocalPlayerState.current.status.getProgress(),
            color = getNPOnBackground(),
            trackColor = getNPOnBackground().setAlpha(0.5f),
            modifier = modifier
                .requiredHeight(2.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = 1f - expansion.get()
                }
        )
    }
}
