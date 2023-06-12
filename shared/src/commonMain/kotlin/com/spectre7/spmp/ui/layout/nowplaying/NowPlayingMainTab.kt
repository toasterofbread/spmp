package com.spectre7.spmp.ui.layout.nowplaying

import LocalPlayerState
import SpMp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.github.krottv.compose.sliders.DefaultThumb
import com.github.krottv.compose.sliders.DefaultTrack
import com.github.krottv.compose.sliders.SliderValueHorizontal
import com.spectre7.spmp.model.mediaitem.Song
import com.spectre7.spmp.platform.vibrateShort
import com.spectre7.spmp.ui.layout.mainpage.MINIMISED_NOW_PLAYING_HEIGHT
import com.spectre7.spmp.ui.layout.mainpage.MINIMISED_NOW_PLAYING_V_PADDING
import com.spectre7.spmp.ui.theme.Theme
import com.spectre7.utils.composable.Marquee
import com.spectre7.utils.composable.OnChangedEffect
import com.spectre7.utils.composable.RecomposeOnInterval
import com.spectre7.utils.formatElapsedTime
import com.spectre7.utils.setAlpha
import kotlin.math.absoluteValue

const val NOW_PLAYING_MAIN_PADDING = 10f

internal const val MINIMISED_NOW_PLAYING_HORIZ_PADDING = 10f
internal const val OVERLAY_MENU_ANIMATION_DURATION: Int = 200
internal const val NOW_PLAYING_TOP_BAR_HEIGHT: Int = 40
internal const val MIN_EXPANSION = 0.07930607f

@Composable
fun ColumnScope.NowPlayingMainTab() {
    val player = LocalPlayerState.current
    val current_song: Song? = player.status.m_song
    val expansion = LocalNowPlayingExpansion.current

    var theme_colour by remember { mutableStateOf<Color?>(null) }
    fun setThemeColour(value: Color?) {
        theme_colour = value
        current_song?.theme_colour = theme_colour
    }

    var seek_state by remember { mutableStateOf(-1f) }

    LaunchedEffect(theme_colour) {
        Theme.currentThumbnnailColourChanged(theme_colour)
    }

    fun onThumbnailLoaded(song: Song?) {
        if (song != current_song) {
            return
        }

        if (song == null) {
            theme_colour = null
        }
        else if (song.theme_colour != null) {
            theme_colour = song.theme_colour
        }
        else if (song.canGetThemeColour()) {
            theme_colour = song.getDefaultThemeColour()
        }
    }

    LaunchedEffect(current_song) {
        onThumbnailLoaded(current_song)
    }

    val screen_height = SpMp.context.getScreenHeight()

    val offsetProvider: Density.() -> IntOffset = remember {
        {
            val absolute = expansion.getBounded()
            IntOffset(
                0,
                if (absolute > 1f)
                    (
                        -screen_height * ((NOW_PLAYING_VERTICAL_PAGE_COUNT * 0.5f) - absolute)
                    ).toPx().toInt()
                else 0
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .offset(offsetProvider),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopBar()

        val screen_width = SpMp.context.getScreenWidth()

        ThumbnailRow(
            Modifier
                .padding(
                    top = MINIMISED_NOW_PLAYING_V_PADDING.dp
                        * (1f - expansion.getBounded())
                        .coerceAtLeast(0f)
                )
                .height(
                    (expansion.getAbsolute() * (screen_width - (NOW_PLAYING_MAIN_PADDING.dp * 2)))
                        .coerceAtLeast(
                            MINIMISED_NOW_PLAYING_HEIGHT.dp - (MINIMISED_NOW_PLAYING_V_PADDING.dp * 2)
                        )
                )
                .width(
                    screen_width -
                        (2 * (MINIMISED_NOW_PLAYING_HORIZ_PADDING.dp + ((MINIMISED_NOW_PLAYING_HORIZ_PADDING.dp - NOW_PLAYING_MAIN_PADDING.dp) * expansion.getAbsolute())))
                ),
            { onThumbnailLoaded(it) },
            { setThemeColour(it) },
            { seek_state }
        )
    }

    if (expansion.getAbsolute() > 0.0f) {
        Controls(
            current_song,
            {
                player.player.seekTo((player.player.duration_ms * it).toLong())
                seek_state = it
            },
            Modifier
                .weight(1f)
                .offset(offsetProvider)
                .graphicsLayer {
                    alpha = 1f - (1f - expansion.getBounded()).absoluteValue
                }
                .padding(horizontal = NOW_PLAYING_MAIN_PADDING.dp)
        )
    }
}

@Composable
private fun Controls(
    song: Song?,
    seek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val player = LocalPlayerState.current

    Spacer(Modifier.requiredHeight(30.dp))

    Box(
        modifier,
        contentAlignment = Alignment.TopCenter
    ) {

        @Composable
        fun PlayerButton(image: ImageVector, size: Dp = 60.dp, alpha: Float = 1f, colourProvider: (() -> Color)? = null, label: String? = null, enabled: Boolean = true, on_click: () -> Unit) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clickable(
                        onClick = on_click,
                        indication = rememberRipple(
                            radius = 25.dp,
                            bounded = false
                        ),
                        interactionSource = remember { MutableInteractionSource() },
                        enabled = enabled
                    )
                    .alpha(if (enabled) 1.0f else 0.5f)
            ) {
                val colour = colourProvider?.invoke() ?: getNPOnBackground()
                Image(
                    image, null,
                    Modifier
                        .requiredSize(size, 60.dp)
                        .offset(y = if (label != null) (-7).dp else 0.dp),
                    colorFilter = ColorFilter.tint(colour),
                    alpha = alpha
                )
                if (label != null) {
                    Text(label, color = colour, fontSize = 10.sp, modifier = Modifier.offset(y = (10).dp))
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(35.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {

                var title_text by remember { mutableStateOf(song?.title ?: "") }
                OnChangedEffect(song?.title) {
                    title_text = song?.title ?: ""
                }

                Marquee(Modifier.fillMaxWidth()) {
                    Text(
                        title_text,
                        fontSize = 17.sp,
                        color = getNPOnBackground(),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false,
                        // TODO Using ellipsis makes this go weird, no clue why
                        overflow = TextOverflow.Clip,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Text(
                    song?.artist?.title ?: "",
                    fontSize = 12.sp,
                    color = getNPOnBackground(),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .platformClickable(
                            onClick = {
                                if (song?.artist?.is_for_item == false) {
                                    player.status.m_song?.artist?.also {
                                        player.onMediaItemClicked(it)
                                    }
                                }
                            },
                            onAltClick = {
                                if (song?.artist?.is_for_item == false) {
                                    player.status.m_song?.artist?.also {
                                        player.onMediaItemLongClicked(it)
                                    }
                                }
                            }
                        )
                )
            }

            SeekBar(seek)

            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                // Previous
                PlayerButton(Icons.Filled.SkipPrevious, enabled = player.status.m_has_previous) {
                    player.player.seekToPrevious()
                }

                // Play / pause
                PlayerButton(
                    if (player.status.m_playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    enabled = song != null
                ) {
                    player.player.playPause()
                }

                // Next
                PlayerButton(Icons.Filled.SkipNext, enabled = player.status.m_has_next) {
                    player.player.seekToNext()
                }
            }

            val bottom_row_colour = getNPOnBackground().setAlpha(0.5f)
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                var volume_slider_visible by remember { mutableStateOf(false) }
                Row(
                    Modifier
                        .weight(1f, false)
                        .animateContentSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        { volume_slider_visible = !volume_slider_visible }
                    ) {
                        Icon(Icons.Filled.VolumeUp, null, tint = bottom_row_colour)
                    }

                    AnimatedVisibility(volume_slider_visible) {
                        VolumeSlider(bottom_row_colour)
                    }
                }

                val expansion = LocalNowPlayingExpansion.current
                IconButton(
                    { expansion.scroll(1) }
                ) {
                    Icon(Icons.Filled.KeyboardArrowDown, null, tint = bottom_row_colour)
                }

                AnimatedVisibility(!volume_slider_visible, enter = expandHorizontally(), exit = shrinkHorizontally()) {
                    Spacer(Modifier
                        .width(48.dp)
                        .background(Color.Green))
                }
            }
        }
    }
}

@Composable
private fun VolumeSlider(colour: Color, modifier: Modifier = Modifier) {
    val player = LocalPlayerState.current
    SliderValueHorizontal(
        value = player.status.m_volume,
        onValueChange = {
            player.player.volume = it
        },
        thumbSizeInDp = DpSize(12.dp, 12.dp),
        track = { a, b, c, d, e -> DefaultTrack(a, b, c, d, e, colour.setAlpha(0.5f), colour) },
        thumb = { a, b, c, d, e -> DefaultThumb(a, b, c, d, e, colour, 1f) },
        modifier = modifier
    )
}

@Composable
private fun SeekBarTimeText(time: Long, colour: Color) {
    val seconds = time / 1000f
    Text(
        remember(seconds) { if (seconds < 0f) "??:??" else formatElapsedTime(seconds.toLong()) },
        fontSize = 10.sp,
        fontWeight = FontWeight.Light,
        color = colour
    )
}

@Composable
private fun SeekBar(seek: (Float) -> Unit) {
    val player = LocalPlayerState.current

    var position_override by remember { mutableStateOf<Float?>(null) }
    var old_position by remember { mutableStateOf<Float?>(null) }
    var grab_start_position by remember { mutableStateOf<Float?>(null) }
    var cancel_area_side: Int? by remember { mutableStateOf(null) }

    fun getSliderValue(): Float {
        if (position_override != null && old_position != null) {
            if (player.status.getProgress() != old_position) {
                old_position = null
                position_override = null
            }
            else {
                return position_override!!
            }
        }
        return position_override ?: player.status.getProgress()
    }

    RecomposeOnInterval(POSITION_UPDATE_INTERVAL_MS, player.status.m_playing) { state ->
        state
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {

            SeekBarTimeText(player.status.getPositionMillis(), getNPOnBackground())

            SliderValueHorizontal(
                value = getSliderValue(),
                onValueChange = {
                    if (grab_start_position == null) {
                        grab_start_position = player.status.getProgress()
                    }

                    position_override = it

                    val side = if (it <= grab_start_position!! - SEEK_CANCEL_THRESHOLD / 2.0) -1 else if (it >= grab_start_position!! + SEEK_CANCEL_THRESHOLD / 2.0) 1 else 0
                    if (side != cancel_area_side) {
                        if (side == 0 || side + (cancel_area_side ?: 0) == 0) {
                            SpMp.context.vibrateShort()
                        }
                        cancel_area_side = side
                    }
                },
                onValueChangeFinished = {
                    if (cancel_area_side == 0 && grab_start_position != null) {
                        SpMp.context.vibrateShort()
                    }
                    else {
                        seek(position_override!!)
                    }
                    old_position = player.status.getProgress()
                    grab_start_position = null
                    cancel_area_side = null
                },
                thumbSizeInDp = DpSize(12.dp, 12.dp),
                track = { a, b, _, _, e -> SeekTrack(a, b, e, grab_start_position, getNPOnBackground().setAlpha(0.5f), getNPOnBackground()) },
                thumb = { a, b, c, d, e -> DefaultThumb(a, b, c, d, e, getNPOnBackground(), 1f) },
                modifier = Modifier.weight(1f)
            )

            SeekBarTimeText(player.status.m_duration_ms, getNPOnBackground())
        }
    }
}

@Composable
fun SeekTrack(
    modifier: Modifier,
    progress: Float,
    enabled: Boolean,
    grab_start_position: Float?,
    track_colour: Color = Color(0xffD3B4F7),
    progress_colour: Color = Color(0xff7000F8),
    height: Dp = 4.dp,
    highlight_colour: Color = Color.Red.setAlpha(0.2f)
) {
    androidx.compose.foundation.Canvas(
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
                Offset(size.width * (grab_start_position - SEEK_CANCEL_THRESHOLD / 2.0f),
                    center.y),
                Offset(size.width * (grab_start_position + SEEK_CANCEL_THRESHOLD / 2.0f),
                    center.y),
                size.height,
                StrokeCap.Square,
                alpha = if (enabled) 1f else 0.6f
            )
        }
    }
}
