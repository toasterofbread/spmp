package com.spectre7.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.luminance
import java.util.*
import kotlin.math.absoluteValue

fun Color.Companion.red(argb: Int): Int = argb shr 16 and 0xFF
fun Color.Companion.green(argb: Int): Int = argb shr 8 and 0xFF
fun Color.Companion.blue(argb: Int): Int = argb and 0xFF

fun Color.Companion.random(randomise_alpha: Boolean = false, rnd: Random = Random()): Color {
    return Color(
        rnd.nextInt(256),
        rnd.nextInt(256),
        rnd.nextInt(256),
        if (randomise_alpha) rnd.nextInt(256) else 255
    )
}

fun Color.setAlpha(alpha: Float): Color {
    return copy(alpha = alpha)
}

fun Color.offsetRGB(offset: Float, clip: Boolean = true): Color {
    var f_offset = offset
    if (clip) {
        for (value in listOf(red, green, blue)) {
            val final = value + f_offset
            if (final > 1.0) {
                f_offset = 1f - value
            }
            else if (final < 0.0) {
                f_offset = -value
            }
        }
    }

    return Color(
        (red + f_offset).coerceIn(0f..1f),
        (green + f_offset).coerceIn(0f..1f),
        (blue + f_offset).coerceIn(0f..1f),
        alpha
    )
}

fun Color.amplify(by: Float, opposite: Float = by): Color {
    val offset = offsetRGB(if (isDark()) -by else by)
    if (compare(offset) < 0.9f) {
        return offset
    }
    return offsetRGB(if (isDark()) opposite else -opposite)
}

fun Color.amplifyPercent(by_percent: Float, opposite_percent: Float = by_percent, allow_reverse: Boolean = true): Color {
    val by = if (isDark()) by_percent else -by_percent
    val ret = if (by < 0f)
        Color(
            red * (1f + by),
            green * (1f + by),
            blue * (1f + by),
            alpha
        )
    else
        Color(
            red + ((1f - red) * by),
            green + ((1f - green) * by),
            blue + ((1f - blue) * by),
            alpha
        )

    if (allow_reverse && compare(ret) > 0.975f) {
        return amplifyPercent(-opposite_percent, allow_reverse = false)
    }

    return ret
}

fun Color.compare(against: Color): Float {
    return 1f - (((red - against.red).absoluteValue + (green - against.green).absoluteValue + (blue - against.blue).absoluteValue) / 3f)
}

fun ImageBitmap.getThemeColour(): Color? {
    val pixel_count = width * height

    val pixels = IntArray(pixel_count)
    readPixels(pixels, 0, 0, width, height)

    var light_count = 0
    var light_r = 0
    var light_g = 0
    var light_b = 0

    var dark_r = 0
    var dark_g = 0
    var dark_b = 0

    for (x in 0 until width) {
        for (y in 0 until height) {
            val colour = pixels[x + y * width]

            val r = (colour shr 16 and 0xFF) / 255f
            val g = (colour shr 8 and 0xFF) / 255f
            val b = (colour and 0xFF) / 255f

            if ((0.299 * r) + (0.587 * g) + (0.114 * b) >= 0.5) {
                light_count += 1
                light_r += (r * 255).toInt()
                light_g += (g * 255).toInt()
                light_b += (b * 255).toInt()
            }
            else {
                dark_r += (r * 255).toInt()
                dark_g += (g * 255).toInt()
                dark_b += (b * 255).toInt()
            }
        }
    }

    val dark_count = pixel_count - light_count
    if (dark_count == 0 && light_count == 0) {
        return null
    }

    if (light_count > dark_count) {
        return Color(
            light_r / light_count,
            light_g / light_count,
            light_b / light_count
        )
    }
    else {
        return Color(
            dark_r / dark_count,
            dark_g / dark_count,
            dark_b / dark_count
        )
    }
}

fun Color.isDark(): Boolean =
    luminance() < 0.2

fun Color.contrastAgainst(against: Color, by: Float = 0.5f): Color =
    offsetRGB(if (against.isDark()) by else -by)

fun Color.getContrasted(keep_alpha: Boolean = false): Color {
    val colour =
        if (isDark()) Color.White
        else Color.Black
    return if (keep_alpha) colour.copy(alpha = alpha) else colour
}

fun Color.getNeutral(): Color {
    if (isDark()) 
        return Color.Black
    else
        return Color.White
}

fun List<Color>.sorted(descending: Boolean = false): List<Color> {
    return if (descending) sortedByDescending { it.luminance() }
            else sortedBy { it.luminance() }
}

fun Color.generatePalette(size: Int, variance: Float = 0.2f): List<Color> {
    val rnd = Random()
    val ret: MutableList<Color> = mutableListOf()

    fun isColourValid(colour: Color): Boolean {
        if (ret.any { it.compare(colour) > 0.5f }) {
            return false
        }

        if (colour.compare(Color.Black) > 0.8f || colour.compare(Color.White) > 0.8f) {
            return false
        }

        return true
    }

    for (i in 0 until size) {
        var tries = 5
        while (tries-- > 0) {
            val colour = offsetRGB(rnd.nextFloat() * variance * (if (rnd.nextBoolean()) 1f else -1f), false)
            if (isColourValid(colour)) {
                ret.add(colour)
                break
            }
        }
    }

    return List(size) {
        offsetRGB(rnd.nextFloat() * variance * (if (rnd.nextBoolean()) 1f else -1f), false)
    }
}

fun Color.Companion.generatePalette(size: Int): List<Color> {
    val rnd = Random()
    return List(size) {
        random(rnd = rnd)
    }
}
