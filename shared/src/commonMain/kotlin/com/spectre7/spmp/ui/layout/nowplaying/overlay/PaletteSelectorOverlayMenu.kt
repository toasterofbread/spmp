package com.spectre7.spmp.ui.layout.nowplaying.overlay

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spectre7.spmp.PlayerServiceHost
import com.spectre7.spmp.model.MediaItem
import com.spectre7.spmp.model.Song
import com.spectre7.spmp.platform.generatePalette
import com.spectre7.spmp.ui.layout.PlayerViewContext
import com.spectre7.spmp.ui.layout.nowplaying.*
import com.spectre7.utils.*
import com.spectre7.utils.composable.OnChangedEffect
import kotlin.concurrent.thread
import kotlin.math.roundToInt

//const val PALETTE_SIMILAR_COLOUR_THRESHOLD = 0.1f
const val DEFAULT_THUMBNAIL_ROUNDING: Int = 5
const val MIN_THUMBNAIL_ROUNDING: Int = 0
const val MAX_THUMBNAIL_ROUNDING: Int = 50

class PaletteSelectorOverlayMenu(
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
//        val palette_colours = remember(palette) {
//            val colours = mutableListOf(Color.Black, Color.White)
//
//            fun addColour(colour: Color?) {
//                if (colour != null && !colours.any { it == colour || it.compare(colour) <= PALETTE_SIMILAR_COLOUR_THRESHOLD }) {
//                    colours.add(colour)
//                }
//            }
//
//            for (i in 0 until 7) {
//                addColour(palette.getColour(i))
//            }
//
//            addColour(defaultThemeColourProvider())
//
//            colours.sortByDescending { it.luminance()) }
//            return@remember colours
//        }
        var palette_colours by remember { mutableStateOf<List<Color>?>(null) }
        LaunchedEffect(Unit) {
            val song = PlayerServiceHost.status.song!!
            thread {
                val image = song.loadAndGetThumbnail(MediaItem.ThumbnailQuality.HIGH)!!
                palette_colours = image.generatePalette(8)
            }
        }

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

//private fun km(image: ImageBitmap, k: Int): List<Color> {
//    // Scale the image down to reduce the number of pixels to process
//    val scaled = image.scale(50, 50)
//
//    // Get the pixel values from the scaled image
//    val pixels = IntArray(scaled.width * scaled.height)
//    scaled.readPixels(pixels, 0, scaled.width, 0, 0, scaled.width, scaled.height)
//
//    // Convert the pixel values to Color objects
//    val colors = pixels.map { Color(it) }
//
//    // Initialize the k-means algorithm
//    val centroids = colors.shuffled().take(k).toMutableList()
//    var prevCentroids: List<Color>
//
//    do {
//        // Assign each color to its nearest centroid
//        val clusters = colors.groupBy { color ->
//            centroids.minByOrNull { centroid -> color.distanceTo(centroid) }!!
//        }
//
//        // Update the centroids based on the mean of the colors in each cluster
//        prevCentroids = centroids.toList()
//        centroids.clear()
//        for ((centroid, cluster) in clusters) {
//            val mean = Color(
//                red = cluster.map { it.red }.average().toInt(),
//                green = cluster.map { it.green }.average().toInt(),
//                blue = cluster.map { it.blue }.average().toInt(),
//                alpha = cluster.map { it.alpha }.average().toInt()
//            )
//            centroids.add(mean)
//        }
//    } while (centroids != prevCentroids)
//
//    // Return the final centroids as the dominant colors
//    return centroids
//}
//
