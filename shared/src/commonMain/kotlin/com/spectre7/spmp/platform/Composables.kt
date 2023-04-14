package com.spectre7.spmp.platform

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*

@Composable
expect fun BackHandler(enabled: Boolean = true, action: () -> Unit)

@Composable
expect fun PlatformDialog(
    onDismissRequest: () -> Unit,
    use_platform_default_width: Boolean = true,
    dim_behind: Boolean = true,
    content: @Composable () -> Unit
)

@Composable
expect fun PlatformAlertDialog(
//    onDismissRequest: () -> Unit,
//    confirmButton: @Composable () -> Unit,
//    modifier: Modifier = Modifier,
//    dismissButton: @Composable() (() -> Unit)? = null,
//    icon: @Composable() (() -> Unit)? = null,
//    title: @Composable() (() -> Unit)? = null,
//    text: @Composable() (() -> Unit)? = null,
//    shape: Shape = MaterialTheme.shapes.small,
//    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
//    iconContentColor: Color = LocalContentColor.current,
//    titleContentColor: Color = LocalContentColor.current,
//    textContentColor: Color = LocalContentColor.current
)

@Composable
expect fun SwipeRefresh(
    state: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    swipe_enabled: Boolean = true,
    content: @Composable () -> Unit
)
