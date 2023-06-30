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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spectre7.spmp.model.Settings
import com.spectre7.spmp.model.mediaitem.Song
import com.spectre7.spmp.platform.generatePalette
import com.spectre7.spmp.resources.getString
import com.spectre7.spmp.ui.layout.nowplaying.getNPBackground
import com.spectre7.spmp.ui.layout.nowplaying.getNPOnBackground
import com.spectre7.utils.composable.OnChangedEffect
import com.spectre7.utils.setAlpha
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
        getSong: () -> Song,
        getExpansion: () -> Float,
        openMenu: (OverlayMenu?) -> Unit,
        getSeekState: () -> Any,
        getCurrentSongThumb: () -> ImageBitmap?
    ) {
        val song = getSong()
        val thumb_image = getCurrentSongThumb()
        var palette_colours by remember { mutableStateOf<List<Color>?>(null) }
        var colourpick_requested by remember { mutableStateOf(false) }
        val button_colours = ButtonDefaults.buttonColors(
            containerColor = getNPBackground(),
            contentColor = getNPOnBackground()
        )

        LaunchedEffect(thumb_image) {
            palette_colours = null
            palette_colours = thumb_image?.generatePalette(8)
        }

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

                Row {
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
                        colors = button_colours
                    ) {
                        Text(getString("song_theme_menu_pick_colour_from_thumb"))
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val gradient_depth = getSong().song_reg_entry.np_gradient_depth ?: Settings.KEY_NOWPLAYING_DEFAULT_GRADIENT_DEPTH.get()
                    Text(
                        getString("song_theme_menu_gradient_depth_\$x")
                            .replace("\$x", gradient_depth.toString().padStart(3, ' ')),
                        Modifier.offset(y = 10.dp),
                        fontSize = 15.sp
                    )
                    val background_colour = getNPBackground()

                    Row {
                        Slider(
                            value = gradient_depth,
                            onValueChange = { value ->
                                getSong().song_reg_entry.np_gradient_depth =
                                    if (value == Settings.KEY_NOWPLAYING_DEFAULT_GRADIENT_DEPTH.get()) null else value
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = background_colour,
                                activeTrackColor = background_colour,
                                inactiveTrackColor = background_colour.setAlpha(0.2f)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton({
                            getSong().song_reg_entry.np_gradient_depth = null
                        }) {
                            Icon(Icons.Filled.Refresh, null)
                        }
                    }
                }

                var corner_slider_value by remember { mutableStateOf(
                    ((song.song_reg_entry.thumbnail_rounding ?: DEFAULT_THUMBNAIL_ROUNDING) - MIN_THUMBNAIL_ROUNDING) / (MAX_THUMBNAIL_ROUNDING - MIN_THUMBNAIL_ROUNDING).toFloat()
                ) }

                val anim_state = remember { Animatable(0f) }
                var anim_target: Float? by remember { mutableStateOf(null) }
                OnChangedEffect(anim_target) {
                    anim_state.animateTo(anim_target!!)
                }

                var value_change_count by remember { mutableStateOf(0) }
                OnChangedEffect(corner_slider_value) {
                    if (value_change_count > 1) {
                        anim_state.snapTo(corner_slider_value)
                    }
                }

                OnChangedEffect(anim_state.value) {
                    song.apply {
                        song_reg_entry.thumbnail_rounding = MIN_THUMBNAIL_ROUNDING + ((MAX_THUMBNAIL_ROUNDING - MIN_THUMBNAIL_ROUNDING) * anim_state.value).roundToInt()
                        if (!anim_state.isRunning) {
                            saveRegistry()
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val radius = (getSong().song_reg_entry.thumbnail_rounding ?: DEFAULT_THUMBNAIL_ROUNDING) * 2
                    Text(
                        getString("song_theme_menu_corner_radius_\$x")
                            .replace("\$x", radius.toString().padStart(3, ' ')),
                        Modifier.offset(y = 10.dp),
                        fontSize = 15.sp
                    )
                    val background_colour = getNPBackground()

                    Row {
                        Slider(
                            value = corner_slider_value,
                            onValueChange = {
                                corner_slider_value = it
                                value_change_count += 1
                            },
                            onValueChangeFinished = {
                                value_change_count = 0
                                anim_target = corner_slider_value
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = background_colour,
                                activeTrackColor = background_colour,
                                inactiveTrackColor = background_colour.setAlpha(0.2f)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton({
                            corner_slider_value = (DEFAULT_THUMBNAIL_ROUNDING - MIN_THUMBNAIL_ROUNDING) / (MAX_THUMBNAIL_ROUNDING - MIN_THUMBNAIL_ROUNDING).toFloat()
                            anim_target = corner_slider_value
                        }) {
                            Icon(Icons.Filled.Refresh, null)
                        }
                    }
                }

                val notif_image_menu_button_text = notifImageOverlayMenuButtonText()
                if (notif_image_menu_button_text != null) {
                    Button({ openMenu(NotifImageOverlayMenu()) }, colors = button_colours) {
                        Text(notif_image_menu_button_text)
                    }
                }
            }
        }
    }
}
