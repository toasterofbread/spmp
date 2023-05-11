package com.spectre7.spmp.platform.composable

import androidx.compose.foundation.Indication
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.onClick
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerButton

@Composable
actual fun Modifier.platformClickable(onClick: () -> Unit, onAltClick: (() -> Unit)?, indication: Indication?): Modifier {
    val ret: Modifier = this.onClick(onClick = onClick)
    if (onAltClick == null) {
        return ret
    }
    return ret.onClick(
        matcher = PointerMatcher.mouse(PointerButton.Secondary),
        onClick = onAltClick
    )
}
