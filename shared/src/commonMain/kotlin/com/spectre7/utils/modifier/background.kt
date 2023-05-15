package com.spectre7.utils.modifier

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline

fun Modifier.background(colourProvider: () -> Color) = drawBehind {
    drawRect(colourProvider())
}

fun Modifier.brushBackground(brushProvider: () -> Brush) = drawBehind {
    drawRect(brushProvider())
}

fun Modifier.background(shape: Shape, colourProvider: () -> Color) = drawBehind {
    drawOutline(shape.createOutline(size, layoutDirection, this), colourProvider())
}