package com.spectre7.spmp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.core.view.ViewCompat
import com.spectre7.spmp.R

@Composable
fun ApplicationTheme(
    font_family: FontFamily = FontFamily.Default,
    content: @Composable () -> Unit
) {
    val colour_scheme = if (isSystemInDarkTheme()) dynamicDarkColorScheme(LocalContext.current) else dynamicLightColorScheme(LocalContext.current)
    val dark_theme = isSystemInDarkTheme()

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            (view.context as Activity).window.statusBarColor = colour_scheme.background.toArgb()
            ViewCompat.getWindowInsetsController(view)?.isAppearanceLightStatusBars = !dark_theme
        }
    }

    val default = Typography()

    MaterialTheme(
        colorScheme = colour_scheme,
        typography = Typography(
            displayLarge = default.displayLarge.copy(fontFamily = font_family),
            displayMedium = default.displayMedium.copy(fontFamily = font_family),
            displaySmall = default.displaySmall.copy(fontFamily = font_family),
            headlineLarge = default.headlineLarge.copy(fontFamily = font_family),
            headlineMedium = default.headlineMedium.copy(fontFamily = font_family),
            headlineSmall = default.headlineSmall.copy(fontFamily = font_family),
            titleLarge = default.titleLarge.copy(fontFamily = font_family),
            titleMedium = default.titleMedium.copy(fontFamily = font_family),
            titleSmall = default.titleSmall.copy(fontFamily = font_family),
            bodyLarge = default.bodyLarge.copy(fontFamily = font_family),
            bodyMedium = default.bodyMedium.copy(fontFamily = font_family),
            bodySmall = default.bodySmall.copy(fontFamily = font_family),
            labelLarge = default.labelLarge.copy(fontFamily = font_family),
            labelMedium = default.labelMedium.copy(fontFamily = font_family),
            labelSmall = default.labelSmall.copy(fontFamily = font_family)
        ),
        content = content
    )
}
