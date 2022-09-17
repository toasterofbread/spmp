package com.spectre7.spmp.ui.components

import android.util.Log
import android.view.MotionEvent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.palette.graphics.Palette
import com.github.krottv.compose.sliders.DefaultThumb
import com.github.krottv.compose.sliders.DefaultTrack
import com.github.krottv.compose.sliders.SliderValueHorizontal
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.android.exoplayer2.Player
import com.spectre7.utils.getString
import com.spectre7.spmp.PlayerHost
import com.spectre7.spmp.R
import com.spectre7.spmp.model.Song
import com.spectre7.spmp.ui.layout.MINIMISED_NOW_PLAYING_HEIGHT
import com.spectre7.spmp.ui.layout.PlayerStatus
import com.spectre7.spmp.ui.components.*
import com.spectre7.utils.*
import kotlin.concurrent.thread
import kotlin.math.max
import androidx.compose.ui.geometry.Offset

enum class NowPlayingThemeMode { BACKGROUND, ELEMENTS }
enum class NowPlayingOverlayMenu { NONE, MAIN, PALETTE, LYRICS }
enum class NowPlayingTab { RELATED, PLAYER, QUEUE }

const val SEEK_CANCEL_THRESHOLD = 0.05f

@OptIn(ExperimentalPagerApi::class)
@Composable
fun NowPlaying(_expansion: Float, max_height: Float, p_status: PlayerStatus, background_colour: Animatable<Color, AnimationVector4D>) {

    val expansion = if (_expansion < 0.08f) 0.0f else _expansion
    val inv_expansion = -expansion + 1.0f

    fun getSongTitle(): String {
        if (p_status.song == null) {
            return "-----"
        }
        return p_status.song!!.getTitle()
    }

    fun getSongArtist(): String {
        if (p_status.song == null) {
            return "---"
        }
        return p_status.song!!.artist.nativeData.name
    }

    val theme_mode = NowPlayingThemeMode.ELEMENTS
    val systemui_controller = rememberSystemUiController()

    var thumbnail by remember { mutableStateOf<ImageBitmap?>(null) }
    var theme_palette by remember { mutableStateOf<Palette?>(null) }
    var palette_index by remember { mutableStateOf(2) }

    val default_background_colour = MaterialTheme.colorScheme.background
    val default_on_background_colour = MaterialTheme.colorScheme.onBackground

    val on_background_colour = remember { Animatable(default_on_background_colour) }

    val colour_filter = ColorFilter.tint(on_background_colour.value)

    fun setThumbnail(thumb: ImageBitmap?) {
        if (thumb == null) {
            thumbnail = null
            theme_palette = null
            return
        }

        thumbnail = thumb
        Palette.from(thumbnail!!.asAndroidBitmap()).generate {
            theme_palette = it
        }
    }

    LaunchedEffect(p_status.song?.getId()) {
        if (p_status.song == null) {
            setThumbnail(null)
        }
        else if (p_status.song!!.thumbnailLoaded(true)) {
            setThumbnail(p_status.song!!.loadThumbnail(true).asImageBitmap())
        }
        else {
            thread {
                setThumbnail(p_status.song!!.loadThumbnail(true).asImageBitmap())
            }
        }
    }

    LaunchedEffect(key1 = theme_palette, key2 = palette_index) {
        if (theme_palette == null) {
            background_colour.animateTo(default_background_colour)
            on_background_colour.animateTo(default_on_background_colour)
        }
        else {
            val colour = getPaletteColour(theme_palette!!, palette_index)
            if (colour == null) {
                background_colour.animateTo(default_background_colour)
                on_background_colour.animateTo(default_on_background_colour)
            } else {

                when (theme_mode) {
                    NowPlayingThemeMode.BACKGROUND -> {
                        background_colour.animateTo(colour)
                        on_background_colour.animateTo(
                            getContrastedColour(colour)
                        )
                    }
                    NowPlayingThemeMode.ELEMENTS -> {
                        on_background_colour.animateTo(colour)
                    }
                }
            }
        }
    }

    LaunchedEffect(key1 = expansion >= 1.0f, key2 = background_colour.value) {
        systemui_controller.setSystemBarsColor(
            color = if (expansion >= 1.0f) background_colour.value else default_background_colour
        )
    }

    if (expansion < 1.0f) {
        LinearProgressIndicator(
            progress = p_status.position,
            color = on_background_colour.value,
            trackColor = setColourAlpha(on_background_colour.value, 0.5),
            modifier = Modifier
                .requiredHeight(2.dp)
                .fillMaxWidth()
                .alpha(inv_expansion)
        )
    }

    Box(Modifier.padding(10.dp + (15.dp * expansion))) {

        val tab_state = rememberPagerState(NowPlayingTab.PLAYER.ordinal)
        var current_tab by remember { mutableStateOf(NowPlayingTab.PLAYER) }
        val button_size = 60.dp

        LaunchedEffect(current_tab) {
            tab_state.animateScrollToPage(current_tab.ordinal)
        }

        LaunchedEffect(expansion >= 1.0f) {
            if (expansion < 1.0f) {
                current_tab = NowPlayingTab.PLAYER
                tab_state.animateScrollToPage(current_tab.ordinal)
            }
        }

        @Composable
        fun Tab(tab: NowPlayingTab, modifier: Modifier = Modifier) {
            Column(verticalArrangement = Arrangement.Top, modifier = modifier.fillMaxHeight()) {
                if (tab == NowPlayingTab.PLAYER) {
                    Spacer(Modifier.requiredHeight(50.dp * expansion))

                    val min_height_fraction = (MINIMISED_NOW_PLAYING_HEIGHT + 20f) / max_height

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.5f * max(expansion, min_height_fraction))
                    ) {

                        var overlay_menu by remember { mutableStateOf(NowPlayingOverlayMenu.NONE) }

                        LaunchedEffect(expansion == 0.0f) {
                            overlay_menu = NowPlayingOverlayMenu.NONE
                        }

                        Box(Modifier.aspectRatio(1f)) {
                            Crossfade(thumbnail, animationSpec = tween(250)) { image ->
                                if (image != null) {
                                    Image(
                                        image, "",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(5))
                                            .clickable(
                                                enabled = expansion == 1.0f,
                                                indication = null,
                                                interactionSource = remember { MutableInteractionSource() }
                                            ) {
                                                if (overlay_menu == NowPlayingOverlayMenu.NONE || overlay_menu == NowPlayingOverlayMenu.MAIN || overlay_menu == NowPlayingOverlayMenu.PALETTE) {
                                                    overlay_menu =
                                                        if (overlay_menu == NowPlayingOverlayMenu.NONE) NowPlayingOverlayMenu.MAIN else NowPlayingOverlayMenu.NONE
                                                }
                                            }
                                    )
                                }
                            }

                            // Thumbnail overlay menu
                            androidx.compose.animation.AnimatedVisibility(overlay_menu != NowPlayingOverlayMenu.NONE, enter = fadeIn(), exit = fadeOut()) {
                                Box(
                                    Modifier
                                        .background(
                                            setColourAlpha(Color.DarkGray, 0.85),
                                            shape = RoundedCornerShape(5)
                                        )
                                        .fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Crossfade(overlay_menu) { menu ->
                                        when (menu) {
                                            NowPlayingOverlayMenu.MAIN ->
                                                Column(
                                                    Modifier
                                                        .fillMaxSize()
                                                        .padding(20.dp), verticalArrangement = Arrangement.SpaceBetween) {
                                                    p_status.song?.artist?.Preview(false)

                                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {

                                                        Box(
                                                            Modifier
                                                                .background(
                                                                    background_colour.value,
                                                                    CircleShape
                                                                )
                                                                .size(40.dp)
                                                                .padding(8.dp)
                                                                .clickable {
                                                                    overlay_menu =
                                                                        NowPlayingOverlayMenu.LYRICS
                                                                }
                                                        ) {
                                                            Image(
                                                                painterResource(R.drawable.ic_music_note), "",
                                                                colorFilter = ColorFilter.tint(on_background_colour.value)
                                                            )
                                                        }

                                                        Box(
                                                            Modifier
                                                                .background(
                                                                    background_colour.value,
                                                                    CircleShape
                                                                )
                                                                .size(40.dp)
                                                                .padding(8.dp)
                                                                .clickable {
                                                                    overlay_menu =
                                                                        NowPlayingOverlayMenu.PALETTE
                                                                }
                                                        ) {
                                                            Image(
                                                                painterResource(R.drawable.ic_palette), "",
                                                                colorFilter = ColorFilter.tint(on_background_colour.value)
                                                            )
                                                        }

                                                    }
                                                }
                                            NowPlayingOverlayMenu.PALETTE ->
                                                PaletteSelector(theme_palette) { index, _ ->
                                                    palette_index = index
                                                    overlay_menu = NowPlayingOverlayMenu.NONE
                                                }
                                            NowPlayingOverlayMenu.LYRICS ->
                                                if (p_status.song != null) {
                                                    LyricsDisplay(p_status.song!!, { overlay_menu = NowPlayingOverlayMenu.NONE }, p_status)
                                                }
                                            NowPlayingOverlayMenu.NONE -> {}
                                        }
                                    }
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth(inv_expansion * 0.9f)) {

                            Spacer(Modifier.requiredWidth(10.dp))

                            Text(
                                getSongTitle(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .align(Alignment.CenterVertically)
                                    .weight(1f)
                                    .fillMaxWidth()
                            )

                            AnimatedVisibility(p_status.has_previous, enter = expandHorizontally(), exit = shrinkHorizontally()) {
                                IconButton(
                                    onClick = {
                                        PlayerHost.interact {
                                            it.seekToPreviousMediaItem()
                                        }
                                    }
                                ) {
                                    Image(
                                        painterResource(R.drawable.ic_skip_previous),
                                        "",
                                        colorFilter = colour_filter
                                    )
                                }
                            }

                            AnimatedVisibility(p_status.song != null, enter = fadeIn(), exit = fadeOut()) {
                                IconButton(
                                    onClick = {
                                        PlayerHost.interactService {
                                            it.playPause()
                                        }
                                    }
                                ) {
                                    Image(
                                        painterResource(if (p_status.playing) R.drawable.ic_pause else R.drawable.ic_play_arrow),
                                        getString(if (p_status.playing) R.string.media_pause else R.string.media_play),
                                        colorFilter = colour_filter
                                    )
                                }
                            }

                            AnimatedVisibility(p_status.has_next, enter = expandHorizontally(), exit = shrinkHorizontally()) {
                                IconButton(
                                    onClick = {
                                        PlayerHost.interact {
                                            it.seekToNextMediaItem()
                                        }
                                    }
                                ) {
                                    Image(
                                        painterResource(R.drawable.ic_skip_next),
                                        "",
                                        colorFilter = colour_filter
                                    )
                                }
                            }
                        }
                    }

                    if (expansion > 0.0f) {
                        Spacer(Modifier.requiredHeight(30.dp))

                        Box(
                            Modifier
                                .alpha(expansion)
                                .weight(1f), contentAlignment = Alignment.TopCenter) {

                            @Composable
                            fun PlayerButton(painter: Painter, size: Dp = button_size, alpha: Float = 1f, colour: Color = on_background_colour.value, label: String? = null, enabled: Boolean = true, on_click: () -> Unit) {
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
                                    Image(
                                        painter, "",
                                        Modifier
                                            .requiredSize(size, button_size)
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
                            fun PlayerButton(image_id: Int, size: Dp = button_size, alpha: Float = 1f, colour: Color = on_background_colour.value, label: String? = null, enabled: Boolean = true, on_click: () -> Unit) {
                                PlayerButton(painterResource(image_id), size, alpha, colour, label, enabled, on_click)
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(35.dp)) {

                                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {

                                    // Title text
                                    Text(getSongTitle(),
                                        fontSize = 17.sp,
                                        color = on_background_colour.value,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .animateContentSize())

                                    // Artist text
                                    Text(getSongArtist(),
                                        fontSize = 12.sp,
                                        color = on_background_colour.value,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .animateContentSize())

                                }

                                var slider_moving by remember { mutableStateOf(false) }
                                var slider_value by remember { mutableStateOf(0.0f) }
                                var old_p_position by remember { mutableStateOf<Float?>(null) }
                                var slider_start_position by remember { mutableStateOf<Float?>(null) }

                                LaunchedEffect(p_status.position) {
                                    if (!slider_moving && p_status.position != old_p_position) {
                                        slider_value = p_status.position
                                        old_p_position = null
                                    }
                                }

                                @Composable
                                fun SeekTrack(
                                    modifier: Modifier,
                                    progress: Float,
                                    enabled: Boolean,
                                    track_colour: Color = Color(0xffD3B4F7),
                                    progress_colour: Color = Color(0xff7000F8),
                                    height: Dp = 4.dp,
                                    highlight: Pair<Float, Float>? = null,
                                    highlight_colour: Color = setColourAlpha(Color.Red, 0.2)
                                ) {
                                    Canvas(
                                        Modifier
                                            .then(modifier)
                                            .height(height)
                                    ) {

                                        val isRtl = layoutDirection == LayoutDirection.Rtl
                                        val slider_left = Offset(0f, center.y)
                                        val slider_right = Offset(size.width, center.y)
                                        val slider_start = if (isRtl) slider_right else slider_left
                                        val slider_end = if (isRtl) slider_left else slider_right
                                        drawLine(
                                            track_colour,
                                            slider_start,
                                            slider_end,
                                            size.height,
                                            StrokeCap.Round,
                                            alpha = if (enabled) 1f else 0.6f
                                        )

                                        val slider_value_end = Offset(
                                            slider_start.x + (slider_end.x - slider_start.x) * progress,
                                            center.y
                                        )
                                        val slider_value_start = Offset(
                                            slider_start.x,
                                            center.y
                                        )
                                        drawLine(
                                            progress_colour,
                                            slider_value_start,
                                            slider_value_end,
                                            size.height,
                                            StrokeCap.Round,
                                            alpha = if (enabled) 1f else 0.6f
                                        )

                                        if (highlight != null) {
                                            drawLine(
                                                highlight_colour,
                                                Offset(size.width * highlight.first, center.y),
                                                Offset(size.width * highlight.second, center.y),
                                                size.height,
                                                StrokeCap.Square,
                                                alpha = if (enabled) 1f else 0.6f
                                            )
                                        }
                                    }
                                }

                                // Song position seek bar
                                var in_cancel_area by remember { mutableStateOf(false) }
                                val highlight = if (slider_start_position != null) Pair(
                                    slider_start_position!! - SEEK_CANCEL_THRESHOLD / 2.0f, slider_start_position!! + SEEK_CANCEL_THRESHOLD / 2.0f
                                ) else null

                                SliderValueHorizontal(
                                    value = slider_value,
                                    onValueChange = {
                                        if (slider_start_position == null) {
                                            slider_start_position = slider_value
                                        }

                                        slider_moving = true
                                        slider_value = it

                                        if (in_cancel_area != (slider_value >= slider_start_position!! - SEEK_CANCEL_THRESHOLD / 2.0 && slider_value <= slider_start_position!! + SEEK_CANCEL_THRESHOLD / 2.0)) {
                                            in_cancel_area = !in_cancel_area
                                            if (in_cancel_area) {
                                                vibrate(0.01)
                                            }
                                        }

                                    },
                                    onValueChangeFinished = {
                                        slider_moving = false
                                        old_p_position = p_status.position
                                        slider_start_position = null

                                        if (!in_cancel_area) {
                                            PlayerHost.interact {
                                                it.seekTo((it.duration * slider_value).toLong())
                                            }
                                        }
                                        else {
                                            vibrate(0.01)
                                            in_cancel_area = false
                                        }
                                    },
                                    thumbSizeInDp = DpSize(12.dp, 12.dp),
                                    track = { a, b, _, _, c -> SeekTrack(a, b, c, setColourAlpha(on_background_colour.value, 0.5), on_background_colour.value, highlight = highlight) },
                                    thumb = { a, b, c, d, e -> DefaultThumb(a, b, c, d, e, on_background_colour.value, 1f) }
                                )

                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                ) {

                                    val utility_separation = 25.dp

                                    // Toggle shuffle
                                    PlayerButton(R.drawable.ic_shuffle, button_size * 0.65f, if (p_status.shuffle) 1f else 0.25f) {
                                        PlayerHost.interact {
                                            it.shuffleModeEnabled = !it.shuffleModeEnabled
                                        }
                                    }

                                    Spacer(Modifier.requiredWidth(utility_separation))

                                    // Previous
                                    PlayerButton(R.drawable.ic_skip_previous, enabled = p_status.has_previous) {
                                        PlayerHost.interact {
                                            it.seekToPreviousMediaItem()
                                        }
                                    }

                                    // Play / pause
                                    PlayerButton(if (p_status.playing) R.drawable.ic_pause else R.drawable.ic_play_arrow, enabled = p_status.song != null) {
                                        PlayerHost.interactService {
                                            it.playPause()
                                        }
                                    }

                                    // Next
                                    PlayerButton(R.drawable.ic_skip_next, enabled = p_status.has_next) {
                                        PlayerHost.interact {
                                            it.seekToNextMediaItem()
                                        }
                                    }

                                    Spacer(Modifier.requiredWidth(utility_separation))

                                    // Cycle repeat mode
                                    PlayerButton(
                                        if (p_status.repeat_mode == Player.REPEAT_MODE_ONE) R.drawable.ic_repeat_one else R.drawable.ic_repeat,
                                        button_size * 0.65f,
                                        if (p_status.repeat_mode != Player.REPEAT_MODE_OFF) 1f else 0.25f
                                    ) {
                                        PlayerHost.interact {
                                            it.repeatMode = when (it.repeatMode) {
                                                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                                                Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_OFF
                                                else -> Player.REPEAT_MODE_ALL
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                else if (tab == NowPlayingTab.QUEUE) {
                    QueueTab(p_status, on_background_colour.value)
                }
            }
        }

        Column(verticalArrangement = Arrangement.Top, modifier = Modifier.fillMaxHeight()) {

            // Box(Modifier.weight(1f)) {
            //     Crossfade(current_tab, animationSpec = tween(100)) { tab ->
            //         Tab(tab)
            //     }
            // }

            if (expansion >= 1.0f) {
                HorizontalPager(count = NowPlayingTab.values().size, state = tab_state, modifier = Modifier.weight(1f)) { page ->
                    Tab(NowPlayingTab.values()[page])
                }
            }
            else {
                Tab(current_tab, Modifier.weight(1f))
            }

            if (expansion > 0.0f) {
                MultiSelector(
                    3,
                    current_tab.ordinal,
                    Modifier.requiredHeight(button_size * 0.8f),
                    Modifier.aspectRatio(1f),
                    colour = setColourAlpha(on_background_colour.value, 0.75),
                    background_colour = background_colour.value,
                    on_selected = { current_tab = NowPlayingTab.values()[it] }
                ) { index ->

                    val tab = NowPlayingTab.values()[index]

                    Box(
                        contentAlignment = Alignment.Center
                    ) {

                        val colour = if (tab == current_tab) background_colour.value else on_background_colour.value

                        Image(
                            when(tab) {
                                NowPlayingTab.PLAYER -> rememberVectorPainter(Icons.Filled.PlayArrow)
                                NowPlayingTab.QUEUE -> painterResource(R.drawable.ic_music_queue)
                                NowPlayingTab.RELATED -> rememberVectorPainter(Icons.Filled.Menu)
                            }, "",
                            Modifier
                                .requiredSize(button_size * 0.4f, button_size)
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
        }
    }
}

// Popup menu
//
//    val menu_visible = remember { MutableTransitionState(false) }
//
//    if (menu_visible.targetState || menu_visible.currentState) {
//        Popup(
//            alignment = Alignment.Center,
//            properties = PopupProperties(
//                excludeFromSystemGesture = false,
//                focusable = true
//            ),
//            onDismissRequest = { menu_visible.targetState = false },
//            offset = IntOffset(0, -60)
//        ) {
//            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
//                AnimatedVisibility(
//                    visibleState = menu_visible,
//                    enter = expandHorizontally(tween(150)) + slideInVertically(
//                        initialOffsetY = { it / 8 }),
//                    exit = shrinkHorizontally(tween(150)) + slideOutVertically(
//                        targetOffsetY = { it / 8 })
//                ) {
//                    Card(
//                        modifier = Modifier
//                            .fillMaxWidth(0.9f)
//                            .fillMaxHeight(0.85f),
//                        colors = CardDefaults.cardColors(
//                            MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.onBackground
//                        )
//                    ) {
//
//                    }
//                }
//            }
//        }
//    }
