@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
package com.toasterofbread.spmp.ui.theme

import PlatformTheme
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import dev.toastbits.composekit.platform.Platform
import dev.toastbits.composekit.settings.ui.Theme
import dev.toastbits.composekit.utils.common.amplifyPercent
import dev.toastbits.composekit.utils.common.blendWith
import dev.toastbits.composekit.utils.common.contrastAgainst
import dev.toastbits.composekit.utils.common.getContrasted
import dev.toastbits.composekit.utils.common.thenIf
import com.toasterofbread.spmp.platform.AppContext
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.AnimationSpec

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

fun Modifier.appHover(
    button: Boolean = false,
    expand: Boolean = false,
    hover_scale: Float = if (button) 0.95f else 0.97f,
    animation_spec: AnimationSpec<Float> = tween(100)
): Modifier = composed {
    val interaction_source: MutableInteractionSource = remember { MutableInteractionSource() }
    val hovered: Boolean by interaction_source.collectIsHoveredAsState()

    val actual_hover_scale: Float = if (expand) 2f - hover_scale else hover_scale
    val scale: Float by animateFloatAsState(
        if (hovered) actual_hover_scale else 1f,
        animationSpec = animation_spec
    )

    return@composed this
        .hoverable(interaction_source)
        .scale(scale)
        .thenIf(button) {
            pointerHoverIcon(PointerIcon.Hand)
        }
}
