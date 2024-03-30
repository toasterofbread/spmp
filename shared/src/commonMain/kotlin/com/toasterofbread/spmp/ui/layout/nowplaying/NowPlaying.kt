package com.toasterofbread.spmp.ui.layout.nowplaying

import LocalNowPlayingExpansion
import LocalPlayerState
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.FractionalThreshold
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import com.toasterofbread.composekit.platform.*
import com.toasterofbread.composekit.platform.composable.*
import com.toasterofbread.composekit.utils.common.*
import com.toasterofbread.composekit.utils.composable.*
import com.toasterofbread.composekit.utils.modifier.brushBackground
import com.toasterofbread.spmp.model.settings.category.*
import com.toasterofbread.spmp.model.settings.rememberMutableEnumState
import com.toasterofbread.spmp.platform.*
import com.toasterofbread.spmp.platform.playerservice.PlatformPlayerService
import com.toasterofbread.spmp.service.playercontroller.*
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.MINIMISED_NOW_PLAYING_V_PADDING_DP
import com.toasterofbread.spmp.ui.layout.StatusBarColourState
import com.toasterofbread.spmp.ui.layout.contentbar.layoutslot.CustomColourSource
import kotlinx.coroutines.*

enum class ThemeMode {
    BACKGROUND, ELEMENTS, NONE;

    companion object {
        val DEFAULT: ThemeMode =
            when (Platform.current) {
                Platform.ANDROID -> BACKGROUND
                Platform.DESKTOP -> BACKGROUND
            }
    }
}

fun getNowPlayingVerticalPageCount(player: PlayerState): Int =
    NowPlayingPage.ALL.count { it.shouldShow(player) }

const val EXPANDED_THRESHOLD = 0.1f
const val POSITION_UPDATE_INTERVAL_MS: Long = 100
private const val GRADIENT_BOTTOM_PADDING_DP = 100
private const val GRADIENT_TOP_START_RATIO = 0.7f
private const val OVERSCROLL_CLEAR_DISTANCE_THRESHOLD_DP = 5f

private fun PlayerState.getBackgroundColourOverride(): Color {
    val pages: List<NowPlayingPage> = NowPlayingPage.ALL.filter { it.shouldShow(this) }

    var current: Color? = pages.getOrNull(expansion.swipe_state.currentValue - 1)?.getPlayerBackgroundColourOverride(this)
    var target: Color? = pages.getOrNull(expansion.swipe_state.targetValue - 1)?.getPlayerBackgroundColourOverride(this)

    val default: Color = when (np_theme_mode) {
        ThemeMode.BACKGROUND -> theme.accent
        ThemeMode.ELEMENTS -> theme.card
        ThemeMode.NONE -> theme.card
    }

    if (current == null && target == null) {
        return default
    }

    if (current == null) {
        current = default
    }
    else if (target == null) {
        target = default
    }

    return target!!.blendWith(current, if (expansion.swipe_state.lastVelocity < 0 ) 1f - expansion.swipe_state.progress else expansion.swipe_state.progress)
}

private var derived_np_background: State<Color>? = null
private var derived_np_background_player: PlayerState? = null

fun PlayerState.getNPBackground(): Color {
    if (derived_np_background == null || derived_np_background_player != this) {
        derived_np_background = derivedStateOf { getBackgroundColourOverride() }
        derived_np_background_player = this
    }
    return derived_np_background!!.value
}

internal fun PlayerState.getNPOnBackground(): Color {
    return getBackgroundColourOverride().getContrasted()
//    val override: Color? = getBackgroundColourOverride()?.getPlayerBackgroundColourOverride(this)
//    if (override != null) {
//        return override.getContrasted()
//    }
//
//    return when (np_theme_mode) {
//        ThemeMode.BACKGROUND -> theme.on_accent
//        ThemeMode.ELEMENTS -> theme.accent
//        ThemeMode.NONE -> theme.on_background
//    }
}

internal fun PlayerState.getNPAltBackground(theme_mode: ThemeMode = np_theme_mode): Color {
    return when (theme_mode) {
        ThemeMode.BACKGROUND -> getNPBackground().amplifyPercent(-0.4f, opposite_percent = -0.2f)
        else -> theme.background
    }
}

internal fun PlayerState.getNPAltOnBackground(): Color =
    getNPBackground().amplifyPercent(-0.4f, opposite_percent = -0.1f)

@Composable
fun NowPlaying(
    page_height: Dp,
    swipe_state: AnchoredDraggableState<Int>,
    content_padding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val player: PlayerState = LocalPlayerState.current
    val expansion: NowPlayingExpansionState = LocalNowPlayingExpansion.current
    val density: Density = LocalDensity.current
    val coroutine_scope: CoroutineScope = rememberCoroutineScope()

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

    AnimatedVisibility(
        player.player_showing,
        modifier,
        exit = slideOutVertically() { it },
        enter = slideInVertically() { it }
    ) {
        val bottom_padding: Dp = player.nowPlayingBottomPadding()
        val default_gradient_depth: Float by ThemeSettings.Key.NOWPLAYING_DEFAULT_GRADIENT_DEPTH.rememberMutableState()

        val half_page_height: Dp = page_height * 0.5f

        val is_shut: Boolean by remember { derivedStateOf { swipe_state.targetValue == 0 } }

        val overscroll_clear_enabled: Boolean by PlayerSettings.Key.MINI_OVERSCROLL_CLEAR_ENABLED.rememberMutableState()
        val overscroll_clear_time: Float by PlayerSettings.Key.MINI_OVERSCROLL_CLEAR_TIME.rememberMutableState()
        val overscroll_clear_mode: OverscrollClearMode by PlayerSettings.Key.MINI_OVERSCROLL_CLEAR_MODE.rememberMutableEnumState()

        LaunchedEffect(player.controller, swipe_interactions.isNotEmpty(), overscroll_clear_enabled) {
            if (!overscroll_clear_enabled) {
                return@LaunchedEffect
            }

            val service: PlatformPlayerService = player.controller ?: return@LaunchedEffect
            val anchor: Float = swipe_state.anchors.positionOf(0)
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

                val offset: Dp = with(density) { (swipe_state.offset - anchor).toDp() }
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
        val large_form_factor: Boolean = player.form_factor.is_large

        val swipe_modifier: Modifier =
            Modifier.anchoredDraggable(
                state = swipe_state,
                orientation = Orientation.Vertical,
                reverseDirection = true,
                interactionSource = swipe_interaction_source,
                enabled = !large_form_factor
            )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .requiredHeight((page_height * (getNowPlayingVerticalPageCount(player) + 1)))
                .offset {
                    IntOffset(
                        0,
                        with(density) {
                            ((half_page_height * getNowPlayingVerticalPageCount(player)) - swipe_state.offset.dp - bottom_padding).roundToPx()
                        }
                    )
                }
                .then(swipe_modifier)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (is_shut) {
                        coroutine_scope.launch {
                            expansion.scrollTo(if (swipe_state.targetValue == 0) 1 else 0)
                        }
                    }
                }
                .graphicsLayer {
                    alpha = player_alpha
                }
                .brushBackground {
                    with(density) {
                        val screen_height_px: Float = page_height.toPx()
                        val v_offset: Float = (expansion.get() - 1f).coerceAtLeast(0f) * screen_height_px

                        val gradient_depth: Float = 1f - (song_gradient_depth ?: default_gradient_depth)
                        check(gradient_depth in 0f .. 1f)

                        return@brushBackground Brush.verticalGradient(
                            listOf(player.getNPBackground(), player.getNPAltBackground()),
                            startY = v_offset + (page_height.toPx() * GRADIENT_TOP_START_RATIO),
                            endY = v_offset - GRADIENT_BOTTOM_PADDING_DP.dp.toPx() + (
                                screen_height_px * (1.2f + (gradient_depth * 2f))
                            )
                        )
                    }
                }
        ) {
            if (ThemeSettings.Key.SHOW_EXPANDED_PLAYER_WAVE.rememberMutableState<Boolean>().value) {
                NowPlayingOverlappingWaveBackground(
                    page_height + content_padding.calculateBottomPadding(),
                    Modifier
                        .align(Alignment.TopCenter)
                        .zIndex(1f)
                )
            }

            if (NowPlayingPage.getFormFactor(player) == FormFactor.LANDSCAPE) {
                NowPlayingThumbnailBackground(Modifier.requiredSize(maxOf(page_height, player.screen_size.width)))
            }

            BackHandler({ !is_shut }, priority = 1) {
                coroutine_scope.launch {
                    expansion.scroll(-1)
                }
            }

            CompositionLocalProvider(LocalContentColor provides player.getNPOnBackground()) {
                Column(Modifier.fillMaxSize().zIndex(2f), horizontalAlignment = Alignment.CenterHorizontally) {
                    NowPlayingCardContent(page_height, content_padding, swipe_modifier)
                }
            }
        }
    }
}

@Composable
private fun updateBarColours(page_height: Dp) {
    val player: PlayerState = LocalPlayerState.current
    val expansion: NowPlayingExpansionState = LocalNowPlayingExpansion.current
    val background_colour = player.getNPBackground()
    val status_bar_height = WindowInsets.statusBars.getTop()

    val status_bar_height_percent = (
        status_bar_height.value * (if (player.context.isDisplayingAboveNavigationBar()) 1f else 0.75f)
    ) / page_height.value
    val under_status_bar by remember { derivedStateOf { 1f - expansion.get() < status_bar_height_percent } }

    DisposableEffect(under_status_bar, background_colour) {
        player.status_bar_colour_state.setLevelColour(
            if (under_status_bar) CustomColourSource(background_colour) else null,
            StatusBarColourState.Level.PLAYER
        )

        onDispose {
            player.status_bar_colour_state.setLevelColour(null, StatusBarColourState.Level.PLAYER)
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
    val player: PlayerState = LocalPlayerState.current
    val click_overrides: PlayerClickOverrides = LocalPlayerClickOverrides.current
    val expansion: NowPlayingExpansionState = LocalNowPlayingExpansion.current

    updateBarColours(page_height)
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

                if (player.form_factor == FormFactor.PORTRAIT || top_inset <= 2.dp) {
                    spacer_height = lerp(MINIMISED_NOW_PLAYING_V_PADDING_DP.dp, WindowInsets.statusBars.getTop(), expansion.get().coerceIn(0f, 1f))
                }
                else {
                    val max_height: Dp = top_inset - 2.dp
                    val proportion: Float = (max_height + 5.dp) / page_height

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

            CompositionLocalProvider(LocalPlayerClickOverrides provides click_overrides.copy(
                onClickOverride = { item, _ ->
                    click_overrides.onMediaItemClicked(item, player)
                    expansion.toggle()
                }
            )) {
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
            pages[i].Page(page_height, top_bar, content_padding, swipe_modifier, Modifier.fillMaxWidth().offset(0.dp, -player.nowPlayingBottomPadding()))
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
