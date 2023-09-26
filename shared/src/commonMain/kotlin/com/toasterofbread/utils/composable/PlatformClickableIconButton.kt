package com.toasterofbread.utils.composable

import androidx.compose.foundation.Indication
import androidx.compose.foundation.layout.Box
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.minimumInteractiveComponentSize
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.platform.composable.platformClickable
import com.toasterofbread.utils.common.thenIf

@Composable
fun PlatformClickableIconButton(
    onClick: (() -> Unit)? = null,
    onAltClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    indication: Indication? = rememberRipple(bounded = false, radius = 24.dp),
    apply_minimum_size: Boolean = true,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .platformClickable(
                onClick = onClick,
                onAltClick = onAltClick,
                enabled = enabled,
                indication = indication
            )
            .thenIf(apply_minimum_size) {
                minimumInteractiveComponentSize()
            },
        contentAlignment = Alignment.Center
    ) {
        val contentAlpha = if (enabled) LocalContentAlpha.current else ContentAlpha.disabled
        CompositionLocalProvider(LocalContentAlpha provides contentAlpha, content = content)
    }
}