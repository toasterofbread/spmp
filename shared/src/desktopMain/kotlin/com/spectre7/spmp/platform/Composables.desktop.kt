package com.spectre7.spmp.platform

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Indication
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.foundation.onClick
import androidx.compose.ui.input.pointer.PointerButton

@OptIn(ExperimentalFoundationApi::class)
@Composable
actual fun Modifier.platformClickable(onClick: () -> Unit, onAltClick: (() -> Unit)?, indication: Indication?): Modifier {
    var ret: Modifier = this.onClick(onClick = onClick)
    if (onAltClick != null) {
        ret = ret.onClick(
            matcher = PointerMatcher.mouse(PointerButton.Secondary),
            onClick = onAltClick
        )
    }
    return ret
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun BackHandler(enabled: Boolean, action: () -> Unit) {
    Box(Modifier.onKeyEvent { event ->
        if (event.key == Key.Escape) {
            action()
            true
        } else false
    })
}

@Composable
actual fun PlatformDialog(
    onDismissRequest: () -> Unit,
    use_platform_default_width: Boolean,
    dim_behind: Boolean,
    content: @Composable () -> Unit
) {
    TODO()
}

@Composable
actual fun PlatformAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier,
    dismissButton: @Composable() (() -> Unit)?,
    icon: @Composable() (() -> Unit)?,
    title: @Composable() (() -> Unit)?,
    text: @Composable() (() -> Unit)?,
    shape: Shape,
    containerColor: Color,
    iconContentColor: Color,
    titleContentColor: Color,
    textContentColor: Color
) {
    TODO()
}

@Composable
actual fun PlatformAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier,
    dismissButton: @Composable() (() -> Unit)?,
    icon: @Composable() (() -> Unit)?,
    title: @Composable() (() -> Unit)?,
    text: @Composable() (() -> Unit)?
) {
    TODO()
}

@Composable
actual fun SwipeRefresh(
    state: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier,
    swipe_enabled: Boolean,
    content: @Composable () -> Unit
) {
    content()
}