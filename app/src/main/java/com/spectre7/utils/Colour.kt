package com.spectre7.utils

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import java.util.*
import kotlin.math.absoluteValue

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

fun Color.amplify(by: Float): Color {
    return offsetRGB(if (isDark()) -by else by)
}

const val PALETTE_COLOUR_AMOUNT = 7

fun Color.compare(against: Color): Float {
    return ((red - against.red).absoluteValue + (green - against.green).absoluteValue + (blue - against.blue).absoluteValue) / 3f
}

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

fun Bitmap.getThemeColour(): Color? {
    val pixel_count = width * height

    val pixels = IntArray(pixel_count)
    getPixels(pixels, 0, width, 0, 0, width, height)

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

fun Color.isDark(): Boolean {
    return ColorUtils.calculateLuminance(toArgb()) < 0.5
}

fun Color.contrastAgainst(against: Color, by: Float = 0.5f): Color {
    return offsetRGB(if (against.isDark()) by else -by)
}

fun Color.getContrasted(): Color {
    if (isDark())
        return Color.White
    else
        return Color.Black
}

fun List<Color>.sorted(descending: Boolean = false): List<Color> {
    return if (descending) sortedByDescending { ColorUtils.calculateLuminance(it.toArgb()) }
            else sortedBy { ColorUtils.calculateLuminance(it.toArgb()) }
}

fun Color.generatePalette(size: Int, variance: Float = 0.2f): List<Color> {
    val rnd = Random()
    val ret: MutableList<Color> = mutableListOf()
    val luminance_boundary = 0.2f

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
        var colour: Color
        do {
            colour = offsetRGB(rnd.nextFloat() * variance * (if (rnd.nextBoolean()) 1f else -1f), false)
        }
        while (!isColourValid(colour))

        ret.add(colour)
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
