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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.swipeable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.*
import com.github.krottv.compose.sliders.DefaultThumb
import com.github.krottv.compose.sliders.SliderValueHorizontal
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.PlayerServiceHost
import com.spectre7.spmp.R
import com.spectre7.spmp.ui.component.MultiSelector
import com.spectre7.spmp.ui.layout.MINIMISED_NOW_PLAYING_HEIGHT
import com.spectre7.spmp.ui.layout.PlayerViewContext
import com.spectre7.spmp.ui.layout.getScreenHeight
import com.spectre7.utils.*

enum class AccentColourSource { THUMBNAIL, SYSTEM }
enum class ThemeMode { BACKGROUND, ELEMENTS, NONE }
enum class NowPlayingTab { RELATED, PLAYER, QUEUE }

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
                    anchors = (0..VERTICAL_PAGE_COUNT).associateBy{ if (it == 0) MINIMISED_NOW_PLAYING_HEIGHT else screen_height.value * it },
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
                NowPlayingCardContent(swipe_state.offset.value / screen_height.value, screen_height, { switch_to_page = if (swipe_state.targetValue == 0) 1 else 0 }, playerProvider)
            }
        }
    }
}

@Composable
fun NowPlayingCardContent(expansion: Float, page_height: Dp, close: () -> Unit, playerProvider: () -> PlayerViewContext) {
    val expanded = expansion >= EXPANDED_THRESHOLD

    val systemui_controller = rememberSystemUiController()
    val status_bar_height_percent = (getStatusBarHeight(MainActivity.context).value * 0.75) / page_height.value

    LaunchedEffect(key1 = expansion, key2 = MainActivity.theme.getBackground(true)) {
        systemui_controller.setSystemBarsColor(
            color = if (1f - expansion < status_bar_height_percent) MainActivity.theme.getBackground(true) else MainActivity.theme.default_n_background
        )
    }

    MinimisedProgressBar(expansion)

    val screen_width_dp = LocalConfiguration.current.screenWidthDp.dp
    val screen_width_px = with(LocalDensity.current) { screen_width_dp.roundToPx() }
    val main_padding_px = with(LocalDensity.current) { NOW_PLAYING_MAIN_PADDING.roundToPx() }

    val thumbnail = remember { mutableStateOf<ImageBitmap?>(null) }

    Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.Top) {
        val current_tab = remember { mutableStateOf(NowPlayingTab.PLAYER) }

        fun getTabScrollTarget(): Int {
            return current_tab.value.ordinal * (screen_width_px - (main_padding_px * 2))
        }
        val tab_scroll_state = rememberScrollState(getTabScrollTarget())

        LaunchedEffect(current_tab.value) {
            tab_scroll_state.animateScrollTo(getTabScrollTarget())
        }

        LaunchedEffect(expanded) {
            if (!expanded) {
                current_tab.value = NowPlayingTab.PLAYER
//                tab_scroll_state.scrollTo(getTabScrollTarget())
            }
        }

        Column(
            verticalArrangement = Arrangement.Top,
            modifier = Modifier.requiredHeight(page_height).requiredWidth(screen_width_dp - (NOW_PLAYING_MAIN_PADDING * 2)).padding(NOW_PLAYING_MAIN_PADDING)
        ) {
            NowPlayingMainTab(
                minOf(expansion, 1f),
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
                }
            )
        }

        Column(
            verticalArrangement = Arrangement.Top,
            modifier = Modifier.requiredHeight(page_height).requiredWidth(screen_width_dp - (NOW_PLAYING_MAIN_PADDING * 2)).padding(NOW_PLAYING_MAIN_PADDING)
        ) {
            QueueTab((expansion - 1f).coerceIn(0f, 1f), playerProvider)
        }
    }
}

@Composable
fun SeekBar(seek: (Float) -> Unit) {
    var position_override by remember { mutableStateOf<Float?>(null) }
    var old_position by remember { mutableStateOf<Float?>(null) }
    var grab_start_position by remember { mutableStateOf<Float?>(null) }

    @Composable
    fun SeekTrack(
        modifier: Modifier,
        progress: Float,
        enabled: Boolean,
        track_colour: Color = Color(0xffD3B4F7),
        progress_colour: Color = Color(0xff7000F8),
        height: Dp = 4.dp,
        highlight_colour: Color = setColourAlpha(Color.Red, 0.2)
    ) {
        Canvas(
            Modifier
                .then(modifier)
                .height(height)
        ) {

            val left = Offset(0f, center.y)
            val right = Offset(size.width, center.y)
            val start = if (layoutDirection == LayoutDirection.Rtl) right else left
            val end = if (layoutDirection == LayoutDirection.Rtl) left else right

            drawLine(
                track_colour,
                start,
                end,
                size.height,
                StrokeCap.Round,
                alpha = if (enabled) 1f else 0.6f
            )

            drawLine(
                progress_colour,
                Offset(
                    start.x,
                    center.y
                ),
                Offset(
                    start.x + (end.x - start.x) * progress,
                    center.y
                ),
                size.height,
                StrokeCap.Round,
                alpha = if (enabled) 1f else 0.6f
            )

            if (grab_start_position != null) {
                drawLine(
                    highlight_colour,
                    Offset(size.width * (grab_start_position!! - SEEK_CANCEL_THRESHOLD / 2.0f), center.y),
                    Offset(size.width * (grab_start_position!! + SEEK_CANCEL_THRESHOLD / 2.0f), center.y),
                    size.height,
                    StrokeCap.Square,
                    alpha = if (enabled) 1f else 0.6f
                )
            }
        }
    }

    var cancel_area_side: Int? by remember { mutableStateOf(null) }

    fun getSliderValue(): Float {
        if (position_override != null && old_position != null) {
            if (PlayerServiceHost.status.m_position != old_position) {
                old_position = null
                position_override = null
            }
            else {
                return position_override!!
            }
        }
        return position_override ?: PlayerServiceHost.status.m_position
    }

    SliderValueHorizontal(
        value = getSliderValue(),
        onValueChange = {
            if (grab_start_position == null) {
                grab_start_position = PlayerServiceHost.status.position
            }

            position_override = it

            val side = if (it <= grab_start_position!! - SEEK_CANCEL_THRESHOLD / 2.0) -1 else if (it >= grab_start_position!! + SEEK_CANCEL_THRESHOLD / 2.0) 1 else 0
            if (side != cancel_area_side) {
                if (side == 0 || side + (cancel_area_side ?: 0) == 0) {
                    vibrateShort()
                }
                cancel_area_side = side
            }
        },
        onValueChangeFinished = {
            if (cancel_area_side == 0 && grab_start_position != null) {
                vibrateShort()
            }
            else {
                seek(position_override!!)
            }
            old_position = PlayerServiceHost.status.position
            grab_start_position = null
            cancel_area_side = null
        },
        thumbSizeInDp = DpSize(12.dp, 12.dp),
        track = { a, b, _, _, c -> SeekTrack(a, b, c, setColourAlpha(MainActivity.theme.getOnBackground(true), 0.5), MainActivity.theme.getOnBackground(true)) },
        thumb = { a, b, c, d, e -> DefaultThumb(a, b, c, d, e, MainActivity.theme.getOnBackground(true), 1f) }
    )
}

@Composable
fun MinimisedProgressBar(expansion: Float) {
    LinearProgressIndicator(
        progress = PlayerServiceHost.status.m_position,
        color = MainActivity.theme.getOnBackground(true),
        trackColor = MainActivity.theme.getOnBackground(true).setAlpha(0.5),
        modifier = Modifier
            .requiredHeight(2.dp)
            .fillMaxWidth()
            .alpha(1f - expansion)
    )
}

