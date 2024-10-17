package com.toasterofbread.spmp.widget.component

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import androidx.annotation.FontRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import androidx.core.util.TypedValueCompat.spToPx
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.layout.ContentScale
import androidx.glance.layout.wrapContentWidth
import androidx.glance.text.Text
import dev.toastbits.composekit.platform.composable.theme.LocalApplicationTheme

@Composable
fun GlanceText(
    text: String,
    @FontRes
    font: Int?,
    modifier: GlanceModifier = GlanceModifier,
    font_size: TextUnit = 15.sp,
    alpha: Float = 1f
) {
    val context: Context = LocalContext.current
    val colour: Color = LocalApplicationTheme.current.on_background.copy(alpha)

    val image: Bitmap =
        remember(text, font_size, colour, font) {
            context.textAsBitmap(
                text = text,
                fontSize = font_size,
                color = colour,
                font = font,
                letterSpacing = 0.1.sp.value
            )
        } ?: return

    Image(
        modifier = modifier.wrapContentWidth(),
        provider = ImageProvider(image),
        contentDescription = text
    )
}

// https://proandroiddev.com/jetpack-glance-no-way-to-custom-fonts-e761b789567d
private fun Context.textAsBitmap(
    text: String,
    fontSize: TextUnit,
    color: Color = Color.Black,
    letterSpacing: Float = 0.1f,
    @FontRes
    font: Int?
): Bitmap? {
    val paint: TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    paint.textSize = spToPx(fontSize.value, this.resources.displayMetrics)
    paint.color = color.toArgb()
    paint.letterSpacing = letterSpacing
    paint.typeface =
        font?.let { ResourcesCompat.getFont(this, font) }
            ?: Typeface.DEFAULT

    val baseline: Float = -paint.ascent()
    val width: Int = (paint.measureText(text)).toInt()
    val height: Int = (baseline + paint.descent()).toInt()

    if (width <= 0 || height <= 0) {
        return null
    }

    val image: Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas: Canvas = Canvas(image)
    canvas.drawText(text, 0f, baseline, paint)
    return image
}
