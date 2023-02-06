package com.spectre7.spmp.ui.layout.nowplaying

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
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
import com.spectre7.spmp.model.MediaItem
import com.spectre7.spmp.ui.component.MultiSelector
import com.spectre7.spmp.ui.layout.PlayerViewContext
import com.spectre7.utils.getStatusBarHeight
import com.spectre7.utils.getString
import com.spectre7.utils.setColourAlpha
import com.spectre7.utils.vibrateShort

enum class AccentColourSource { THUMBNAIL, SYSTEM }
enum class ThemeMode { BACKGROUND, ELEMENTS, NONE }

enum class NowPlayingTab { RELATED, PLAYER, QUEUE }

const val SEEK_CANCEL_THRESHOLD = 0.03f
const val EXPANDED_THRESHOLD = 0.9f
val NOW_PLAYING_MAIN_PADDING = 10.dp

@Composable
fun NowPlaying(expansion: Float, max_height: Float, close: () -> Unit, player: PlayerViewContext) {
    val expanded = expansion >= EXPANDED_THRESHOLD

    val systemui_controller = rememberSystemUiController()
    val status_bar_height_percent = remember { (getStatusBarHeight(MainActivity.context).value * 0.75) / max_height }

    LaunchedEffect(key1 = expansion, key2 = MainActivity.theme.getBackground(true)) {
        systemui_controller.setSystemBarsColor(
            color = if (1f - expansion < status_bar_height_percent) MainActivity.theme.getBackground(true) else MainActivity.theme.default_n_background
        )
    }

    MinimisedProgressBar(expansion)

    val screen_width_dp = remember { LocalConfiguration.current.screenWidthDp.dp }
    val screen_width_px = remember { with(LocalDensity.current) { screen_width_dp.roundToPx() } }
    val main_padding_px = remember { with(LocalDensity.current) { NOW_PLAYING_MAIN_PADDING.roundToPx() } }

    val thumbnail = remember { mutableStateOf<ImageBitmap?>(null) }

    Box(Modifier.padding(NOW_PLAYING_MAIN_PADDING)) {

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

        Column(verticalArrangement = Arrangement.Top, modifier = Modifier.fillMaxHeight()) {

            Row(
                Modifier
                    .horizontalScroll(tab_scroll_state, false)
                    .requiredWidth(screen_width_dp * 3)
                    .weight(1f)
            ) {
                for (page in 0 until NowPlayingTab.values().size) {
                    Tab(
                        NowPlayingTab.values()[page],
                        current_tab,
                        expansion,
                        max_height,
                        thumbnail,
                        close,
                        player,
                        Modifier.requiredWidth(screen_width_dp - (NOW_PLAYING_MAIN_PADDING * 2))
                    )
                }
            }

            if (expansion > 0.0f) {
                TabSelector(current_tab)
            }
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
        trackColor = setColourAlpha(MainActivity.theme.getOnBackground(true), 0.5),
        modifier = Modifier
            .requiredHeight(2.dp)
            .fillMaxWidth()
            .alpha(1f - expansion)
    )
}

@Composable
fun TabSelector(current_tab: MutableState<NowPlayingTab>) {
    MultiSelector(
        3,
        current_tab.value.ordinal,
        Modifier.requiredHeight(60.dp * 0.8f),
        Modifier.aspectRatio(1f),
        colour = setColourAlpha(MainActivity.theme.getOnBackground(true), 0.75),
        background_colour = MainActivity.theme.getBackground(true),
        on_selected = { current_tab.value = NowPlayingTab.values()[it] }
    ) { index ->

        val tab = NowPlayingTab.values()[index]

        Box(
            contentAlignment = Alignment.Center
        ) {

            val colour = if (tab == current_tab.value) MainActivity.theme.getBackground(true) else MainActivity.theme.getOnBackground(true)

            Image(
                when(tab) {
                    NowPlayingTab.PLAYER -> rememberVectorPainter(Icons.Filled.PlayArrow)
                    NowPlayingTab.QUEUE -> painterResource(R.drawable.ic_music_queue)
                    NowPlayingTab.RELATED -> rememberVectorPainter(Icons.Filled.Menu)
                }, "",
                Modifier
                    .requiredSize(60.dp * 0.4f, 60.dp)
                    .offset(y = (-7).dp),
                colorFilter = ColorFilter.tint(colour)
            )
            Text(when (tab) {
                NowPlayingTab.PLAYER -> getString(R.string.now_playing_player)
                NowPlayingTab.QUEUE -> getString(R.string.now_playing_queue)
                NowPlayingTab.RELATED -> getString(R.string.now_playing_related)
            }, color = colour, fontSize = 10.sp, modifier = Modifier.offset(y = (10).dp))
        }
    }
}

@Composable
fun Tab(
    tab: NowPlayingTab,
    open_tab: MutableState<NowPlayingTab>,
    expansion: Float,
    max_height: Float,
    thumbnail: MutableState<ImageBitmap?>,
    close: () -> Unit,
    player: PlayerViewContext,
    modifier: Modifier = Modifier
) {
    BackHandler(tab == open_tab.value && expansion >= EXPANDED_THRESHOLD) {
        if (tab == NowPlayingTab.PLAYER) {
            close()
        }
        else {
            open_tab.value = NowPlayingTab.PLAYER
        }
    }

    Column(verticalArrangement = Arrangement.Top, modifier = modifier.fillMaxHeight().then(
        if (tab == NowPlayingTab.PLAYER) padding(15.dp * expansion) else padding(top = 20.dp)
    )) {
        when (tab) {
            NowPlayingTab.PLAYER -> {
                MainTab(
                    Modifier.weight(1f),
                    expansion,
                    max_height,
                    thumbnail.value,
                    { thumbnail.value = it },
                    remember { player.copy(
                        onClickedOverride = {
                            player.onMediaItemClicked(it)
                            close()
                        }
                    ) }
                )
            }
            NowPlayingTab.QUEUE -> QueueTab(Modifier.weight(1f), player)
            else -> {}
        }
    }
}