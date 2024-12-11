@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
package com.toasterofbread.spmp.ui.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import dev.toastbits.composekit.util.thenIf

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
