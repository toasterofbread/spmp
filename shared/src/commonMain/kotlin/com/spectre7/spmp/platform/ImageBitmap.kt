package com.toasterofbread.spmp.platform

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap

expect fun ByteArray.toImageBitmap(): ImageBitmap
expect fun ImageBitmap.toByteArray(): ByteArray

expect fun ImageBitmap.crop(x: Int, y: Int, width: Int, height: Int): ImageBitmap
expect fun ImageBitmap.getPixel(x: Int, y: Int): Color
expect fun ImageBitmap.scale(width: Int, height: Int): ImageBitmap
expect fun ImageBitmap.generatePalette(max_amount: Int): List<Color>