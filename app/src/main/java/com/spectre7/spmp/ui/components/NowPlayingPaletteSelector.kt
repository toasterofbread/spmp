package com.spectre7.spmp.ui.components

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
