package com.spectre7.spmp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import com.spectre7.utils.getPaletteColour

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
