package com.toasterofbread.spmp.platform

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap

expect fun createImageBitmapUtil(): ImageBitmapUtil?

expect fun ByteArray.toImageBitmap(): ImageBitmap
expect fun ImageBitmap.toByteArray(): ByteArray
expect fun ImageBitmap.crop(x: Int, y: Int, width: Int, height: Int): ImageBitmap
expect fun ImageBitmap.getPixel(x: Int, y: Int): Color

interface ImageBitmapUtil {
    fun scale(image: ImageBitmap, width: Int, height: Int): ImageBitmap
    fun generatePalette(image: ImageBitmap, max_amount: Int): List<Color>
}
