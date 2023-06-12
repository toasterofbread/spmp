package com.spectre7.spmp.ui.layout.nowplaying

import GlobalPlayerState
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
import com.spectre7.spmp.platform.composable.BackHandler
import com.spectre7.spmp.platform.composable.scrollWheelSwipeable
import com.spectre7.spmp.ui.layout.mainpage.MINIMISED_NOW_PLAYING_HEIGHT
import com.spectre7.spmp.ui.theme.Theme
import com.spectre7.utils.*
import com.spectre7.utils.composable.OnChangedEffect
import com.spectre7.utils.composable.RecomposeOnInterval

enum class ThemeMode { BACKGROUND, ELEMENTS, NONE }

private enum class NowPlayingVerticalPage { MAIN, QUEUE }
val NOW_PLAYING_VERTICAL_PAGE_COUNT = NowPlayingVerticalPage.values().size

const val SEEK_CANCEL_THRESHOLD = 0.03f
const val EXPANDED_THRESHOLD = 0.9f
const val POSITION_UPDATE_INTERVAL_MS: Long = 100

internal fun getNPBackground(): Color {
    return when (GlobalPlayerState.np_theme_mode) {
        ThemeMode.BACKGROUND -> Theme.current.accent
        ThemeMode.ELEMENTS -> Theme.current.background
        ThemeMode.NONE -> Theme.current.background
    }
}

internal fun getNPOnBackground(): Color {
    return when (GlobalPlayerState.np_theme_mode) {
        ThemeMode.BACKGROUND -> Theme.current.on_accent
        ThemeMode.ELEMENTS -> Theme.current.accent
        ThemeMode.NONE -> Theme.current.on_background
    }
}

val LocalNowPlayingExpansion: ProvidableCompositionLocal<NowPlayingExpansionState> = staticCompositionLocalOf { GlobalPlayerState.expansion_state }

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun NowPlaying(swipe_state: SwipeableState<Int>, swipe_anchors: Map<Float, Int>) {
    LocalNowPlayingExpansion.current.init()

    AnimatedVisibility(
        LocalPlayerState.current.session_started,
        exit = slideOutVertically(),
        enter = slideInVertically(),
    ) {
        val density = LocalDensity.current
        val keyboard_insets = SpMp.context.getImeInsets()
        val player = LocalPlayerState.current
        val bottom_padding = player.nowPlayingBottomPadding()
        val screen_height = SpMp.context.getScreenHeight()

        val half_screen_height = screen_height.value * 0.5f
        val is_shut by remember { derivedStateOf { swipe_state.targetValue == 0 } }

        var switch_to_page: Int by remember { mutableStateOf(-1) }
        OnChangedEffect(switch_to_page) {
            if (switch_to_page >= 0) {
                swipe_state.animateTo(switch_to_page)
                switch_to_page = -1
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = getNPBackground(), contentColor = getNPOnBackground()),
            shape = RectangleShape,
            modifier = Modifier
                .fillMaxWidth()
                .requiredHeight(screen_height * (NOW_PLAYING_VERTICAL_PAGE_COUNT + 1))
                .offset {
                    IntOffset(
                        0,
                        with (density) {
                            ((half_screen_height.dp * NOW_PLAYING_VERTICAL_PAGE_COUNT) - swipe_state.offset.value.dp).toPx().toInt() - (keyboard_insets?.getBottom(density) ?: 0) - bottom_padding.toPx().toInt()
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
                    enabled = is_shut,
                    indication = null
                ) { switch_to_page = if (swipe_state.targetValue == 0) 1 else 0 }
        ) {
            BackHandler(!is_shut) {
                switch_to_page = swipe_state.targetValue - 1
            }

            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                NowPlayingCardContent(screen_height)
            }
        }
    }
}

@Composable
fun NowPlayingCardContent(page_height: Dp) {
    val status_bar_height = SpMp.context.getStatusBarHeight()
    val status_bar_height_percent = (status_bar_height.value * 0.75) / page_height.value
    val player = LocalPlayerState.current
    val expansion = LocalNowPlayingExpansion.current

    val under_status_bar by remember { derivedStateOf { 1f - expansion.get() < status_bar_height_percent } }
    val np_background = getNPBackground()

    LaunchedEffect(key1 = under_status_bar, key2 = np_background) {
        val colour = if (under_status_bar) np_background else Theme.current.background
        SpMp.context.setStatusBarColour(colour, !colour.isDark())
    }

    MinimisedProgressBar()

    val screen_width_dp = SpMp.context.getScreenWidth()

    Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.Top) {
        Column(
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .requiredHeight(page_height)
                .requiredWidth(screen_width_dp)
                .padding(top = (status_bar_height * expansion.get().coerceIn(0f, 1f)))
        ) {
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

        Column(
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .requiredHeight(page_height + (maxOf(0f, expansion.get() - 2f) * page_height) + player.nowPlayingBottomPadding())
                .requiredWidth(screen_width_dp)
        ) {
            QueueTab()
        }
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
