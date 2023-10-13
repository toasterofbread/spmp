@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.toasterofbread.utils.composable

import LocalPlayerState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Indication
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.tokens.IconButtonTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import com.toasterofbread.spmp.ui.theme.Theme
import com.toasterofbread.utils.common.setAlpha

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShapedIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    colours: IconButtonColors = IconButtonDefaults.iconButtonColors(
        containerColor = LocalPlayerState.current.theme.accent,
        contentColor = LocalPlayerState.current.theme.on_accent,
        disabledContainerColor = LocalPlayerState.current.theme.accent.setAlpha(0.5f)
    ),
    indication: Indication? = rememberRipple(
        bounded = false,
        radius = IconButtonTokens.StateLayerSize / 2
    ),
    onLongClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Box(
        modifier =
        modifier
            .size(IconButtonTokens.StateLayerSize)
            .background(color = colours.containerColor(enabled).value, shape = shape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                enabled = enabled,
                role = Role.Button,
                interactionSource = interactionSource,
                indication = indication
            ),
        contentAlignment = Alignment.Center
    ) {
        val contentColor = colours.contentColor(enabled).value
        CompositionLocalProvider(LocalContentColor provides contentColor, content = content)
    }
}
