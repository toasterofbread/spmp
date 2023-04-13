package com.spectre7.spmp.ui.layout.nowplaying.overlay

import android.graphics.Bitmap
import android.graphics.Color.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import com.spectre7.spmp.PlayerServiceHost
import com.spectre7.spmp.model.MediaItem
import com.spectre7.spmp.model.Song
import com.spectre7.spmp.ui.layout.PlayerViewContext
import com.spectre7.spmp.ui.layout.nowplaying.*
import com.spectre7.spmp.ui.layout.nowplaying.getNPBackground
import com.spectre7.spmp.ui.layout.nowplaying.getNPOnBackground
import com.spectre7.utils.*
import kotlin.concurrent.thread
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.random.Random

const val PALETTE_SIMILAR_COLOUR_THRESHOLD = 0.1f

class PaletteSelectorOverlayMenu(
    val paletteProvider: () -> Palette?,
    val defaultThemeColourProvider: () -> Color?,
    val requestColourPicker: ((Color?) -> Unit) -> Unit,
    val onColourSelected: (Color) -> Unit
): OverlayMenu() {

    override fun closeOnTap(): Boolean = true

    @Composable
    override fun Menu(
        songProvider: () -> Song,
        expansion: Float,
        openShutterMenu: (@Composable () -> Unit) -> Unit,
        close: () -> Unit,
        seek_state: Any,
        playerProvider: () -> PlayerViewContext
    ) {
        val palette = paletteProvider()
        if (palette != null) {

            val palette_colours = remember(palette) {
                val colours = mutableListOf(Color.Black, Color.White)

                fun addColour(colour: Color?) {
                    if (colour != null && !colours.any { it == colour || it.compare(colour) <= PALETTE_SIMILAR_COLOUR_THRESHOLD }) {
                        colours.add(colour)
                    }
                }

                for (i in 0 until 7) {
                    addColour(palette.getColour(i))
                }

                addColour(defaultThemeColourProvider())

                colours.sortByDescending { ColorUtils.calculateLuminance(it.toArgb()) }
                return@remember colours
            }
//            var palette_colours by remember { mutableStateOf<List<Color>?>(null) }
//            LaunchedEffect(Unit) {
//                val song = PlayerServiceHost.status.song!!
//                thread {
//                    val r = km(song.getThumbnail(MediaItem.ThumbnailQuality.HIGH)!!, 3)
//                    palette_colours = r
//
//
//                }
//            }

            var colourpick_requested by remember { mutableStateOf(false) }

            AnimatedVisibility(
                !colourpick_requested,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceEvenly, horizontalAlignment = Alignment.CenterHorizontally) {

                    val button_size = 40.dp
                    val button_spacing = 15.dp
                    LazyVerticalGrid(
                        GridCells.Adaptive(button_size + button_spacing),
                        Modifier.fillMaxWidth(0.75f),
                        horizontalArrangement = Arrangement.Center,
                        verticalArrangement = Arrangement.spacedBy(button_spacing)
                    ) {
                        items(palette_colours ?: emptyList()) { colour ->
                            Button(
                                onClick = {
                                    onColourSelected(colour)
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

                    Button(
                        {
                            colourpick_requested = true
                            requestColourPicker {
                                colourpick_requested = false
                                if (it != null) {
                                    onColourSelected(it)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = getNPBackground(playerProvider),
                            contentColor = getNPOnBackground(playerProvider)
                        )
                    ) {
                        Text("Pick from thumbnail")
                    }

                    var slider_value by remember { mutableStateOf(
                        ((songProvider().song_reg_entry.thumbnail_rounding ?: DEFAULT_THUMBNAIL_ROUNDING) - MIN_THUMBNAIL_ROUNDING) / (MAX_THUMBNAIL_ROUNDING - MIN_THUMBNAIL_ROUNDING).toFloat()
                    ) }

                    val anim_state = remember { Animatable(0f) }
                    var anim_target: Float? by remember { mutableStateOf(null) }
                    OnChangedEffect(anim_target) {
                        anim_state.animateTo(anim_target!!)
                    }

                    var value_change_count by remember { mutableStateOf(0) }
                    OnChangedEffect(slider_value) {
                        if (value_change_count > 1) {
                            anim_state.snapTo(slider_value)
                        }
                    }

                    OnChangedEffect(anim_state.value) {
                        songProvider().apply {
                            song_reg_entry.thumbnail_rounding = MIN_THUMBNAIL_ROUNDING + ((MAX_THUMBNAIL_ROUNDING - MIN_THUMBNAIL_ROUNDING) * anim_state.value).roundToInt()
                            if (!anim_state.isRunning) {
                                saveRegistry()
                            }
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val radius = (songProvider().song_reg_entry.thumbnail_rounding ?: DEFAULT_THUMBNAIL_ROUNDING) * 2
                        Text("Corner radius ${radius.toString().padStart(3, ' ')}", Modifier.offset(y = 10.dp), fontSize = 15.sp)
                        val background_colour = getNPBackground(playerProvider)

                        Row {
                            Slider(
                                value = slider_value,
                                onValueChange = {
                                    slider_value = it
                                    value_change_count += 1
                                },
                                onValueChangeFinished = {
                                    value_change_count = 0
                                    anim_target = slider_value
                                },
                                colors = SliderDefaults.colors(
                                    thumbColor = background_colour,
                                    activeTrackColor = background_colour,
                                    inactiveTrackColor = background_colour.setAlpha(0.2f)
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            IconButton({
                                slider_value = (DEFAULT_THUMBNAIL_ROUNDING - MIN_THUMBNAIL_ROUNDING) / (MAX_THUMBNAIL_ROUNDING - MIN_THUMBNAIL_ROUNDING).toFloat()
                                anim_target = slider_value
                            }) {
                                Icon(Icons.Filled.Refresh, null)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun km(image: Bitmap, k: Int): List<Color> {
    val scaled = Bitmap.createScaledBitmap(image, 50, 50, false)

    val pixels = IntArray(scaled.width * scaled.height)
    scaled.getPixels(pixels, 0, scaled.width, 0, 0, scaled.width, scaled.height)

    val centroids = IntArray(k) { pixels[(Random.nextDouble() * (pixels.size - 1)).toInt()] }
    val clusters: MutableList<MutableList<Int>> = List(k) { mutableListOf<Int>() }.toMutableList()


    var iterations = 0.01 * 10000

    while (iterations-- > 0) {
        println(iterations)

        for (cluster in clusters) {
            cluster.clear()
        }

        // Assign each pixel to closest cluster
        for (pixel in pixels.withIndex()) {
            var closest_diff: Int? = null
            var closest_centroid = -1

            for (centroid in centroids.withIndex()) {
                val diff = (pixel.value - centroid.value).absoluteValue
                if (closest_diff == null || diff < closest_diff) {
                    closest_diff = diff
                    closest_centroid = centroid.index
                }
            }

            clusters[closest_centroid].add(pixel.value)
        }

        // Recalculate cluster means
        for (cluster in clusters.withIndex()) {
            var r = 0L
            var g = 0L
            var b = 0L
            for (pixel in cluster.value) {
                r += red(pixel)
                g += green(pixel)
                b += blue(pixel)
            }
            centroids[cluster.index] = Color(
                (r / cluster.value.size.toLong()).toInt(),
                (g / cluster.value.size.toLong()).toInt(),
                (b / cluster.value.size.toLong()).toInt(),
            ).toArgb()
        }
    }

    return centroids.map { Color(it) }
}
