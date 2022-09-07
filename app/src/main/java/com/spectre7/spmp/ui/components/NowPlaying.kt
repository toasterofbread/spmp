package com.spectre7.spmp.ui.components

import android.app.Activity
import android.util.Log
import android.graphics.drawable.VectorDrawable
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.ViewCompat
import androidx.palette.graphics.Palette
import com.github.krottv.compose.sliders.DefaultThumb
import com.github.krottv.compose.sliders.DefaultTrack
import com.github.krottv.compose.sliders.SliderValueHorizontal
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.android.exoplayer2.Player
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.R
import com.spectre7.spmp.ui.layout.MINIMISED_NOW_PLAYING_HEIGHT
import com.spectre7.spmp.ui.layout.PlayerStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.lang.RuntimeException
import kotlin.concurrent.thread
import kotlin.math.max

fun Boolean.toInt() = if (this) 1 else 0

fun setColourAlpha(colour: Color, alpha: Double): Color {
    return Color(ColorUtils.setAlphaComponent(colour.toArgb(), (255 * alpha).toInt()))
}

fun getPaletteColour(palette: Palette, type: Int): Color? {
    val ret = Color(
        when (type) {
            0 -> palette.getDominantColor(Color.Unspecified.toArgb())
            1 -> palette.getVibrantColor(Color.Unspecified.toArgb())
            2 -> palette.getDarkVibrantColor(Color.Unspecified.toArgb())
            3 -> palette.getDarkMutedColor(Color.Unspecified.toArgb())
            4 -> palette.getLightVibrantColor(Color.Unspecified.toArgb())
            5 -> palette.getLightMutedColor(Color.Unspecified.toArgb())
            6 -> palette.getMutedColor(Color.Unspecified.toArgb())
            else -> throw RuntimeException("Invalid palette colour type '$type'")
        }
    )

    if (ret.toArgb() == Color.Unspecified.toArgb()) {
        return null
    }

    return ret
}

fun isColorDark(colour: Color): Boolean {
 return ColorUtils.calculateLuminance(colour.toArgb()) < 0.5;
}

enum class ThemeMode { BACKGROUND, ELEMENTS }
enum class OverlayMenu { NONE, MAIN, PALETTE, LYRICS }

@Composable
fun NowPlaying(_expansion: Float, max_height: Float, p_status: PlayerStatus, background_colour: Animatable<Color, AnimationVector4D>) {

    val expansion = if (_expansion < 0.08f) 0.0f else _expansion
    val exx = expansion == 1.0f
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

    val theme_mode = ThemeMode.BACKGROUND
    val systemui_controller = rememberSystemUiController()

    var thumbnail by remember { mutableStateOf<ImageBitmap?>(null) }
    var theme_palette by remember { mutableStateOf<Palette?>(null) }
    var palette_index by remember { mutableStateOf(2) }

    val default_background_colour = MaterialTheme.colorScheme.background
    val default_on_background_colour = MaterialTheme.colorScheme.onBackground

    val default_on_dark_colour = Color.White
    val default_on_light_colour = Color.Black

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
                    ThemeMode.BACKGROUND -> {
                        background_colour.animateTo(colour)
                        on_background_colour.animateTo(
                            if (isColorDark(colour)) default_on_dark_colour
                            else default_on_light_colour
                        )
                    }
                    ThemeMode.ELEMENTS -> {
                        on_background_colour.animateTo(colour)
                    }
                }
            }
        }
    }

    LaunchedEffect(key1 = exx, key2 = background_colour.value) {
        systemui_controller.setSystemBarsColor(
            color = if (exx) background_colour.value else default_background_colour
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

    Box(Modifier.padding(10.dp + (15.dp * expansion))) {

        // Main column
        Column(verticalArrangement = Arrangement.Top, modifier = Modifier.fillMaxHeight()) {

            Spacer(Modifier.requiredHeight(50.dp * expansion))

            val min_height_fraction = (MINIMISED_NOW_PLAYING_HEIGHT + 20f) / max_height

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.5f * max(expansion, min_height_fraction))
            ) {

                var overlay_menu by remember { mutableStateOf(OverlayMenu.NONE) }

                LaunchedEffect(expansion == 0.0f) {
                    overlay_menu = OverlayMenu.NONE
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
                                        if (overlay_menu == OverlayMenu.NONE || overlay_menu == OverlayMenu.MAIN || overlay_menu == OverlayMenu.PALETTE) {
                                            overlay_menu =
                                                if (overlay_menu == OverlayMenu.NONE) OverlayMenu.MAIN else OverlayMenu.NONE
                                        }
                                    }
                            )
                        }
                    }

                    // Thumbnail overlay menu
                    androidx.compose.animation.AnimatedVisibility(overlay_menu != OverlayMenu.NONE, enter = fadeIn(), exit = fadeOut()) {
                        Box(
                            Modifier
                                .background(
                                    setColourAlpha(Color.DarkGray, 0.85),
                                    shape = RoundedCornerShape(5)
                                )
                                .fillMaxSize(), contentAlignment = Alignment.Center) {
                            Crossfade(overlay_menu) { menu ->
                                when (menu) {
                                    OverlayMenu.MAIN ->
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
                                                            overlay_menu = OverlayMenu.LYRICS
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
                                                            overlay_menu = OverlayMenu.PALETTE
                                                        }
                                                ) {
                                                    Image(
                                                        painterResource(R.drawable.ic_palette), "",
                                                        colorFilter = ColorFilter.tint(on_background_colour.value)
                                                    )
                                                }

                                            }
                                        }
                                    OverlayMenu.PALETTE ->
                                        PaletteSelector(theme_palette) { index, _ ->
                                            palette_index = index
                                            overlay_menu = OverlayMenu.NONE
                                        }
                                    OverlayMenu.LYRICS ->
                                        if (p_status.song != null) {
                                            LyricsDisplay(p_status.song!!, { overlay_menu = OverlayMenu.NONE })
                                        }
                                    OverlayMenu.NONE -> {}
                                }
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.End, modifier = Modifier
                    .fillMaxWidth()
                    ) {

                    Spacer(Modifier.requiredWidth(10.dp))

                    Text(
                        getSongTitle(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .weight(1f)
                            .fillMaxWidth()
//                            .animateContentSize()
//                            .clipToBounds()
                    )

                    AnimatedVisibility(
                        p_status.has_previous,
                        enter = expandHorizontally(),
                        exit = shrinkHorizontally()
                    ) {
                        IconButton(
                            onClick = {
                                MainActivity.player.interact {
                                    it.player.seekToPreviousMediaItem()
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
                                MainActivity.player.interact {
                                    it.playPause()
                                }
                            }
                        ) {
                            Image(
                                painterResource(if (p_status.playing) R.drawable.ic_pause else R.drawable.ic_play_arrow),
                                MainActivity.getString(if (p_status.playing) R.string.media_pause else R.string.media_play),
                                colorFilter = colour_filter
                            )
                        }
                    }

                    AnimatedVisibility(p_status.has_next, enter = expandHorizontally(), exit = shrinkHorizontally()) {
                        IconButton(
                            onClick = {
                                MainActivity.player.interact {
                                    it.player.seekToNextMediaItem()
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

            val button_size = 60.dp

            if (expansion > 0.0f) {
                Spacer(Modifier.requiredHeight(30.dp))

                Box(Modifier.alpha(expansion).weight(1f), contentAlignment = Alignment.TopCenter) {

                    @Composable
                    fun PlayerButton(painter: Painter, size: Dp = button_size, alpha: Float = 1f, colour: Color = on_background_colour.value, label: String? = null, enabled: Boolean = true, on_click: () -> Unit) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clickable(
                                    onClick = on_click,
                                    indication = rememberRipple(radius = 25.dp, bounded = false),
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

                        LaunchedEffect(p_status.position) {
                            if (!slider_moving && p_status.position != old_p_position) {
                                slider_value = p_status.position
                                old_p_position = null
                            }
                        }

                        SliderValueHorizontal(
                            value = slider_value,
                            onValueChange = { slider_moving = true; slider_value = it },
                            onValueChangeFinished = {
                                slider_moving = false
                                old_p_position = p_status.position
                                MainActivity.player.interact {
                                    it.player.seekTo((it.player.duration * slider_value).toLong())
                                }
                            },
                            thumbSizeInDp = DpSize(12.dp, 12.dp),
                            track = { a, b, c, d, e -> DefaultTrack(a, b, c, d, e, setColourAlpha(on_background_colour.value, 0.5), on_background_colour.value) },
                            thumb = { a, b, c, d, e -> DefaultThumb(a, b, c, d, e, on_background_colour.value, 1f) }
                        )

                        Row(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth().weight(1f),
                        ) {

                            val utility_separation = 25.dp

                            PlayerButton(R.drawable.ic_shuffle, button_size * 0.65f, if (p_status.shuffle) 1f else 0.25f) { MainActivity.player.interact {
                                it.player.shuffleModeEnabled = !it.player.shuffleModeEnabled
                            } }

                            Spacer(Modifier.requiredWidth(utility_separation))

                            PlayerButton(R.drawable.ic_skip_previous, enabled = p_status.has_previous) {
                                MainActivity.player.interact { it.player.seekToPreviousMediaItem() }
                            }

                            PlayerButton(if (p_status.playing) R.drawable.ic_pause else R.drawable.ic_play_arrow, enabled = p_status.song != null) {
                                MainActivity.player.interact { it.playPause() }
                            }

                            PlayerButton(R.drawable.ic_skip_next, enabled = p_status.has_next) {
                                MainActivity.player.interact { it.player.seekToNextMediaItem() }
                            }

                            Spacer(Modifier.requiredWidth(utility_separation))

                            PlayerButton(
                                if (p_status.repeat_mode == Player.REPEAT_MODE_ONE) R.drawable.ic_repeat_one else R.drawable.ic_repeat,
                                button_size * 0.65f,
                                if (p_status.repeat_mode != Player.REPEAT_MODE_OFF) 1f else 0.25f) {

                                MainActivity.player.interact {
                                    it.player.repeatMode = when (it.player.repeatMode) {
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

            var selected by remember { mutableStateOf(1) }

            if (expansion > 0.0f)

            MultiSelector(
                3,
                selected,
                Modifier.requiredHeight(button_size * 0.8f),
                Modifier.aspectRatio(1f),
                colour = setColourAlpha(on_background_colour.value, 0.75),
                background_colour = background_colour.value,
                on_selected = { selected = it }
            ) { index ->
                Box(
                    contentAlignment = Alignment.Center
                ) {

                    val colour = if (index == selected) background_colour.value else on_background_colour.value

                    Image(
                        when(index) {
                            0 -> rememberVectorPainter(Icons.Filled.Menu)
                            1 -> rememberVectorPainter(Icons.Filled.PlayArrow)
                            else -> painterResource(R.drawable.ic_music_queue)
                        }, "",
                        Modifier
                            .requiredSize(button_size * 0.4f, button_size)
                            .offset(y = (-7).dp),
                        colorFilter = ColorFilter.tint(colour)
                    )
                    Text(when (index) {
                        0 -> "Salad bar"
                        1 -> "Player"
                        else -> "Queue"
                    }, color = colour, fontSize = 10.sp, modifier = Modifier.offset(y = (10).dp))
                }
            }
        }
    }
}

@Composable
fun PaletteSelector(palette: Palette?, on_selected: (index: Int, colour: Color) -> Unit) {
    if (palette != null) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxSize()
        ) {
            for (i in 0 until 5) {
                val colour = getPaletteColour(palette, i)
                if (colour != null) {
                    Button(
                        onClick = {
                            on_selected(i, colour)
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(75))
                            .requiredSize(40.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colour
                        )
                    ) {}
                }
            }
        }
    }
}

@Stable
interface MultiSelectorState {
    val selectedIndex: Float
    val startCornerPercent: Int
    val endCornerPercent: Int

    fun selectOption(scope: CoroutineScope, index: Int)
}

@Stable
class MultiSelectorStateImpl(
    val option_count: Int,
    selected_option: Int,
) : MultiSelectorState {

    override val selectedIndex: Float
        get() = _selectedIndex.value
    override val startCornerPercent: Int
        get() = _startCornerPercent.value.toInt()
    override val endCornerPercent: Int
        get() = _endCornerPercent.value.toInt()

    private var _selectedIndex = Animatable(selected_option.toFloat())
    private var _startCornerPercent = Animatable(
        if (selected_option == 0) {
            50f
        } else {
            15f
        }
    )
    private var _endCornerPercent = Animatable(
        if (selected_option == option_count - 1) {
            50f
        } else {
            15f
        }
    )

    private val animationSpec = tween<Float>(
        durationMillis = 150,
        easing = FastOutSlowInEasing,
    )

    override fun selectOption(scope: CoroutineScope, index: Int) {
        scope.launch {
            _selectedIndex.animateTo(
                targetValue = index.toFloat(),
                animationSpec = animationSpec,
            )
        }
        scope.launch {
            _startCornerPercent.animateTo(
                targetValue = if (index == 0) 50f else 15f,
                animationSpec = animationSpec,
            )
        }
        scope.launch {
            _endCornerPercent.animateTo(
                targetValue = if (index == option_count - 1) 50f else 15f,
                animationSpec = animationSpec,
            )
        }
    }
}

// 9
@Composable
fun rememberMultiSelectorState(
    option_count: Int,
    selected_option: Int,
) = remember {
    MultiSelectorStateImpl(
        option_count,
        selected_option,
    )
}

enum class MultiSelectorOption {
    Option,
    Background,
}

@Composable
fun MultiSelector(
    option_count: Int,
    selected_option: Int,
    modifier: Modifier = Modifier,
    selector_modifier: Modifier = Modifier,
    rounding: Int = 50,
    colour: Color = Color.Unspecified,
    background_colour: Color = Color.Unspecified,
    state: MultiSelectorState = rememberMultiSelectorState(
        option_count = option_count,
        selected_option = selected_option,
    ),
    on_selected: (Int) -> Unit,
    get_option: @Composable (Int) -> Unit
) {
    require(option_count >= 2) { "This composable requires at least 2 options" }

    LaunchedEffect(key1 = option_count, key2 = selected_option) {
        state.selectOption(this, selected_option)
    }

    Layout(
        modifier = modifier
            .clip(shape = RoundedCornerShape(percent = rounding))
            .background(if (background_colour == Color.Unspecified) MaterialTheme.colorScheme.background else background_colour)
            .border(Dp.Hairline, Color.Black, RoundedCornerShape(percent = rounding)),
        content = {

            for (i in 0 until option_count) {
                Box(
                    modifier = selector_modifier
                        .layoutId(MultiSelectorOption.Option)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { on_selected(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    get_option(i)
                }
                Box(
                    modifier = selector_modifier
                        .layoutId(MultiSelectorOption.Background)
                        .clip(
                            shape = RoundedCornerShape(
                                rounding
//                                topStartPercent = state.startCornerPercent,
//                                bottomStartPercent = state.startCornerPercent,
//                                topEndPercent = state.endCornerPercent,
//                                bottomEndPercent = state.endCornerPercent,
                            )
                        )
                        .background(if (colour == Color.Unspecified) MaterialTheme.colorScheme.primary else colour)
                )
            }
        }
    ) { measurables, constraints ->
        val optionWidth = constraints.maxWidth / option_count
        val optionConstraints = Constraints.fixed(
            width = optionWidth,
            height = constraints.maxHeight,
        )
        val optionPlaceables = measurables
            .filter { measurable -> measurable.layoutId == MultiSelectorOption.Option }
            .map { measurable -> measurable.measure(optionConstraints) }
        val backgroundPlaceable = measurables
            .first { measurable -> measurable.layoutId == MultiSelectorOption.Background }
            .measure(optionConstraints)
        layout(
            width = constraints.maxWidth,
            height = constraints.maxHeight,
        ) {
            // 4
            backgroundPlaceable.placeRelative(
                x = (state.selectedIndex * optionWidth).toInt(),
                y = 0,
            )
            optionPlaceables.forEachIndexed { index, placeable ->
                placeable.placeRelative(
                    x = optionWidth * index,
                    y = 0,
                )
            }
        }
    }
}