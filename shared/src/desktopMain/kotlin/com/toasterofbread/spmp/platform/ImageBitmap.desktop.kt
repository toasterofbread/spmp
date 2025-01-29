package com.toasterofbread.spmp.platform

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.graphics.toComposeImageBitmap
import de.androidpit.colorthief.ColorThief
import org.jetbrains.skia.Image
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.IOException
import javax.imageio.ImageIO
import dev.toastbits.composekit.util.sortedByHue

actual fun createImageBitmapUtil(): ImageBitmapUtil? = DesktopImageBitmapUtil()

class DesktopImageBitmapUtil(): ImageBitmapUtil {
    override fun scale(image: ImageBitmap, width: Int, height: Int): ImageBitmap =
        image.scale(width, height)

    override fun generatePalette(image: ImageBitmap, max_amount: Int): List<Color> =
        image.generatePalette(max_amount)
}

actual fun ByteArray.toImageBitmap(): ImageBitmap =
    Image.makeFromEncoded(this).toComposeImageBitmap()

actual fun ImageBitmap.toByteArray(): ByteArray {
    val stream = ByteArrayOutputStream()
    ImageIO.write(this.toAwtImage(), "PNG", stream)
    return stream.toByteArray()
}

actual fun ImageBitmap.crop(x: Int, y: Int, width: Int, height: Int): ImageBitmap =
    toAwtImage().getSubimage(x, y, width, height).toComposeImageBitmap()

actual fun ImageBitmap.getPixel(x: Int, y: Int): Color =
    Color(toAwtImage().getRGB(x, y))

fun ImageBitmap.toScaledBufferedImage(width: Int, height: Int): BufferedImage {
    val scaled: java.awt.Image = toAwtImage().getScaledInstance(width, height, 0)

    val buffered: BufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    buffered.createGraphics().apply {
        drawImage(scaled, 0, 0, null)
        dispose()
    }

    return buffered
}

fun ImageBitmap.scale(width: Int, height: Int): ImageBitmap {
    val scaled = toScaledBufferedImage(width, height)
    val stream = ByteArrayOutputStream()
    try {
        ImageIO.write(scaled, "png", stream)
        return Image.makeFromEncoded(stream.toByteArray()).toComposeImageBitmap()
    } catch (e: IOException) {
        e.printStackTrace()
    }
    return this
}

fun ImageBitmap.generatePalette(max_amount: Int): List<Color> {
    require(max_amount in 2..256)

    val scaled = toScaledBufferedImage(50, 50)
    val palette = ColorThief.getColorMap(scaled, max_amount, 10, false).palette()
    return palette.map { Color(it[0], it[1], it[2]) }.sortedByHue()
}
