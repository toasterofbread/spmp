package com.spectre7.spmp.ui.components

import android.R
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.model.Song
import java.lang.RuntimeException
import kotlin.concurrent.thread

fun getImage(id: Int): ImageBitmap {
    return BitmapFactory.decodeResource(MainActivity.instance!!.resources, id).asImageBitmap()
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

val default_image = getImage(R.drawable.ic_menu_gallery)

@Composable
fun NowPlaying(expanded: Boolean, p_song: Song?, p_playing: Boolean, p_position: Float, theme_colour: Animatable<Color, AnimationVector4D>) {

    fun getSongTitle(): String {
        if (p_song == null) {
            return "-----"
        }
        return p_song.nativeData!!.title
    }

    fun getSongArtist(): String {
        if (p_song == null) {
            return ""
        }
        return p_song.artist.nativeData.name
    }

    val default_colour = MaterialTheme.colorScheme.secondaryContainer
    var thumbnail by remember { mutableStateOf(default_image) }
    var theme_palette by remember { mutableStateOf<Palette?>(null) }
    var palette_index by remember { mutableStateOf(2) }

    fun setThumbnail(thumb: ImageBitmap?) {
        if (thumb == null) {
            thumbnail = default_image
            theme_palette = null
            return
        }

        thumbnail = thumb
        Palette.from(thumbnail.asAndroidBitmap()).generate {
            theme_palette = it
        }
    }

    LaunchedEffect(p_song?.getId()) {
        if (p_song == null) {
            setThumbnail(null)
        }
        else if (p_song.thumbnailLoaded(true)) {
            setThumbnail(p_song.loadThumbnail(true).asImageBitmap())
        }
        else {
            thread {
                setThumbnail(p_song.loadThumbnail(true).asImageBitmap())
            }
        }
    }

    LaunchedEffect(key1 = theme_palette, key2 = palette_index) {
        if (theme_palette == null) {
            theme_colour.animateTo(default_colour)
        }
        else {
            theme_colour.animateTo(getPaletteColour(theme_palette!!, palette_index) ?: default_colour)
        }
    }

    LinearProgressIndicator(progress = p_position, modifier = Modifier
        .requiredHeight(2.dp)
        .fillMaxWidth())

    Box(Modifier.padding(if (expanded) 30.dp else 10.dp)) {

        Column(verticalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxHeight()) {

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {

                var thumb_menu_visible by remember { mutableStateOf(false) }
                LaunchedEffect(expanded) {
                    thumb_menu_visible = false
                }

                Box(Modifier.aspectRatio(1f)) {
                    Crossfade(thumbnail, animationSpec = tween(250)) { image ->
                        Image(
                            image, "",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(if (expanded) 5 else 20))
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

                    // Thumbnail overlay menu
                    androidx.compose.animation.AnimatedVisibility(thumb_menu_visible, enter = fadeIn(), exit = fadeOut()) {
                        Box(Modifier.background(Color(ColorUtils.setAlphaComponent(Color.Gray.toArgb(), (255 * 0.5f).toInt())))) {
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
                            MainActivity.instance!!.player.interact {
                                it.playPause()
                            }
                        }, modifier = Modifier.align(Alignment.CenterVertically)) {
                            Image(
                                painterResource(if (p_playing) R.drawable.ic_media_pause else R.drawable.ic_media_play),
                                MainActivity.getString(if (p_playing) com.spectre7.spmp.R.string.media_pause else com.spectre7.spmp.R.string.media_play)
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = expanded, enter = fadeIn(), exit = fadeOut()) {

                Column() {

                    // Title text
                    Text(getSongTitle(),
                        fontSize = 17.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize())

                    // Artist text
                    Text(getSongArtist(),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize())

                    var slider_moving by remember { mutableStateOf(false) }
                    var slider_value by remember { mutableStateOf(0.0f) }
                    var old_p_position by remember { mutableStateOf<Float?>(null) }

                    LaunchedEffect(p_position) {
                        if (!slider_moving && p_position != old_p_position) {
                            slider_value = p_position
                            old_p_position = null
                        }
                    }

                    Slider(
                        value = slider_value,
                        onValueChange = { slider_moving = true; slider_value = it },
                        onValueChangeFinished = {
                            slider_moving = false
                            old_p_position = p_position
                            MainActivity.instance!!.player.interact {
                                it.player.seekTo((it.player.duration * slider_value).toLong())
                            }
                        }
                    )

                    Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {

                        val button_size = 75.dp
                        val button_ripple = rememberRipple(radius = 35.dp)

                        // Previous
                        Image(
                            getImage(R.drawable.ic_media_previous), "",
                            Modifier
                                .requiredSize(button_size)
                                .clickable(
                                    onClick = { MainActivity.instance!!.player.interact { it.player.seekToPreviousMediaItem() } },
                                    indication = button_ripple,
                                    interactionSource = remember { MutableInteractionSource() }
                                )
                        )

                        // Pause / play
                        Image(
                            getImage(if (p_playing) R.drawable.ic_media_pause else R.drawable.ic_media_play), "",
                            Modifier
                                .requiredSize(button_size)
                                .clickable(
                                    onClick = { MainActivity.instance!!.player.interact { it.playPause() }; },
                                    indication = button_ripple,
                                    interactionSource = remember { MutableInteractionSource() }
                                )
                        )

                        // Next
                        Image(
                            getImage(R.drawable.ic_media_next), "",
                            Modifier
                                .requiredSize(button_size)
                                .clickable(
                                    onClick = { MainActivity.instance!!.player.interact { it.player.seekToNextMediaItem() } },
                                    indication = button_ripple,
                                    interactionSource = remember { MutableInteractionSource() }
                                )
                        )
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
