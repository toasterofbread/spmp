package com.spectre7.spmp.ui.layout.nowplaying

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.swipeable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.PlayerServiceHost
import com.spectre7.spmp.model.Song
import com.spectre7.spmp.ui.layout.MINIMISED_NOW_PLAYING_HEIGHT
import com.spectre7.spmp.ui.layout.PlayerViewContext
import com.spectre7.spmp.ui.layout.getScreenHeight
import com.spectre7.utils.*

enum class AccentColourSource { THUMBNAIL, SYSTEM }
enum class ThemeMode { BACKGROUND, ELEMENTS, NONE }

private enum class NowPlayingVerticalPage { MAIN, QUEUE }
private val VERTICAL_PAGE_COUNT = NowPlayingVerticalPage.values().size

const val SEEK_CANCEL_THRESHOLD = 0.03f
const val EXPANDED_THRESHOLD = 0.9f
val NOW_PLAYING_MAIN_PADDING = 10.dp

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun NowPlaying(playerProvider: () -> PlayerViewContext, swipe_state: SwipeableState<Int>) {
    AnimatedVisibility(PlayerServiceHost.session_started, enter = slideInVertically(), exit = slideOutVertically()) {
        val screen_height = getScreenHeight()

        var switch_to_page: Int by remember { mutableStateOf(-1) }
        OnChangedEffect(switch_to_page) {
            if (switch_to_page >= 0) {
                swipe_state.animateTo(switch_to_page)
                switch_to_page = -1
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MainActivity.theme.getBackground(true)),
            shape = RectangleShape,
            modifier = Modifier
                .fillMaxWidth()
                .requiredHeight(screen_height * VERTICAL_PAGE_COUNT)
                .offset(y = (screen_height * VERTICAL_PAGE_COUNT * 0.5f) - swipe_state.offset.value.dp)
                .swipeable(
                    state = swipe_state,
                    anchors = (0..VERTICAL_PAGE_COUNT).associateBy { if (it == 0) MINIMISED_NOW_PLAYING_HEIGHT else screen_height.value * it },
                    thresholds = { _, _ -> FractionalThreshold(0.2f) },
                    orientation = Orientation.Vertical,
                    reverseDirection = true,
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    enabled = swipe_state.targetValue == 0,
                    indication = null
                ) { switch_to_page = if (swipe_state.targetValue == 0) 1 else 0 }
        ) {
            BackHandler(swipe_state.targetValue != 0) {
                switch_to_page = swipe_state.targetValue - 1
            }

            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                NowPlayingCardContent(
                    remember { { swipe_state.offset.value / screen_height.value } },
                    screen_height,
                    { switch_to_page = if (swipe_state.targetValue == 0) 1 else 0 },
                    { switch_to_page = swipe_state.targetValue + it },
                    playerProvider
                )
            }
        }
    }
}

@Composable
fun NowPlayingCardContent(
    expansionProvider: () -> Float,
    page_height: Dp,
    close: () -> Unit,
    scroll: (pages: Int) -> Unit,
    playerProvider: () -> PlayerViewContext
) {
    val systemui_controller = rememberSystemUiController()
    val status_bar_height_percent = (getStatusBarHeight(MainActivity.context).value * 0.75) / page_height.value

    LaunchedEffect(key1 = expansionProvider(), key2 = MainActivity.theme.getBackground(true)) {
        systemui_controller.setSystemBarsColor(
            color = if (1f - expansionProvider() < status_bar_height_percent) MainActivity.theme.getBackground(true) else MainActivity.theme.default_n_background
        )
    }

    MinimisedProgressBar(expansionProvider)

    val screen_width_dp = LocalConfiguration.current.screenWidthDp.dp
    val thumbnail = remember { mutableStateOf<ImageBitmap?>(null) }

    Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.Top) {
        Column(
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .requiredHeight(page_height)
                .requiredWidth(screen_width_dp - (NOW_PLAYING_MAIN_PADDING * 2))
                .padding(start = NOW_PLAYING_MAIN_PADDING, end = NOW_PLAYING_MAIN_PADDING, top = (getStatusBarHeight(MainActivity.context)) * expansionProvider())
        ) {
            NowPlayingMainTab(
                minOf(expansionProvider(), 1f),
                page_height,
                thumbnail.value,
                { thumbnail.value = it },
                remember {
                    {
                        playerProvider().copy(
                            onClickedOverride = {
                                playerProvider().onMediaItemClicked(it)
                                close()
                            }
                        )
                    }
                },
                scroll
            )
        }

        Column(
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .requiredHeight(page_height)
                .requiredWidth(screen_width_dp - (NOW_PLAYING_MAIN_PADDING * 2))
                .padding(NOW_PLAYING_MAIN_PADDING)
        ) {
            QueueTab(remember { { (expansionProvider() - 1f).coerceIn(0f, 1f) } }, playerProvider, scroll)
        }
    }
}

@Composable
fun MinimisedProgressBar(expansionProvider: () -> Float) {
    LinearProgressIndicator(
        progress = PlayerServiceHost.status.m_position,
        color = MainActivity.theme.getOnBackground(true),
        trackColor = MainActivity.theme.getOnBackground(true).setAlpha(0.5),
        modifier = Modifier
            .requiredHeight(2.dp)
            .fillMaxWidth()
            .graphicsLayer {
                alpha = 1f - expansionProvider()
            }
    )
}

