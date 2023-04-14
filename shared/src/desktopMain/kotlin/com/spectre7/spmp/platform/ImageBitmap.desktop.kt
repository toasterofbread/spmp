package com.spectre7.spmp.platform

import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.Color
import org.jetbrains.skia.*
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.IOException
import javax.imageio.ImageIO

actual fun ByteArray.toImageBitmap(): ImageBitmap =
    Image.makeFromEncoded(this).toComposeImageBitmap()

actual fun ImageBitmap.crop(x: Int, y: Int, width: Int, height: Int): ImageBitmap =
    toAwtImage().getSubimage(x, y, width, height).toComposeImageBitmap()

actual fun ImageBitmap.getPixel(x: Int, y: Int): Color =
    Color(toAwtImage().getRGB(x, y))

actual fun ImageBitmap.scale(width: Int, height: Int): ImageBitmap {
    val scaled = toAwtImage().getScaledInstance(width, height, 0) as BufferedImage
    val stream = ByteArrayOutputStream()
    try {
        ImageIO.write(scaled, "png", stream)
        return Image.makeFromEncoded(stream.toByteArray()).toComposeImageBitmap()
    } catch (e: IOException) {
        e.printStackTrace()
    }
    return this
}
