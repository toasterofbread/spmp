package com.toasterofbread.spmp.widget.configuration.enum

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import dev.toastbits.composekit.platform.composable.theme.LocalApplicationTheme
import dev.toastbits.composekit.settings.ui.vibrant_accent

enum class WidgetSectionThemeMode {
    BACKGROUND, ACCENT, TRANSPARENT
}

val WidgetSectionThemeMode.colour: Color
    @Composable
    get() = with (LocalApplicationTheme.current) {
        when (this@colour) {
            WidgetSectionThemeMode.BACKGROUND -> background
            WidgetSectionThemeMode.ACCENT -> vibrant_accent
            WidgetSectionThemeMode.TRANSPARENT -> Color.Transparent
        }
    }
