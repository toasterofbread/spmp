package com.toasterofbread.spmp.widget.configuration.enum

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import dev.toastbits.composekit.platform.composable.theme.LocalApplicationTheme
import dev.toastbits.composekit.settings.ui.vibrant_accent
import dev.toastbits.composekit.utils.common.blendWith
import dev.toastbits.composekit.utils.common.thenIf
import kotlinx.serialization.Serializable

@Serializable
data class WidgetSectionTheme(
    val mode: Mode,
    val opacity: Float = DEFAULT_OPACITY
) {
    enum class Mode(val opacity_configurable: Boolean = true) {
        BACKGROUND, ACCENT, TRANSPARENT(opacity_configurable = false)
    }

    companion object {
        const val DEFAULT_OPACITY: Float = 1f
    }
}

val WidgetSectionTheme.colour: Color
    @Composable
    get() = mode.colour.thenIf(mode.opacity_configurable) { copy(alpha = opacity) }

val WidgetSectionTheme.Mode.colour: Color
    @Composable
    get() = with (LocalApplicationTheme.current) {
        when (this@colour) {
            WidgetSectionTheme.Mode.BACKGROUND -> background
            WidgetSectionTheme.Mode.ACCENT -> card.blendWith(accent, 0.2f)
            WidgetSectionTheme.Mode.TRANSPARENT -> Color.Transparent
        }
    }
