package com.spectre7.spmp.ui.layout.nowplaying

import LocalPlayerState
import SpMp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.github.krottv.compose.sliders.DefaultThumb
import com.github.krottv.compose.sliders.DefaultTrack
import com.github.krottv.compose.sliders.SliderValueHorizontal
import com.spectre7.spmp.model.mediaitem.Song
import com.spectre7.spmp.platform.composable.platformClickable
import com.spectre7.spmp.platform.composeScope
import com.spectre7.spmp.platform.vibrateShort
import com.spectre7.spmp.ui.component.MediaItemTitleEditDialog
import com.spectre7.spmp.ui.layout.mainpage.MINIMISED_NOW_PLAYING_HEIGHT
import com.spectre7.spmp.ui.layout.mainpage.MINIMISED_NOW_PLAYING_V_PADDING
import com.spectre7.spmp.ui.theme.Theme
import com.spectre7.utils.composable.Marquee
import com.spectre7.utils.composable.RecomposeOnInterval
import com.spectre7.utils.formatElapsedTime
import com.spectre7.utils.setAlpha
import kotlin.math.absoluteValue

const val NOW_PLAYING_MAIN_PADDING = 10f

internal const val MINIMISED_NOW_PLAYING_HORIZ_PADDING = 10f
internal const val OVERLAY_MENU_ANIMATION_DURATION: Int = 200
internal const val NOW_PLAYING_TOP_BAR_HEIGHT: Int = 40
internal const val MIN_EXPANSION = 0.07930607f

private const val SEEK_BAR_GRADIENT_OVERFLOW_RATIO = 0.3f

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

        composeScope {
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
                onThumbnailLoaded = { onThumbnailLoaded(it) },
                setThemeColour = { setThemeColour(it) },
                getSeekState = { seek_state }
            )
        }
    }

    val controls_visible by remember { derivedStateOf { expansion.getAbsolute() > 0.0f } }
    if (controls_visible) {
        Controls(
            current_song,
            {
                player.withPlayer {
                    seekTo((duration_ms * it).toLong())
                }
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
    var show_title_edit_dialog: Boolean by remember { mutableStateOf(false) }

    LaunchedEffect(song) {
        show_title_edit_dialog = false
    }

    if (show_title_edit_dialog && song != null) {
        MediaItemTitleEditDialog(song) { show_title_edit_dialog = false }
    }

    Spacer(Modifier.requiredHeight(30.dp))

    Box(
        modifier,
        contentAlignment = Alignment.TopCenter
    ) {

        @Composable
        fun PlayerButton(
            image: ImageVector,
            size: Dp = 60.dp,
            enabled: Boolean = true,
            onClick: () -> Unit
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clickable(
                        onClick = onClick,
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        enabled = enabled
                    )
                    .alpha(if (enabled) 1.0f else 0.5f)
            ) {
                val painter = rememberVectorPainter(image)

                Canvas(
                    Modifier
                        .requiredSize(size)
                        // https://stackoverflow.com/a/67820996
                        .graphicsLayer { alpha = 0.99f }
                ) {
                    with(painter) {
                        draw(this@Canvas.size)
                    }

                    val gradient_end = this@Canvas.size.width * 1.7f
                    drawRect(
                        Brush.linearGradient(
                            listOf(getNPOnBackground(), getNPBackground()),
                            end = Offset(gradient_end, gradient_end)
                        ),
                        blendMode = BlendMode.SrcAtop
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(25.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {

                Marquee(Modifier.fillMaxWidth()) {
                    Text(
                        song?.title ?: "",
                        fontSize = 20.sp,
                        color = getNPOnBackground(),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false,
                        // TODO Using ellipsis makes this go weird, no clue why
                        overflow = TextOverflow.Clip,
                        modifier = Modifier
                            .fillMaxWidth()
                            .platformClickable(
                                onAltClick = {
                                    show_title_edit_dialog = !show_title_edit_dialog
                                    SpMp.context.vibrateShort()
                                }
                            )
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
                                        SpMp.context.vibrateShort()
                                    }
                                }
                            }
                        )
                )
            }

            SeekBar(seek)

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Previous
                PlayerButton(
                    Icons.Rounded.SkipPrevious,
                    enabled = player.status.m_has_previous,
                    size = 60.dp
                ) {
                    player.player?.seekToPrevious()
                }

                // Play / pause
                PlayerButton(
                    if (player.status.m_playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    enabled = song != null,
                    size = 75.dp
                ) {
                    player.player?.playPause()
                }

                // Next
                PlayerButton(
                    Icons.Rounded.SkipNext,
                    enabled = player.status.m_has_next,
                    size = 60.dp
                ) {
                    player.player?.seekToNext()
                }
            }

            Spacer(Modifier.fillMaxHeight().weight(1f))

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
            player.player?.volume = it
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
        remember(seconds) { if (seconds < 0f) "" else formatElapsedTime(seconds.toLong()) },
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

        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 7.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                SeekBarTimeText(player.status.getPositionMillis(), getNPOnBackground())
                SeekBarTimeText(player.status.m_duration_ms, getNPOnBackground())
            }

            SliderValueHorizontal(
                value = getSliderValue(),
                onValueChange = {
                    position_override = it
                },
                onValueChangeFinished = {
                    seek(position_override!!)
                    old_position = player.status.getProgress()
                    cancel_area_side = null
                },
                thumbSizeInDp = DpSize(12.dp, 12.dp),
                track = { a, b, _, _, e -> SeekTrack(a, b, e, getNPAltOnBackground(), getNPOnBackground()) },
                thumb = { a, b, c, d, e -> DefaultThumb(a, b, c, d, e, getNPOnBackground(), 1f) }
            )
        }
    }
}

@Composable
fun SeekTrack(
    modifier: Modifier,
    progress: Float,
    enabled: Boolean,
    track_colour: Color,
    progress_colour: Color,
    height: Dp = 4.dp
) {
    val visual_progress by animateFloatAsState(progress, spring(stiffness = Spring.StiffnessLow))

    Canvas(
        Modifier
            .then(modifier)
            .height(height)
    ) {
        val left = Offset(0f, center.y)
        val right = Offset(size.width, center.y)
        val start = if (layoutDirection == LayoutDirection.Rtl) right else left
        val end = if (layoutDirection == LayoutDirection.Rtl) left else right

        val progress_width = (end.x - start.x) * visual_progress

        drawLine(
            Brush.horizontalGradient(
                listOf(progress_colour, track_colour),
                startX = progress_width,
                endX = progress_width + ((size.width - progress_width) * SEEK_BAR_GRADIENT_OVERFLOW_RATIO)
            ),
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
                start.x + progress_width,
                center.y
            ),
            size.height,
            StrokeCap.Round,
            alpha = if (enabled) 1f else 0.6f
        )
    }
}
