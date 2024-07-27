package com.toasterofbread.spmp.platform

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.IRect
import org.jetbrains.skia.Image

actual fun createImageBitmapUtil(): ImageBitmapUtil? = null

actual fun ByteArray.toImageBitmap(): ImageBitmap =
    Bitmap.makeFromImage(Image.makeFromEncoded(this)).asComposeImageBitmap()

actual fun ImageBitmap.toByteArray(): ByteArray =
    asSkiaBitmap().readPixels()!!

actual fun ImageBitmap.crop(x: Int, y: Int, width: Int, height: Int): ImageBitmap {
    val bitmap = Bitmap()
    asSkiaBitmap().extractSubset(bitmap, IRect.makeXYWH(x, y, width, height))
    return bitmap.asComposeImageBitmap()
}

actual fun ImageBitmap.getPixel(x: Int, y: Int): Color =
    Color(asSkiaBitmap().getColor(x, y))

