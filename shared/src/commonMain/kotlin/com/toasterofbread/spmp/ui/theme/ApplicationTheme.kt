package com.toasterofbread.spmp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import com.toasterofbread.spmp.platform.PlatformContext
import com.toasterofbread.utils.amplify
import com.toasterofbread.utils.amplifyPercent
import com.toasterofbread.utils.blendWith
import com.toasterofbread.utils.contrastAgainst
import com.toasterofbread.utils.getContrasted
import com.toasterofbread.utils.setAlpha

@Composable
fun Theme.ApplicationTheme(
    context: PlatformContext,
    font_family: FontFamily = FontFamily.Default,
    content: @Composable () -> Unit
) {
    val dark_theme = isSystemInDarkTheme()

    val primary_container = accent.blendWith(background, ratio = 0.2f).contrastAgainst(background, by = 0.1f)
    val secondary_container = accent.blendWith(background, ratio = 0.6f).contrastAgainst(background, by = 0.1f)
    val tertiary_container = accent.blendWith(background, ratio = 0.8f).contrastAgainst(background, by = 0.1f)

    val colour_scheme =
        (if (dark_theme) context.getDarkColorScheme() else context.getLightColorScheme())
        .copy(
            primary = on_background,
            onPrimary = background,
            inversePrimary = accent,
            secondary = on_background,
            onSecondary = background,
            tertiary = on_background,
            onTertiary = background,

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
            surfaceTint = accent,
            inverseSurface = vibrant_accent,
            inverseOnSurface = vibrant_accent.getContrasted(),

            outline = on_background,
            outlineVariant = vibrant_accent
//            error: Color = this.error,
//            onError: Color = this.onError,
//            errorContainer: Color = this.errorContainer,
//            onErrorContainer: Color = this.onErrorContainer,
//            scrim: Color = this.scrim
        )

    val default_typography = MaterialTheme.typography
    val typography = remember(default_typography) {
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

    MaterialTheme(
        colorScheme = colour_scheme,
        typography = typography,
        content = content
    )
}
