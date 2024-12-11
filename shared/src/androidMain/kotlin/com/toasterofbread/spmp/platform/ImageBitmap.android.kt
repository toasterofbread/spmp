package com.toasterofbread.spmp.platform

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.palette.graphics.Palette
import java.io.ByteArrayOutputStream
import dev.toastbits.composekit.util.sortedByHue

actual fun createImageBitmapUtil(): ImageBitmapUtil? = AndroidImageBitmapUtil()

class AndroidImageBitmapUtil(): ImageBitmapUtil {
    override fun scale(image: ImageBitmap, width: Int, height: Int): ImageBitmap =
        image.scale(width, height)

    override fun generatePalette(image: ImageBitmap, max_amount: Int): List<Color> =
        image.generatePalette(max_amount)
}

actual fun ByteArray.toImageBitmap(): ImageBitmap =
    BitmapFactory.decodeByteArray(this, 0, size).asImageBitmap()

actual fun ImageBitmap.toByteArray(): ByteArray {
    val stream = ByteArrayOutputStream()
    asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, stream)
    return stream.toByteArray()
}

actual fun ImageBitmap.crop(x: Int, y: Int, width: Int, height: Int): ImageBitmap =
    Bitmap.createBitmap(this.asAndroidBitmap(), x, y, width, height).asImageBitmap()

actual fun ImageBitmap.getPixel(x: Int, y: Int): Color =
    Color(asAndroidBitmap().getPixel(x, y))

fun ImageBitmap.scale(width: Int, height: Int): ImageBitmap =
    Bitmap.createScaledBitmap(this.asAndroidBitmap(), width, height, false).asImageBitmap()

fun Palette.getColour(type: Int): Color? {
    val colour = when (type) {
        0 -> getVibrantColor(0)
        1 -> getLightVibrantColor(0)
        2 -> getLightMutedColor(0)
        3 -> getDarkVibrantColor(0)
        4 -> getDarkMutedColor(0)
        5 -> getDominantColor(0)
        6 -> getMutedColor(0)
        else -> throw RuntimeException("Invalid palette colour type $type")
    }

    if (colour == 0) {
        return null
    }

    return Color(colour)
}

fun ImageBitmap.generatePalette(max_amount: Int): List<Color> {
    require(max_amount >= 0)

    val palette = Palette.from(this.asAndroidBitmap()).clearFilters().generate()

    val colours: MutableList<Color> = mutableListOf()
    for (i in 0 until minOf(max_amount, 7)) {
        val colour = palette.getColour(i)
        if (colour != null) {
            colours.add(colour)
        }
    }

    return colours.sortedByHue()
}
