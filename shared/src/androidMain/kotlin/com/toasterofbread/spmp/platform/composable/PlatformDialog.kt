package com.toasterofbread.spmp.platform.composable

import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider

@Composable
actual fun PlatformDialog(
    onDismissRequest: () -> Unit,
    use_platform_default_width: Boolean,
    dim_behind: Boolean,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest,
        DialogProperties(usePlatformDefaultWidth = use_platform_default_width, decorFitsSystemWindows = false)
    ) {
        if (!dim_behind) {
            val dialog = LocalView.current.parent as DialogWindowProvider
            dialog.window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }
        content()
    }
}

@Composable
actual fun PlatformAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier,
    dismissButton: @Composable (() -> Unit)?,
    icon: @Composable (() -> Unit)?,
    title: @Composable (() -> Unit)?,
    text: @Composable (() -> Unit)?,
    shape: Shape,
    containerColor: Color,
    iconContentColor: Color,
    titleContentColor: Color,
    textContentColor: Color
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest,
        confirmButton,
        modifier,
        dismissButton,
        icon,
        title,
        text,
        shape,
        containerColor,
        iconContentColor,
        titleContentColor,
        textContentColor
    )
}

@Composable
actual fun PlatformAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier,
    dismissButton: @Composable (() -> Unit)?,
    icon: @Composable (() -> Unit)?,
    title: @Composable (() -> Unit)?,
    text: @Composable (() -> Unit)?
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest,
        confirmButton,
        modifier,
        dismissButton,
        icon,
        title,
        text
    )
}
