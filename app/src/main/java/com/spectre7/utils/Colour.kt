package com.spectre7.utils

import android.annotation.SuppressLint
import android.graphics.Bitmap
import androidx.compose.animation.Animatable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import kotlin.math.absoluteValue

fun Color.setAlpha(alpha: Double): Color {
    return setColourAlpha(this, alpha)
}

fun setColourAlpha(colour: Color, alpha: Double): Color {
    return colour.copy(alpha = alpha.toFloat())
}

fun offsetColourRGB(colour: Color, offset: Double, clip: Boolean = true): Color {
    var final_offset = offset.toFloat()
    if (clip) {
        for (value in listOf(colour.red, colour.green, colour.blue)) {
            val final = value + final_offset
            if (final > 1.0) {
                final_offset = 1f - value
            }
            else if (final < 0.0) {
                final_offset = -value
            }
        }
    }

    return Color(
        (colour.red + final_offset).coerceIn(0f..1f),
        (colour.green + final_offset).coerceIn(0f..1f),
        (colour.blue + final_offset).coerceIn(0f..1f),
        colour.alpha
    )
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
    return isColorDark(this)
}

fun isColorDark(colour: Color): Boolean {
    return ColorUtils.calculateLuminance(colour.toArgb()) < 0.5
}

fun Color.contrastAgainst(against: Color, by: Double = 0.5): Color {
    return offsetColourRGB(this, if (against.isDark()) by else -by)
}

fun Color.getContrasted(): Color {
    return getContrastedColour(this)
}

fun getContrastedColour(colour: Color): Color {
    if (isColorDark(colour))
        return Color.White
    else
        return Color.Black
}

data class Theme private constructor(
    private val t_background: Animatable<Color, AnimationVector4D>,
    private val t_on_background: Animatable<Color, AnimationVector4D>,

    private val n_background: Animatable<Color, AnimationVector4D>,
    private val n_on_background: Animatable<Color, AnimationVector4D>,

    private val accent: Animatable<Color, AnimationVector4D>,

    // TODO | Proper access methods for default colours
    var default_t_background: Color,
    var default_t_on_background: Color,
    var default_n_background: Color,
    var default_n_on_background: Color,
    var default_accent: Color,
) {

    suspend fun reset() {
        t_background.snapTo(default_t_background)
        t_on_background.snapTo(default_t_on_background)
        n_background.snapTo(default_n_background)
        n_on_background.snapTo(default_n_on_background)
        accent.snapTo(default_accent)
    }

    suspend fun setBackground(themed: Boolean, value: Color?) {
        (if (themed) t_background else n_background).animateTo(value ?: if (themed) default_t_background else default_n_background)
    }

    suspend fun setOnBackground(themed: Boolean, value: Color?) {
        (if (themed) t_on_background else n_on_background).animateTo(value ?: if (themed) default_t_on_background else default_n_on_background)
    }

    suspend fun setAccent(value: Color?) {
        accent.animateTo(value ?: default_accent)
    }

    fun getBackground(themed: Boolean): Color {
        return if (themed) t_background.value else n_background.value
    }

    fun getOnBackground(themed: Boolean): Color {
        return if (themed) t_on_background.value else n_on_background.value
    }

    fun getAccent(): Color {
        return accent.value
    }

    fun getOnAccent(): Color {
        return accent.value.getContrasted()
    }

    fun getVibrantAccent(): Color {
        return accent.value.contrastAgainst(n_background.value)
    }

    fun getBackgroundProvider(themed: Boolean): () -> Color {
        return { if (themed) t_background.value else n_background.value }
    }

    fun getOnBackgroundProvider(themed: Boolean): () -> Color {
        return { if (themed) t_on_background.value else n_on_background.value }
    }

    fun getAccentProvider(): () -> Color {
        return { accent.value }
    }

    fun getOnAccentProvider(): () -> Color {
        return { accent.value.getContrasted() }
    }

    fun getVibrantAccentProvider(): () -> Color {
        return { accent.value.contrastAgainst(n_on_background.value) }
    }

    companion object {

        @Composable
        fun default(): Theme {
            return create(Color.Black, Color.White, Color(99, 54, 143))
        }

        @SuppressLint("UnrememberedAnimatable")
        @Composable
        fun create(background: Color, on_background: Color, accent: Color): Theme {
            return Theme(
                Animatable(background),
                Animatable(on_background),
                Animatable(background),
                Animatable(on_background),
                Animatable(accent),
                background,
                on_background,
                background,
                on_background,
                accent
            )
        }
    }
}
