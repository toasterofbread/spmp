package com.toasterofbread.spmp.widget.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider

@Composable
fun GlanceCanvas(size: DpSize, modifier: GlanceModifier, draw: @Composable Canvas.(Size) -> Unit) {
    val image: ImageBitmap =
        with (LocalDensity.current) {
            ImageBitmap(size.width.roundToPx(), size.height.roundToPx())
        }

    val canvas: Canvas = Canvas(image)
    draw(canvas, Size(image.width.toFloat(), image.height.toFloat()))

    Image(
        ImageProvider(image.asAndroidBitmap()),
        null,
        modifier
    )
}
