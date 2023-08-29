package com.toasterofbread.utils.modifier

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope

fun Modifier.background(getColour: () -> Color) = drawBehind {
    drawRect(getColour())
}

fun Modifier.drawScopeBackground(getColour: DrawScope.() -> Color) = drawBehind {
    drawRect(getColour())
}

fun Modifier.brushBackground(brushProvider: () -> Brush) = drawBehind {
    drawRect(brushProvider())
}

fun Modifier.background(shape: Shape, getColour: () -> Color) = drawBehind {
    drawOutline(shape.createOutline(size, layoutDirection, this), getColour())
}
