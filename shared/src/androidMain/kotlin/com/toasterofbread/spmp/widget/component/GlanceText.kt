package com.toasterofbread.spmp.widget.component

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.core.graphics.withClip
import androidx.core.util.TypedValueCompat.spToPx
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.layout.ContentScale
import androidx.glance.layout.wrapContentWidth
import com.toasterofbread.spmp.widget.mapper.toAndroidTypeface
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.FontResource

@Composable
internal fun GlanceText(
    text: String,
    modifier: GlanceModifier = GlanceModifier,
    font: Font? = null,
    font_size: TextUnit = 15.sp,
    colour: Color = LocalContentColor.current,
    max_width: Dp? = null
) {
    val context: Context = LocalContext.current
    val typeface: Typeface? = font?.toAndroidTypeface()

    val max_width_px: Int? = with (LocalDensity.current) {
        max_width?.roundToPx()
    }

    val image: Bitmap =
        remember(text, font_size, colour, typeface, max_width_px) {
            context.textAsBitmap(
                text = text,
                fontSize = font_size,
                color = colour,
                font = typeface,
                letterSpacing = 0.03.sp.value,
                maxWidth = max_width_px
            )
        } ?: return

    Image(
        modifier = modifier.wrapContentWidth(),
        provider = ImageProvider(image),
        contentDescription = text,
        contentScale = ContentScale.FillBounds
    )
}

// https://proandroiddev.com/jetpack-glance-no-way-to-custom-fonts-e761b789567d
private fun Context.textAsBitmap(
    text: String,
    fontSize: TextUnit,
    color: Color = Color.Black,
    letterSpacing: Float = 0.1f,
    font: Typeface? = null,
    maxWidth: Int? = null
): Bitmap? {
    val paint: TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    paint.textSize = spToPx(fontSize.value, this.resources.displayMetrics)
    paint.color = color.toArgb()
    paint.letterSpacing = letterSpacing
    paint.typeface = font ?: Typeface.DEFAULT

    val baseline: Float = -paint.ascent()
    val width: Int = paint.measureText(text).toInt()
    val height: Int = (baseline + paint.descent()).toInt()

    if (width <= 0 || height <= 0) {
        return null
    }

    val image: Bitmap = Bitmap.createBitmap(minOf(width, maxWidth ?: width), height, Bitmap.Config.ARGB_8888)
    val canvas: Canvas = Canvas(image)

    if (maxWidth == null || width <= maxWidth) {
        canvas.drawText(text, 0f, baseline, paint)
        return image
    }

    val ellipsis: String = "..."
    val ellipsisWidth: Float = paint.measureText(ellipsis)

    canvas.withClip(0f, 0f, maxWidth - ellipsisWidth, height.toFloat()) {
        drawText(text, 0f, baseline, paint)
    }

    canvas.drawText(ellipsis, maxWidth - ellipsisWidth, baseline, paint)

    return image
}
