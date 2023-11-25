package com.toasterofbread.spmp.ui.theme

import PlatformTheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.toasterofbread.composekit.platform.Platform
import com.toasterofbread.composekit.settings.ui.Theme
import com.toasterofbread.composekit.utils.common.amplifyPercent
import com.toasterofbread.composekit.utils.common.blendWith
import com.toasterofbread.composekit.utils.common.contrastAgainst
import com.toasterofbread.composekit.utils.common.getContrasted
import com.toasterofbread.spmp.platform.AppContext

@Composable
fun Theme.ApplicationTheme(
    context: AppContext,
    font_family: FontFamily = FontFamily.Default,
    content: @Composable () -> Unit
) {
    val dark_theme: Boolean = isSystemInDarkTheme()

    val primary_container: Color = accent.blendWith(background, our_ratio = 0.2f).contrastAgainst(background, by = 0.1f)
    val secondary_container: Color = accent.blendWith(background, our_ratio = 0.6f).contrastAgainst(background, by = 0.1f)
    val tertiary_container: Color = accent.blendWith(background, our_ratio = 0.8f).contrastAgainst(background, by = 0.1f)

    val colour_scheme: ColorScheme =
        (if (dark_theme) context.getDarkColorScheme() else context.getLightColorScheme())
        .copy(
            primary = accent,
            onPrimary = on_accent,
            inversePrimary = vibrant_accent,
            secondary = on_background,
            onSecondary = background,
            tertiary = vibrant_accent,
            onTertiary = vibrant_accent.getContrasted(),

            primaryContainer = primary_container,
            onPrimaryContainer = primary_container.getContrasted(),
            secondaryContainer = secondary_container,
            onSecondaryContainer = secondary_container.getContrasted(),
            tertiaryContainer = tertiary_container,
            onTertiaryContainer = tertiary_container.getContrasted(),

            background = background,
            onBackground = on_background,

            surface = background.amplifyPercent(0.1f),
            onSurface = on_background,
            surfaceVariant = background.amplifyPercent(0.2f),
            onSurfaceVariant = on_background,
            surfaceTint = accent.blendWith(background, 0.75f),
            inverseSurface = vibrant_accent,
            inverseOnSurface = vibrant_accent.getContrasted(),

            outline = on_background,
            outlineVariant = vibrant_accent
        )

    val default_typography: Typography = MaterialTheme.typography
    val typography: Typography = remember(default_typography) {
        with(default_typography) {
            copy(
                displayLarge = displayLarge.copy(fontFamily = font_family),
                displayMedium = displayMedium.copy(fontFamily = font_family),
                displaySmall = displaySmall.copy(fontFamily = font_family),
                headlineLarge = headlineLarge.copy(fontFamily = font_family),
                headlineMedium = headlineMedium.copy(fontFamily = font_family),
                headlineSmall = headlineSmall.copy(fontFamily = font_family),
                titleLarge = titleLarge.copy(fontFamily = font_family),
                titleMedium = titleMedium.copy(fontFamily = font_family),
                titleSmall = titleSmall.copy(fontFamily = font_family),
                bodyLarge = bodyLarge.copy(fontFamily = font_family),
                bodyMedium = bodyMedium.copy(fontFamily = font_family),
                bodySmall = bodySmall.copy(fontFamily = font_family),
                labelLarge = labelLarge.copy(fontFamily = font_family),
                labelMedium = labelMedium.copy(fontFamily = font_family),
                labelSmall = labelSmall.copy(fontFamily = font_family)
            )
        }
    }

    val default_shapes: Shapes = MaterialTheme.shapes
    val shapes: Shapes = remember(default_shapes) {
        when (Platform.current) {
            Platform.DESKTOP ->
                default_shapes.copy(
                    extraSmall = RoundedCornerShape(2.dp),
                    small = RoundedCornerShape(4.dp),
                    medium = RoundedCornerShape(6.dp),
                    large = RoundedCornerShape(8.dp),
                    extraLarge = RoundedCornerShape(10.dp),
                )
            else -> default_shapes
        }
    }

    MaterialTheme(
        colorScheme = colour_scheme,
        typography = typography,
        shapes = shapes
    ) {
        PlatformTheme(this, content)
    }
}
