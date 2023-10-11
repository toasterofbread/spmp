package com.toasterofbread.spmp.ui.layout.nowplaying

import LocalPlayerState
import SpMp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.Interaction
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
import com.toasterofbread.spmp.model.OverscrollClearMode
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.platform.BackHandler
import com.toasterofbread.spmp.platform.composable.scrollWheelSwipeable
import com.toasterofbread.spmp.platform.composeScope
import com.toasterofbread.spmp.platform.vibrateShort
import com.toasterofbread.spmp.platform.playerservice.PlatformPlayerService
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.PlayerState
import com.toasterofbread.spmp.ui.layout.nowplaying.maintab.NowPlayingMainTab
import com.toasterofbread.spmp.ui.layout.nowplaying.queue.QueueTab
import com.toasterofbread.spmp.ui.theme.Theme
import com.toasterofbread.utils.*
import com.toasterofbread.utils.common.amplifyPercent
import com.toasterofbread.utils.common.setAlpha
import com.toasterofbread.utils.composable.OnChangedEffect
import com.toasterofbread.utils.composable.RecomposeOnInterval
import com.toasterofbread.utils.modifier.brushBackground
import kotlinx.coroutines.delay

enum class ThemeMode { BACKGROUND, ELEMENTS, NONE }

private enum class NowPlayingVerticalPage { MAIN, QUEUE }
val NOW_PLAYING_VERTICAL_PAGE_COUNT = NowPlayingVerticalPage.values().size

const val EXPANDED_THRESHOLD = 0.1f
const val POSITION_UPDATE_INTERVAL_MS: Long = 100
private const val GRADIENT_BOTTOM_PADDING_DP = 100
private const val GRADIENT_TOP_START_RATIO = 0.7f
private const val OVERSCROLL_CLEAR_DISTANCE_THRESHOLD_DP = 5f

internal fun PlayerState.getNPBackground(): Color {
    return when (np_theme_mode) {
        ThemeMode.BACKGROUND -> theme.accent
        ThemeMode.ELEMENTS -> theme.background
        ThemeMode.NONE -> theme.background
    }
}

internal fun PlayerState.getNPOnBackground(): Color {
    return when (np_theme_mode) {
        ThemeMode.BACKGROUND -> theme.on_accent
        ThemeMode.ELEMENTS -> theme.accent
        ThemeMode.NONE -> theme.on_background
    }
}

internal fun PlayerState.getNPAltBackground(): Color {
    return when (np_theme_mode) {
        ThemeMode.BACKGROUND -> getNPBackground().amplifyPercent(-0.4f, opposite_percent = -0.2f)
        else -> theme.background
    }
}

internal fun PlayerState.getNPAltOnBackground(): Color =
    getNPBackground().amplifyPercent(-0.4f, opposite_percent = -0.1f)

val LocalNowPlayingExpansion: ProvidableCompositionLocal<NowPlayingExpansionState> = staticCompositionLocalOf { SpMp.player_state.expansion_state }

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun NowPlaying(swipe_state: SwipeableState<Int>, swipe_anchors: Map<Float, Int>) {
    LocalNowPlayingExpansion.current.init()

    val player = LocalPlayerState.current
    val expansion = LocalNowPlayingExpansion.current
    val density = LocalDensity.current

    val swipe_interaction_source = remember { MutableInteractionSource() }
    val swipe_interactions: MutableList<Interaction> = remember { mutableStateListOf() }
    var player_alpha: Float by remember { mutableStateOf(1f) }

    LaunchedEffect(swipe_interaction_source) {
        swipe_interaction_source.interactions.collect { interaction ->
            when (interaction) {
                is DragInteraction.Start -> {
                    swipe_interactions.add(interaction)
                }
                is DragInteraction.Stop -> {
                    swipe_interactions.remove(interaction.start)
                }
                is DragInteraction.Cancel -> {
                    swipe_interactions.remove(interaction.start)
                }
            }
        }
    }

    CompositionLocalProvider(LocalNowPlayingExpansion provides SpMp.player_state.expansion_state) {
        expansion.init()

        AnimatedVisibility(
            player.session_started,
            exit = slideOutVertically(),
            enter = slideInVertically()
        ) {
            val bottom_padding = player.nowPlayingBottomPadding()
            val default_gradient_depth: Float by Settings.KEY_NOWPLAYING_DEFAULT_GRADIENT_DEPTH.rememberMutableState()

            val half_screen_height = player.screen_size.height * 0.5f
            val page_height: Dp = (
                player.screen_size.height
                - bottom_padding
                - player.context.getStatusBarHeightDp()
            )

            val is_shut by remember { derivedStateOf { swipe_state.targetValue == 0 } }

            var switch_to_page: Int by remember { mutableStateOf(-1) }
            OnChangedEffect(switch_to_page) {
                if (switch_to_page >= 0) {
                    swipe_state.animateTo(switch_to_page)
                    switch_to_page = -1
                }
            }

            val overscroll_clear_enabled: Boolean by Settings.KEY_MINI_PLAYER_OVERSCROLL_CLEAR_ENABLED.rememberMutableState()
            val overscroll_clear_time: Float by Settings.KEY_MINI_PLAYER_OVERSCROLL_CLEAR_TIME.rememberMutableState()
            val overscroll_clear_mode: OverscrollClearMode by Settings.KEY_MINI_PLAYER_OVERSCROLL_CLEAR_MODE.rememberMutableEnumState()

            LaunchedEffect(player.controller, swipe_interactions.isNotEmpty(), overscroll_clear_enabled) {
                if (!overscroll_clear_enabled) {
                    return@LaunchedEffect
                }

                val service: PlatformPlayerService = player.controller ?: return@LaunchedEffect
                val anchor: Float = swipe_anchors.keys.first()
                val delta: Long = 50
                val time_threshold: Float = overscroll_clear_time * 1000

                var time_below_threshold: Long = 0
                var triggered: Boolean = false

                player_alpha = 1f

                while (swipe_interactions.isNotEmpty()) {
                    delay(delta)

                    if (service.song_count == 0 && overscroll_clear_mode == OverscrollClearMode.NONE_IF_QUEUE_EMPTY) {
                        continue
                    }

                    if (time_threshold == 0f) {
                        player_alpha = 1f
                    }
                    else {
                        player_alpha = 1f - (time_below_threshold / time_threshold).coerceIn(0f, 1f)
                    }

                    val offset: Dp = with(density) { (swipe_state.offset.value - anchor).toDp() }
                    if (offset < -OVERSCROLL_CLEAR_DISTANCE_THRESHOLD_DP.dp) {
                        if (!triggered && time_below_threshold >= time_threshold) {
                            if (
                                overscroll_clear_mode == OverscrollClearMode.ALWAYS_HIDE
                                || (overscroll_clear_mode == OverscrollClearMode.HIDE_IF_QUEUE_EMPTY && service.song_count == 0)
                            ) {
                                service.service_player.cancelSession()
                            }

                            if (service.song_count > 0) {
                                service.service_player.clearQueue()
                            }

                            player.context.vibrateShort()

                            triggered = true
                        }

                        time_below_threshold += delta
                    }
                    else {
                        time_below_threshold = 0
                        triggered = false
                    }
                }
            }

            val song_gradient_depth: Float? =
                player.status.m_song?.PlayerGradientDepth?.observe(player.database)?.value

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .requiredHeight((player.screen_size.height * (NOW_PLAYING_VERTICAL_PAGE_COUNT + 1)))
                    .offset {
                        IntOffset(
                            0,
                            with(density) {
                                ((half_screen_height * NOW_PLAYING_VERTICAL_PAGE_COUNT) - swipe_state.offset.value.dp - bottom_padding).roundToPx()
                            }
                        )
                    }
                    .scrollWheelSwipeable(
                        state = swipe_state,
                        anchors = swipe_anchors,
                        thresholds = { _, _ -> FractionalThreshold(0.2f) },
                        orientation = Orientation.Vertical,
                        reverse_direction = true,
                        interaction_source = swipe_interaction_source
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (is_shut) {
                            switch_to_page = if (swipe_state.targetValue == 0) 1 else 0
                        }
                    }
                    .graphicsLayer {
                        alpha = player_alpha
                    }
                    .brushBackground {
                        with(density) {
                            val screen_height_px = page_height.toPx()
                            val v_offset = (expansion.get() - 1f).coerceAtLeast(0f) * screen_height_px

                            val gradient_depth = 1f - (song_gradient_depth ?: default_gradient_depth)
                            check(gradient_depth in 0f .. 1f)

                            Brush.verticalGradient(
                                listOf(player.getNPBackground(), player.getNPAltBackground()),
                                startY = v_offset + (page_height.toPx() * GRADIENT_TOP_START_RATIO),
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

                CompositionLocalProvider(LocalContentColor provides player.getNPOnBackground()) {
                    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                        NowPlayingCardContent(page_height)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBarColourHandler(page_height: Dp) {
    val player = LocalPlayerState.current
    val expansion = LocalNowPlayingExpansion.current

    val background_colour = player.getNPBackground()
    val status_bar_height = player.context.getStatusBarHeightDp()

    val status_bar_height_percent = (
        status_bar_height.value * (if (player.context.isDisplayingAboveNavigationBar()) 1f else 0.75f)
    ) / page_height.value
    val under_status_bar by remember { derivedStateOf { 1f - expansion.get() < status_bar_height_percent } }

    DisposableEffect(under_status_bar, background_colour) {
        val colour = if (under_status_bar) background_colour else player.theme.background
        player.context.setStatusBarColour(colour)

        onDispose {
            player.context.setStatusBarColour(player.theme.background)
        }
    }

    DisposableEffect(background_colour) {
        player.onNavigationBarTargetColourChanged(background_colour, false)

        onDispose {
            player.onNavigationBarTargetColourChanged(null, false)
        }
    }
}

@Composable
private fun NowPlayingCardContent(page_height: Dp, modifier: Modifier = Modifier) {
    val player = LocalPlayerState.current
    val expansion = LocalNowPlayingExpansion.current

    StatusBarColourHandler(page_height)
    MinimisedProgressBar()

    var top_bar_height by remember { mutableStateOf(0.dp) }

    Column(modifier.fillMaxHeight(), verticalArrangement = Arrangement.Top) {
        Column(
            verticalArrangement = Arrangement.Top,
            modifier = Modifier.requiredSize(player.screen_size)
        ) {
            composeScope {
                Spacer(
                    Modifier.height(
                        player.context.getStatusBarHeightDp() * expansion.get().coerceIn(0f, 1f)
                    )
                )
            }

            val screen_height = player.screen_size.height - player.nowPlayingBottomPadding()
            val offsetProvider: Density.() -> IntOffset = remember(screen_height) {
                {
                    val bounded = expansion.getBounded()
                    IntOffset(
                        0,
                        if (bounded > 1f)
                            (
                                -(screen_height)
                                * ((NOW_PLAYING_VERTICAL_PAGE_COUNT * 0.5f) - bounded)
                            ).roundToPx()
                        else 0
                    )
                }
            }

            CompositionLocalProvider(LocalPlayerState provides remember {
                player.copy(
                    onClickedOverride = { item, _ ->
                        player.onMediaItemClicked(item)
                        expansion.close()
                    }
                )
            }) {
                NowPlayingMainTab(
                    onTopBarHeightChanged = { top_bar_height = it },
                    modifier = Modifier.fillMaxWidth().requiredHeight(page_height).offset(offsetProvider)
                )
            }

            composeScope {
                Spacer(
                    Modifier.height(
                        player.nowPlayingBottomPadding()
                    )
                )
            }
        }

        QueueTab(
            page_height,
            { top_bar_height },
            Modifier.offset(0.dp, -player.nowPlayingBottomPadding())
        )
    }
}

@Composable
fun MinimisedProgressBar(modifier: Modifier = Modifier) {
    val expansion = LocalNowPlayingExpansion.current
    val player = LocalPlayerState.current

    RecomposeOnInterval(POSITION_UPDATE_INTERVAL_MS) { state ->
        state

        LinearProgressIndicator(
            progress = player.status.getProgress(),
            color = player.getNPOnBackground(),
            trackColor = player.getNPOnBackground().setAlpha(0.5f),
            modifier = modifier
                .requiredHeight(2.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = 1f - expansion.get()
                }
        )
    }
}
