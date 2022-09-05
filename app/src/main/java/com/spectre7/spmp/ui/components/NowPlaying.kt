package com.spectre7.spmp.ui.components

import android.util.Log
import android.graphics.drawable.VectorDrawable
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import com.github.krottv.compose.sliders.DefaultThumb
import com.github.krottv.compose.sliders.DefaultTrack
import com.github.krottv.compose.sliders.SliderValueHorizontal
import com.google.android.exoplayer2.Player
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.R
import com.spectre7.spmp.ui.layout.PlayerStatus
import java.lang.RuntimeException
import kotlin.concurrent.thread

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlaying(expanded: Boolean, p_status: PlayerStatus, background_colour: Animatable<Color, AnimationVector4D>) {

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
            }
            else {

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

    AnimatedVisibility(
        visible = !expanded,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        LinearProgressIndicator(
            progress = p_status.position,
            color = on_background_colour.value,
            trackColor = setColourAlpha(on_background_colour.value, 0.5),
            modifier = Modifier
                .requiredHeight(2.dp)
                .fillMaxWidth()
        )
    }

    val menu_visible = remember { MutableTransitionState(false) }

    if (menu_visible.targetState || menu_visible.currentState) {
        Popup(
            alignment = Alignment.Center,
            properties = PopupProperties(
                excludeFromSystemGesture = false,
                focusable = true
            ),
            onDismissRequest = { menu_visible.targetState = false },
            offset = IntOffset(0, -60)
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                AnimatedVisibility(
                    visibleState = menu_visible,
                    enter = expandHorizontally(tween(150)) + slideInVertically(
                        initialOffsetY = { it / 8 }),
                    exit = shrinkHorizontally(tween(150)) + slideOutVertically(
                        targetOffsetY = { it / 8 })
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.9f).fillMaxHeight(0.85f),
                        colors = CardDefaults.cardColors(
                            MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.onBackground
                        )
                    ) {

                    }
                }
            }
        }
    }

    Box(Modifier.padding(if (expanded) 25.dp else 10.dp)) {

        Column(verticalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxHeight()) {

            if (expanded) {
                Spacer(Modifier.requiredHeight(50.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {

                var thumb_menu_visible by remember { mutableStateOf(false) }
                LaunchedEffect(expanded) {
                    thumb_menu_visible = false
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
                                        enabled = expanded,
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() }
                                    ) {
                                        if (expanded) {
                                            thumb_menu_visible = !thumb_menu_visible
                                        }
                                    }
                            )
                        }
                    }

                    // Thumbnail overlay menu
                    androidx.compose.animation.AnimatedVisibility(thumb_menu_visible, enter = fadeIn(), exit = fadeOut()) {
                        Box(Modifier.background(setColourAlpha(Color.DarkGray, 0.85), shape = RoundedCornerShape(5))) {
                            Column(Modifier.fillMaxSize()) {
                                PaletteSelector(theme_palette) { index, _ ->
                                    palette_index = index
                                }
                            }
                        }
                    }

                }

                AnimatedVisibility(visible = !expanded, enter = slideInHorizontally { -500 }, exit = ExitTransition.None) {
                    Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            getSongTitle(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .animateContentSize()
                                .clipToBounds()
                        )

                        IconButton(onClick = {
                            MainActivity.player.interact {
                                it.playPause()
                            }
                        }, modifier = Modifier.align(Alignment.CenterVertically)) {
                            Image(
                                painterResource(if (p_status.playing) R.drawable.ic_pause else R.drawable.ic_play_arrow),
                                MainActivity.getString(if (p_status.playing) R.string.media_pause else R.string.media_play),
                                colorFilter = colour_filter
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = expanded, enter = fadeIn(), exit = fadeOut()) {

                val button_size = 60.dp

                @Composable
                fun PlayerButton(painter: Painter, size: Dp = button_size, alpha: Float = 1f, c_filter: ColorFilter = colour_filter, label: String? = null, on_click: () -> Unit) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.clickable(
                            onClick = on_click,
                            indication = rememberRipple(radius = 25.dp, bounded = false),
                            interactionSource = remember { MutableInteractionSource() }
                        )
                    ) {
                        Image(
                            painter, "",
                            Modifier
                                .requiredSize(size, button_size)
                                .offset(y = if (label != null) (-7).dp else 0.dp),
                            colorFilter = c_filter,
                            alpha = alpha
                        )

                        if (label != null) {
                            Text(label, color = on_background_colour.value, fontSize = 10.sp, modifier = Modifier.offset(y = (10).dp))
                        }
                    }
                }

                @Composable
                fun PlayerButton(image_id: Int, size: Dp = button_size, alpha: Float = 1f, c_filter: ColorFilter = colour_filter, label: String? = null, on_click: () -> Unit) {
                    PlayerButton(painterResource(image_id), size, alpha, c_filter, label, on_click)
                }

                Column(verticalArrangement = Arrangement.spacedBy(35.dp)) {
                    Spacer(Modifier.weight(1f))

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
                        modifier = Modifier.fillMaxWidth(),
                    ) {

                        val utility_separation = 25.dp

                        PlayerButton(R.drawable.ic_shuffle, button_size * 0.65f, if (p_status.shuffle) 1f else 0.25f) { MainActivity.player.interact {
                            it.player.shuffleModeEnabled = !it.player.shuffleModeEnabled
                        } }

                        Spacer(Modifier.requiredWidth(utility_separation))

                        PlayerButton(R.drawable.ic_skip_previous) {
                            MainActivity.player.interact { it.player.seekToPreviousMediaItem() }
                        }

                        PlayerButton(if (p_status.playing) R.drawable.ic_pause else R.drawable.ic_play_arrow) {
                            MainActivity.player.interact { it.playPause() }
                        }

                        PlayerButton(R.drawable.ic_skip_next) {
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

                    Spacer(Modifier.weight(1f))
                    val spacing = 55.dp

                    Row(
                        Modifier
//                            .background(MaterialTheme.colorScheme.background, CircleShape)
                            .requiredHeight(button_size * 0.8f)
                            .padding(start = spacing, end = spacing)
                            .align(Alignment.CenterHorizontally),
                        horizontalArrangement = Arrangement.spacedBy(spacing)
                    ) {
                        PlayerButton(rememberVectorPainter(Icons.Filled.Edit), button_size * 0.4f, c_filter = ColorFilter.tint(on_background_colour.value), label = "Edit") {

                        }
                        PlayerButton(rememberVectorPainter(Icons.Filled.Menu), button_size * 0.4f, c_filter = ColorFilter.tint(on_background_colour.value), label = "Lyrics") {
                            menu_visible.targetState = !menu_visible.targetState
                        }
                        PlayerButton(rememberVectorPainter(Icons.Filled.Email), button_size * 0.4f, c_filter = ColorFilter.tint(on_background_colour.value), label = "Label") {

                        }
                    }


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
