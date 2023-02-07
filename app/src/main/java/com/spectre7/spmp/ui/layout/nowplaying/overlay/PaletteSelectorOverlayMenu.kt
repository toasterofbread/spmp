package com.spectre7.spmp.ui.layout.nowplaying.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.model.Song
import com.spectre7.spmp.ui.layout.PlayerViewContext
import com.spectre7.utils.PALETTE_COLOUR_AMOUNT
import com.spectre7.utils.compare
import com.spectre7.utils.getColour

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
        song: Song,
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
                    val button_spacing = 30.dp
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
                            containerColor = MainActivity.theme.getBackground(true),
                            contentColor = MainActivity.theme.getOnBackground(true)
                        )
                    ) {
                        Text("Pick from thumbnail")
                    }
                }
            }
        }
    }
}
