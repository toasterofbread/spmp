package com.toasterofbread.spmp.widget.component.styledcolumn

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import com.toasterofbread.spmp.widget.component.GlanceCanvas
import com.toasterofbread.spmp.widget.configuration.enum.WidgetSectionThemeMode
import com.toasterofbread.spmp.widget.configuration.enum.WidgetStyledBorderMode
import com.toasterofbread.spmp.widget.configuration.enum.colour
import com.toasterofbread.spmp.widget.modifier.padding
import dev.toastbits.composekit.utils.common.getContrasted
import dev.toastbits.composekit.utils.composable.wave.wavePath

@Composable
internal fun GlanceStyledColumn(
    border_mode: WidgetStyledBorderMode,
    section_theme_modes: List<WidgetSectionThemeMode>,
    vararg content: @Composable ColumnScope.() -> Unit,
    modifier: GlanceModifier = GlanceModifier,
    vertical_alignment: Alignment.Vertical = Alignment.Top,
    spacing: Dp = 0.dp,
    content_padding: PaddingValues = PaddingValues(),
    getBackgroundColour: @Composable (WidgetSectionThemeMode) -> Color = { it.colour }
) {
    require(section_theme_modes.size == content.size)

    Column(modifier, verticalAlignment = vertical_alignment) {
        for ((index, part) in content.withIndex()) {
            val background_colour: Color = getBackgroundColour(section_theme_modes[index])

            Box(
                GlanceModifier
                    .background(background_colour)
                    .padding(content_padding)
                    .fillMaxWidth()
            ) {
                CompositionLocalProvider(LocalContentColor provides background_colour.getContrasted()) {
                    part()
                }
            }

            if (index + 1 != content.size) {
                val next_background_colour: Color = getBackgroundColour(section_theme_modes[index + 1])

                when (border_mode) {
                    WidgetStyledBorderMode.WAVE ->
                        ColumnWaveBorder(
                            spacing,
                            background_colour,
                            next_background_colour,
                            GlanceModifier.fillMaxWidth()
                        )
                    WidgetStyledBorderMode.NONE ->
                        Spacer(GlanceModifier.height(spacing))
                }
            }
        }
    }
}

@Composable
private fun ColumnWaveBorder(
    height: Dp,
    top_colour: Color,
    bottom_colour: Color,
    modifier: GlanceModifier = GlanceModifier
) {
    val size: DpSize = DpSize(LocalSize.current.width, height)

    // I spent over an hour trying to figure out why BlendMode doesn't behave as stated in the docs
    // Turns out that, in the docs, "source image" refers to the drawn image and "destination image" is the original image?
    // Surely "source image" should mean the original/base image? And the destination should mean the target/provided image?
    GlanceCanvas(size, modifier.height(height)) { image_size ->
        if (bottom_colour.alpha == 0f) {
            drawWave(
                9,
                image_size,
                Paint().apply {
                    color = top_colour
                    style = PaintingStyle.Fill
                },
                fill_direction = 1
            )
            return@GlanceCanvas
        }

        drawRect(
            0f,
            0f,
            image_size.width,
            image_size.height,
            Paint().apply {
                color = top_colour
            }
        )
        drawWave(
            9,
            image_size,
            Paint().apply {
                color = bottom_colour
                style = PaintingStyle.Fill

                blendMode =
                    if (top_colour.alpha == 0f) BlendMode.SrcOver
                    else BlendMode.SrcOver
            },
            fill_direction = -1
        )
    }
}

private fun Canvas.drawWave(
    waves: Int,
    wave_size: Size,
    paint: Paint,
    width_multiplier: Float = 1f,
    offset: Float = 0f,
    fill_direction: Int = 0
) {
    val path: Path = Path()
    wavePath(
        path = path,
        size = wave_size,
        waves = waves,
        width_multiplier = width_multiplier,
        offset = offset,
        fill_direction = fill_direction
    )
    drawPath(path, paint)
}
