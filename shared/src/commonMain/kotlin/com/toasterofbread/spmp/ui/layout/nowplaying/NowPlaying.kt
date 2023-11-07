package com.toasterofbread.spmp.ui.layout.nowplaying

import LocalNowPlayingExpansion
import LocalPlayerState
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
import androidx.compose.material.swipeable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import com.toasterofbread.spmp.model.OverscrollClearMode
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.toastercomposetools.platform.composable.BackHandler
import com.toasterofbread.toastercomposetools.platform.composable.scrollWheelSwipeable
import com.toasterofbread.toastercomposetools.platform.composable.composeScope
import com.toasterofbread.spmp.platform.isPortrait
import com.toasterofbread.toastercomposetools.platform.vibrateShort
import com.toasterofbread.spmp.platform.playerservice.PlatformPlayerService
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.MINIMISED_NOW_PLAYING_V_PADDING_DP
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.PlayerState
import com.toasterofbread.toastercomposetools.utils.*
import com.toasterofbread.toastercomposetools.utils.common.amplifyPercent
import com.toasterofbread.toastercomposetools.utils.composable.OnChangedEffect
import com.toasterofbread.toastercomposetools.utils.composable.RecomposeOnInterval
import com.toasterofbread.toastercomposetools.utils.composable.getTop
import com.toasterofbread.toastercomposetools.utils.modifier.brushBackground
import kotlinx.coroutines.delay

enum class ThemeMode { BACKGROUND, ELEMENTS, NONE }

fun getNowPlayingVerticalPageCount(player: PlayerState): Int =
    NowPlayingPage.ALL.count { it.shouldShow(player) }

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

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun NowPlaying(swipe_state: SwipeableState<Int>, swipe_anchors: Map<Float, Int>, content_padding: PaddingValues) {
    LocalNowPlayingExpansion.current.init()

    val player: PlayerState = LocalPlayerState.current
    val expansion: NowPlayingExpansionState = LocalNowPlayingExpansion.current
    val density: Density = LocalDensity.current

    val swipe_interaction_source: MutableInteractionSource = remember { MutableInteractionSource() }
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

    expansion.init()

    AnimatedVisibility(
        player.session_started,
        exit = slideOutVertically(),
        enter = slideInVertically()
    ) {
        val bottom_padding: Dp = player.nowPlayingBottomPadding()
        val default_gradient_depth: Float by Settings.KEY_NOWPLAYING_DEFAULT_GRADIENT_DEPTH.rememberMutableState()

        val half_screen_height: Dp = player.screen_size.height * 0.5f
        val page_height: Dp = (
            player.screen_size.height
            - bottom_padding
            - WindowInsets.getTop()
        )

        val is_shut: Boolean by remember { derivedStateOf { swipe_state.targetValue == 0 } }

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

        val swipe_modifier: Modifier = remember(swipe_anchors) {
            Modifier.swipeable(
                state = swipe_state,
                anchors = swipe_anchors,
                thresholds = { _, _ -> FractionalThreshold(0.2f) },
                orientation = Orientation.Vertical,
                reverseDirection = true,
                interactionSource = swipe_interaction_source
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .requiredHeight((player.screen_size.height * (getNowPlayingVerticalPageCount(player) + 1)))
                .offset {
                    IntOffset(
                        0,
                        with(density) {
                            ((half_screen_height * getNowPlayingVerticalPageCount(player)) - swipe_state.offset.value.dp - bottom_padding).roundToPx()
                        }
                    )
                }
                .then(swipe_modifier)
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
                    NowPlayingCardContent(page_height, content_padding, swipe_modifier)
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
    val status_bar_height = WindowInsets.statusBars.getTop()

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
private fun NowPlayingCardContent(page_height: Dp, content_padding: PaddingValues, swipe_modifier: Modifier, modifier: Modifier = Modifier) {
    val player = LocalPlayerState.current
    val expansion = LocalNowPlayingExpansion.current

    StatusBarColourHandler(page_height)
    MinimisedProgressBar()

    val pages: List<NowPlayingPage> = NowPlayingPage.ALL.filter { it.shouldShow(player) }
    val top_bar = remember { NowPlayingTopBar() }

    Column(modifier.fillMaxHeight(), verticalArrangement = Arrangement.Top) {
        Column(
            verticalArrangement = Arrangement.Top,
            modifier = Modifier.requiredSize(player.screen_size)
        ) {
            composeScope {
                val spacer_height: Dp
                val top_inset: Dp = WindowInsets.statusBars.getTop()

                if (player.isPortrait() || top_inset <= 2.dp) {
                    spacer_height = lerp(MINIMISED_NOW_PLAYING_V_PADDING_DP.dp, WindowInsets.statusBars.getTop(), expansion.get().coerceIn(0f, 1f))
                }
                else {
                    val max_height: Dp = top_inset - 2.dp
                    val proportion: Float = (max_height + 5.dp) / player.screen_size.height

                    val exp: Float = expansion.get().coerceIn(0f, 1f)
                    val proportion_exp: Float =
                        if (exp > proportion) ((exp - 1 + proportion) / proportion)
                        else 0f

                    spacer_height = (max_height * proportion_exp).coerceAtLeast(MINIMISED_NOW_PLAYING_V_PADDING_DP.dp)
                }

                Spacer(Modifier.height(spacer_height))
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
                                * ((getNowPlayingVerticalPageCount(player) * 0.5f) - bounded)
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
                pages.firstOrNull()?.Page(
                    page_height,
                    top_bar,
                    content_padding,
                    swipe_modifier,
                    Modifier.fillMaxWidth().offset(offsetProvider)
                )
            }

            composeScope {
                Spacer(Modifier.height(player.nowPlayingBottomPadding()))
            }
        }

        for (i in 1 until pages.size) {
            pages[i].Page(page_height, top_bar, content_padding, swipe_modifier, Modifier.offset(0.dp, -player.nowPlayingBottomPadding()))
        }
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
            trackColor = player.getNPOnBackground().copy(alpha = 0.5f),
            modifier = modifier
                .requiredHeight(2.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = 1f - expansion.get()
                }
        )
    }
}
