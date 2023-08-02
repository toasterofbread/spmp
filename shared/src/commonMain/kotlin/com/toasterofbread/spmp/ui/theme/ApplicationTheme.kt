package com.toasterofbread.spmp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily
import com.toasterofbread.spmp.platform.PlatformContext

@Composable
fun ApplicationTheme(
    context: PlatformContext,
    font_family: FontFamily = FontFamily.Default,
    content: @Composable () -> Unit
) {
    val dark_theme = isSystemInDarkTheme()
    val colour_scheme = remember(dark_theme) {
        if (dark_theme) context.getDarkColorScheme() else context.getLightColorScheme()
    }

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
