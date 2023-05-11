@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.spectre7.utils.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.minimumTouchTargetSize
import androidx.compose.material3.tokens.IconButtonTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role

@Composable
fun ShapedIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    content: @Composable () -> Unit,
) {
    Box(
        modifier =
        modifier
            .minimumTouchTargetSize()
            .size(IconButtonTokens.StateLayerSize)
            .background(color = colors.containerColor(enabled).value, shape = shape)
            .clickable(
                onClick = onClick,
                enabled = enabled,
                role = Role.Button,
                interactionSource = interactionSource,
                indication = rememberRipple(
                    bounded = false,
                    radius = IconButtonTokens.StateLayerSize / 2
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        val contentColor = colors.contentColor(enabled).value
        CompositionLocalProvider(LocalContentColor provides contentColor, content = content)
    }
}
