package com.spectre7.spmp.ui.layout.nowplaying

import MainOverlayMenu
import android.text.format.DateUtils
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.palette.graphics.Palette
import com.github.krottv.compose.sliders.DefaultThumb
import com.github.krottv.compose.sliders.DefaultTrack
import com.github.krottv.compose.sliders.SliderValueHorizontal
import com.google.android.exoplayer2.Player
import com.spectre7.spmp.PlayerServiceHost
import com.spectre7.spmp.R
import com.spectre7.spmp.model.MediaItem
import com.spectre7.spmp.model.Song
import com.spectre7.spmp.ui.component.LikeDislikeButton
import com.spectre7.spmp.ui.layout.MINIMISED_NOW_PLAYING_HEIGHT
import com.spectre7.spmp.ui.layout.PlayerViewContext
import com.spectre7.spmp.ui.layout.nowplaying.overlay.OverlayMenu
import com.spectre7.spmp.ui.theme.Theme
import com.spectre7.utils.*
import kotlin.concurrent.thread
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

const val OVERLAY_MENU_ANIMATION_DURATION: Int = 200

const val DEFAULT_THUMBNAIL_ROUNDING: Int = 5
const val MIN_THUMBNAIL_ROUNDING: Int = 0
const val MAX_THUMBNAIL_ROUNDING: Int = 50
const val TOP_BAR_HEIGHT: Int = 50
val NOW_PLAYING_MAIN_PADDING = 10.dp

@Composable
fun ColumnScope.NowPlayingMainTab(
    expansionProvider: () -> Float,
    page_height: Dp,
    thumbnail: ImageBitmap?,
    _setThumbnail: (ImageBitmap?) -> Unit,
    playerProvider: () -> PlayerViewContext,
    scroll: (pages: Int) -> Unit
) {
    val _expansion = expansionProvider()
    val expansion =
        if (_expansion <= 1f) maxOf(0.07930607f, _expansion)
        else 2f - _expansion

    var theme_colour by remember { mutableStateOf<Color?>(null) }
    fun setThemeColour(value: Color?) {
        theme_colour = value
        PlayerServiceHost.status.song?.theme_colour = theme_colour
    }
    
    val screen_width_dp = LocalConfiguration.current.screenWidthDp.dp

    var seek_state by remember { mutableStateOf(-1f) }
    var theme_palette by remember { mutableStateOf<Palette?>(null) }
    var loaded_song: Song? by remember { mutableStateOf(null) }

    val disappear_scale = minOf(1f, if (expansion < 0.5f) 1f else (1f - ((expansion - 0.5f) * 2f)))
    val appear_scale = minOf(1f, if (expansion > 0.5f) 1f else (expansion * 2f))

    val system_accent = MaterialTheme.colorScheme.primary
    LaunchedEffect(theme_colour) {
        Theme.currentThumbnnailColourChanged(theme_colour)
    }

    fun loadThumbnail(song: Song, quality: MediaItem.ThumbnailQuality) {
        _setThumbnail(song.loadThumbnail(quality)?.asImageBitmap())
        theme_palette = song.thumbnail_palette

        if (song.theme_colour != null) {
            theme_colour = song.theme_colour
        }
        else if (theme_palette != null) {
            theme_colour = song.getDefaultThemeColour()
        }

        loaded_song = song
    }

    LaunchedEffect(PlayerServiceHost.status.m_song, PlayerServiceHost.status.m_song?.canLoadThumbnail()) {
        val song = PlayerServiceHost.status.song
        if (loaded_song == song) {
            return@LaunchedEffect
        }

        if (song == null || !song.canLoadThumbnail()) {
            _setThumbnail(null)
            theme_palette = null
            theme_colour = null
            loaded_song = null
        }
        else if (song.isThumbnailLoaded(MediaItem.ThumbnailQuality.HIGH)) {
            loadThumbnail(song, MediaItem.ThumbnailQuality.HIGH)
        }
        else {
            thread {
                loadThumbnail(song, MediaItem.ThumbnailQuality.HIGH)
            }
        }
    }

    val thumbnail_rounding: MutableState<Int?>? = remember (PlayerServiceHost.status.m_song?.registry) { PlayerServiceHost.status.song?.registry?.getState("thumbnail_rounding") }
    val thumbnail_shape = RoundedCornerShape(thumbnail_rounding?.value ?: DEFAULT_THUMBNAIL_ROUNDING)
    var image_size by remember { mutableStateOf(IntSize(1, 1)) }
    val status_bar_height = getStatusBarHeight()
    val screen_height = LocalConfiguration.current.screenHeightDp.dp + status_bar_height

    val offsetProvider: Density.() -> IntOffset = {
        IntOffset(
            0,
            if (_expansion > 1f)
                (
                    (-screen_height * ((NOW_PLAYING_VERTICAL_PAGE_COUNT * 0.5f) - _expansion))
                    - ((TOP_BAR_HEIGHT.dp - status_bar_height) * (_expansion - 1f))
                ).toPx().toInt()
            else 0
        )
    }

    Row(
        Modifier
            .fillMaxWidth()
            .requiredHeight(TOP_BAR_HEIGHT.dp * appear_scale)
            .alpha(1f - disappear_scale)
            .offset(offsetProvider)
            .padding(start = NOW_PLAYING_MAIN_PADDING, end = NOW_PLAYING_MAIN_PADDING),
        horizontalArrangement = Arrangement.End
    ) {
        AnimatedVisibility(PlayerServiceHost.status.m_song != null) {
            if (PlayerServiceHost.status.m_song != null) {
                LikeDislikeButton(PlayerServiceHost.status.m_song!!, getNPOnBackground(playerProvider).setAlpha(0.5f))
            }
        }

        IconButton({
            TODO("Edit")
        }) {
            Icon(Icons.Filled.Edit, null, tint = getNPOnBackground(playerProvider).setAlpha(0.5f))
        }

        IconButton({
            PlayerServiceHost.status.m_song?.let { playerProvider().onMediaItemLongClicked(it) }
        }) {
            Icon(Icons.Filled.MoreHoriz, null, tint = getNPOnBackground(playerProvider).setAlpha(0.5f))
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.5f * max(expansion,
                (MINIMISED_NOW_PLAYING_HEIGHT + 20f) / page_height.value))
            .offset(offsetProvider)
            .padding(start = NOW_PLAYING_MAIN_PADDING, end = NOW_PLAYING_MAIN_PADDING)
    ) {

        var overlay_menu by remember { mutableStateOf<OverlayMenu?>(null) }
        var colourpick_callback by remember { mutableStateOf<((Color?) -> Unit)?>(null) }

        LaunchedEffect(expansion > 0f) {
            overlay_menu = null
        }

        var get_shutter_menu by remember { mutableStateOf<(@Composable () -> Unit)?>(null) }
        var shutter_menu_open by remember { mutableStateOf(false) }
        LaunchedEffect(expansion >= EXPANDED_THRESHOLD) {
            shutter_menu_open = false
            overlay_menu = null
        }

        Box(Modifier.aspectRatio(1f)) {
            Crossfade(thumbnail, animationSpec = tween(250)) { image ->
                if (image == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Theme.current.accent)
                    }
                }
                else {
                    Image(
                        image, null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(thumbnail_shape)
                            .onSizeChanged {
                                image_size = it
                            }
                            .run {
                                if (colourpick_callback == null) {
                                    this.clickable(
                                        enabled = expansion == 1f,
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() }
                                    ) {
                                        if (overlay_menu == null || overlay_menu!!.closeOnTap()) {
                                            overlay_menu = if (overlay_menu == null) MainOverlayMenu(
                                                { overlay_menu = it },
                                                { theme_palette },
                                                { colourpick_callback = it },
                                                {
                                                    setThemeColour(it)
                                                    overlay_menu = null
                                                },
                                                screen_width_dp
                                            ) else null
                                        }
                                    }
                                }
                                else {
                                    this.pointerInput(Unit) {
                                        detectTapGestures(
                                            onTap = { offset ->
                                                if (colourpick_callback != null) {
                                                    val bitmap_size = min(image.width, image.height)
                                                    var x = (offset.x / image_size.width) * bitmap_size
                                                    var y = (offset.y / image_size.height) * bitmap_size

                                                    if (image.width > image.height) {
                                                        x += (image.width - image.height) / 2
                                                    }
                                                    else if (image.height > image.width) {
                                                        y += (image.height - image.width) / 2
                                                    }

                                                    colourpick_callback?.invoke(
                                                        Color(image.asAndroidBitmap().getPixel(x.toInt(), y.toInt()))
                                                    )
                                                    colourpick_callback = null
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                    )
                }
            }

            // Thumbnail overlay menu
            androidx.compose.animation.AnimatedVisibility(
                overlay_menu != null,
                enter = fadeIn(tween(OVERLAY_MENU_ANIMATION_DURATION)),
                exit = fadeOut(tween(OVERLAY_MENU_ANIMATION_DURATION))
            ) {
                Box(
                    Modifier
                        .alpha(expansion)
                        .fillMaxSize()
                        .background(
                            Color.DarkGray.setAlpha(0.85f),
                            shape = thumbnail_shape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier
                            .size(with (LocalDensity.current) {
                                getInnerSquareSizeOfCircle(
                                    radius = image_size.height.toDp().value,
                                    corner_percent = thumbnail_rounding?.value ?: DEFAULT_THUMBNAIL_ROUNDING
                                ).dp
                            }),
                        contentAlignment = Alignment.Center
                    ) {
                        Crossfade(overlay_menu) { menu ->
                            if (menu != null) {
                                BackHandler {
                                    overlay_menu = null
                                    colourpick_callback = null
                                }
                            }

                            menu?.Menu(
                                { PlayerServiceHost.status.m_song!! },
                                expansion,
                                {
                                    get_shutter_menu = it
                                    shutter_menu_open = true
                                },
                                {
                                    overlay_menu = null
                                },
                                seek_state,
                                playerProvider
                            )
                        }
                    }
                }

                androidx.compose.animation.AnimatedVisibility(
                    shutter_menu_open,
                    enter = expandVertically(tween(200)),
                    exit = shrinkVertically(tween(200))
                ) {
                    val padding = 15.dp
                    CompositionLocalProvider(
                        LocalContentColor provides getNPOnBackground(playerProvider)
                    ) {
                        Column(
                            Modifier
                                .background(
                                    getNPBackground(playerProvider).setAlpha(0.9f),
                                    thumbnail_shape
                                )
                                .padding(start = padding, top = padding, end = padding)
                                .fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                Modifier
                                    .fillMaxHeight()
                                    .weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                get_shutter_menu?.invoke()
                            }
                            IconButton(onClick = { shutter_menu_open = false }) {
                                Icon(
                                    Icons.Filled.KeyboardArrowUp, null,
                                    tint = getNPOnBackground(playerProvider),
                                    modifier = Modifier.size(50.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth(0.9f * (1f - expansion))
                .scale(disappear_scale, 1f),
            horizontalArrangement = Arrangement.End
        ) {

            Spacer(Modifier.requiredWidth(10.dp))

            Text(
                PlayerServiceHost.status.m_song?.title ?: "",
                maxLines = 1,
                color = getNPOnBackground(playerProvider),
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .weight(1f)
                    .fillMaxWidth()
            )

            AnimatedVisibility(PlayerServiceHost.status.m_has_previous, enter = expandHorizontally(), exit = shrinkHorizontally()) {
                IconButton(
                    onClick = {
                        PlayerServiceHost.service.seekToPrevious()
                    }
                ) {
                    Image(
                        painterResource(R.drawable.ic_skip_previous),
                        null,
                        colorFilter = ColorFilter.tint(getNPOnBackground(playerProvider))
                    )
                }
            }

            AnimatedVisibility(PlayerServiceHost.status.m_song != null, enter = fadeIn(), exit = fadeOut()) {
                IconButton(
                    onClick = {
                        PlayerServiceHost.service.playPause()
                    }
                ) {
                    Image(
                        painterResource(if (PlayerServiceHost.status.m_playing) R.drawable.ic_pause else R.drawable.ic_play_arrow),
                        getString(if (PlayerServiceHost.status.m_playing) R.string.media_pause else R.string.media_play),
                        colorFilter = ColorFilter.tint(getNPOnBackground(playerProvider))
                    )
                }
            }

            AnimatedVisibility(PlayerServiceHost.status.m_has_next, enter = expandHorizontally(), exit = shrinkHorizontally()) {
                IconButton(
                    onClick = {
                        PlayerServiceHost.service.seekToNext()
                    }
                ) {
                    Image(
                        painterResource(R.drawable.ic_skip_next),
                        null,
                        colorFilter = ColorFilter.tint(getNPOnBackground(playerProvider))
                    )
                }
            }
        }
    }

    if (expansion > 0.0f) {
        Controls(
            playerProvider,
            {
                PlayerServiceHost.player.seekTo((PlayerServiceHost.player.duration * it).toLong())
                seek_state = it
            },
            scroll,
            Modifier
                .weight(1f)
                .offset(offsetProvider)
                .graphicsLayer {
                    alpha = 1f - (1f - _expansion).absoluteValue
                }
                .padding(start = NOW_PLAYING_MAIN_PADDING, end = NOW_PLAYING_MAIN_PADDING)
        )
    }
}

@Composable
private fun Controls(
    playerProvider: () -> PlayerViewContext,
    seek: (Float) -> Unit,
    scroll: (pages: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Spacer(Modifier.requiredHeight(30.dp))

    Box(
        modifier,
        contentAlignment = Alignment.TopCenter
    ) {

        @Composable
        fun PlayerButton(painter: Painter, size: Dp = 60.dp, alpha: Float = 1f, colourProvider: (() -> Color)? = null, label: String? = null, enabled: Boolean = true, on_click: () -> Unit) {
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
                val colour = colourProvider?.invoke() ?: getNPOnBackground(playerProvider)
                Image(
                    painter, null,
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

        @Composable
        fun PlayerButton(image_id: Int, size: Dp = 60.dp, alpha: Float = 1f, colourProvider: (() -> Color)? = null, label: String? = null, enabled: Boolean = true, on_click: () -> Unit) {
            PlayerButton(painterResource(image_id), size, alpha, colourProvider, label, enabled, on_click)
        }
//
        Column(verticalArrangement = Arrangement.spacedBy(35.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {

                // Title text
                Marquee(false) {
                    Text(
                        PlayerServiceHost.status.m_song?.title ?: "",
                        fontSize = 17.sp,
                        color = getNPOnBackground(playerProvider),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Artist text
                Text(
                    PlayerServiceHost.status.m_song?.artist?.title ?: "",
                    fontSize = 12.sp,
                    color = getNPOnBackground(playerProvider),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            PlayerServiceHost.status.song?.artist?.also {
                                playerProvider().onMediaItemClicked(it)
                            }
                        }
                )
            }

            SeekBar(playerProvider, seek)

            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                // Previous
                PlayerButton(R.drawable.ic_skip_previous, enabled = PlayerServiceHost.status.m_has_previous) {
                    PlayerServiceHost.service.seekToPrevious()
                }

                // Play / pause
                PlayerButton(
                    if (PlayerServiceHost.status.m_playing) R.drawable.ic_pause else R.drawable.ic_play_arrow,
                    enabled = PlayerServiceHost.status.m_song != null
                ) {
                    PlayerServiceHost.service.playPause()
                }

                // Next
                PlayerButton(R.drawable.ic_skip_next, enabled = PlayerServiceHost.status.m_has_next) {
                    PlayerServiceHost.service.seekToNext()
                }
            }

            val bottom_row_colour = getNPOnBackground(playerProvider).setAlpha(0.5f)
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically, 
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                var volume_slider_visible by remember { mutableStateOf(false) }
                Row(
                    Modifier.weight(1f, false).animateContentSize(),
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

                IconButton(
                    { scroll(1) }
                ) {
                    Icon(Icons.Filled.KeyboardArrowDown, null, tint = bottom_row_colour)
                }

                AnimatedVisibility(!volume_slider_visible, enter = expandHorizontally(), exit = shrinkHorizontally()) {
                    Spacer(Modifier.width(48.dp).background(Color.Green))
                }
            }
        }
    }
}

@Composable
private fun VolumeSlider(colour: Color, modifier: Modifier = Modifier) {
    SliderValueHorizontal(
        value = PlayerServiceHost.status.m_volume,
        onValueChange = {
            PlayerServiceHost.status.m_volume = it
        },
        thumbSizeInDp = DpSize(12.dp, 12.dp),
        track = { a, b, c, d, e -> DefaultTrack(a, b, c, d, e, colour.setAlpha(0.5f), colour) },
        thumb = { a, b, c, d, e -> DefaultThumb(a, b, c, d, e, colour, 1f) },
        modifier = modifier
    )
}

@Composable
private fun SeekBarTimeText(time: Float, colour: Color) {
    Text(
        remember(time) { if (time < 0f) "??:??" else DateUtils.formatElapsedTime(time.toLong()) },
        fontSize = 10.sp,
        fontWeight = FontWeight.Light,
        color = colour
    )
}

@Composable
private fun SeekBar(playerProvider: () -> PlayerViewContext, seek: (Float) -> Unit) {
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
                    Offset(size.width * (grab_start_position!! - SEEK_CANCEL_THRESHOLD / 2.0f),
                        center.y),
                    Offset(size.width * (grab_start_position!! + SEEK_CANCEL_THRESHOLD / 2.0f),
                        center.y),
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
            if (PlayerServiceHost.status.position != old_position) {
                old_position = null
                position_override = null
            }
            else {
                return position_override!!
            }
        }
        return position_override ?: PlayerServiceHost.status.position
    }

    Row(
        Modifier.recomposeOnInterval(POSITION_UPDATE_INTERVAL_MS),
        verticalAlignment = Alignment.CenterVertically, 
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {

        SeekBarTimeText(PlayerServiceHost.status.position_seconds, getNPOnBackground(playerProvider))

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
            track = { a, b, _, _, e -> SeekTrack(a, b, e, getNPOnBackground(playerProvider).setAlpha(0.5f), getNPOnBackground(playerProvider)) },
            thumb = { a, b, c, d, e -> DefaultThumb(a, b, c, d, e, getNPOnBackground(playerProvider), 1f) },
            modifier = Modifier.weight(1f)
        )

        SeekBarTimeText(PlayerServiceHost.status.m_duration, getNPOnBackground(playerProvider))
    }
}
