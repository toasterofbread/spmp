package com.spectre7.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import kotlin.math.min

fun setColourAlpha(colour: Color, alpha: Double): Color {
    return Color(ColorUtils.setAlphaComponent(colour.toArgb(), (255 * alpha).toInt()))
}

fun offsetColourRGB(colour: Color, offset: Double): Color {
    return Color(min(1f, colour.red + offset.toFloat()), min(1f, colour.green + offset.toFloat()), min(1f, colour.blue + offset.toFloat()))
}

fun getPaletteColour(palette: Palette, type: Int): Color? {
    val ret = Color(
        when (type) {
            0 -> palette.getDominantColor(Color.Unspecified.toArgb())
            1 -> palette.getVibrantColor(Color.Unspecified.toArgb())
            2 -> palette.getDarkVibrantColor(Color.Unspecified.toArgb())
            3 -> palette.getDarkMutedColor(Color.Unspecified.toArgb())
            4 -> palette.getLightVibrantColor(Color.Unspecified.toArgb())
            5 -> palette.getLightMutedColor(Color.Unspecified.toArgb())
            6 -> palette.getMutedColor(Color.Unspecified.toArgb())
            else -> throw RuntimeException("Invalid palette colour type '$type'")
        }
    )

    if (ret.toArgb() == Color.Unspecified.toArgb()) {
        return null
    }

    return ret
}

fun isColorDark(colour: Color): Boolean {
    return ColorUtils.calculateLuminance(colour.toArgb()) < 0.5;
}

fun getContrastedColour(colour: Color): Color {
    if (isColorDark(colour))
        return Color.White
    else
        return Color.Black
}
