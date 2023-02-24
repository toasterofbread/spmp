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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.model.Song
import com.spectre7.spmp.ui.layout.PlayerViewContext
import com.spectre7.spmp.ui.layout.nowplaying.*
import com.spectre7.spmp.ui.layout.nowplaying.getNPBackground
import com.spectre7.spmp.ui.layout.nowplaying.getNPOnBackground
import com.spectre7.utils.*
import kotlin.math.roundToInt

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

                for (i in 0 until PALETTE_COLOUR_AMOUNT) {
                    addColour(palette.getColour(i))
                }

                addColour(defaultThemeColourProvider())

                colours.sortByDescending { ColorUtils.calculateLuminance(it.toArgb()) }
                return@remember colours
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
                        items(palette_colours) { colour ->
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

                    val thumbnail_rounding_state: MutableState<Int?> = remember(songProvider().registry) { songProvider().registry.getState("thumbnail_rounding") }
                    var slider_value by remember { mutableStateOf(
                        ((thumbnail_rounding_state.value ?: DEFAULT_THUMBNAIL_ROUNDING) - MIN_THUMBNAIL_ROUNDING) / (MAX_THUMBNAIL_ROUNDING - MIN_THUMBNAIL_ROUNDING).toFloat()
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
                        thumbnail_rounding_state.value = MIN_THUMBNAIL_ROUNDING + ((MAX_THUMBNAIL_ROUNDING - MIN_THUMBNAIL_ROUNDING) * anim_state.value).roundToInt()

                        if (!anim_state.isRunning) {
                            songProvider().registry.save()
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val radius = (thumbnail_rounding_state.value ?: DEFAULT_THUMBNAIL_ROUNDING) * 2
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
