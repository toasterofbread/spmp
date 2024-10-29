package com.toasterofbread.spmp.widget.configuration.enum

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import dev.toastbits.composekit.platform.composable.theme.LocalApplicationTheme

enum class WidgetSectionThemeMode {
    BACKGROUND, ACCENT, TRANSPARENT
}

val WidgetSectionThemeMode.colour: Color
    @Composable
    get() = with (LocalApplicationTheme.current) {
        when (this@colour) {
            WidgetSectionThemeMode.BACKGROUND -> background
            WidgetSectionThemeMode.ACCENT -> accent
            WidgetSectionThemeMode.TRANSPARENT -> Color.Transparent
        }
    }
