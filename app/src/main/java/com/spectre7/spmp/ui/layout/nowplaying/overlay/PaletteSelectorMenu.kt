package com.spectre7.spmp.ui.layout.nowplaying.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import com.spectre7.utils.getPaletteColour

@Composable
fun PaletteSelectorMenu(palette: Palette?, requestColourPicker: ((Color?) -> Unit) -> Unit, onColourSelected: (Color) -> Unit) {
    if (palette != null) {
        var colourpick_requested by remember { mutableStateOf(false) }

        AnimatedVisibility(
            !colourpick_requested,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (i in 0 until 5) {
                        val colour = getPaletteColour(palette, i)
                        if (colour != null) {
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
                }

                Spacer(Modifier.height(50.dp))

                Button({
                    colourpick_requested = true
                    requestColourPicker {
                        colourpick_requested = false
                        if (it != null) {
                            onColourSelected(it)
                        }
                    }
                }) {
                    Text("Colour picker")
                }
            }
        }
    }
}
