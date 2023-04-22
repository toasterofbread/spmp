package com.spectre7.spmp.platform

import android.view.WindowManager
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Indication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import coil.compose.rememberAsyncImagePainter
import com.google.accompanist.swiperefresh.SwipeRefresh as AccSwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

@OptIn(ExperimentalFoundationApi::class)
actual fun Modifier.platformClickable(onClick: () -> Unit, onAltClick: (() -> Unit)?, indication: Indication?): Modifier =
    composed { combinedClickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick,
        onLongClick = onAltClick
    ) }

@OptIn(ExperimentalMaterialApi::class)
actual fun Modifier.scrollWheelSwipeable(
    state: SwipeableState<Int>,
    anchors: Map<Float, Int>,
    thresholds: (from: Int, to: Int) -> ThresholdConfig,
    orientation: Orientation,
    reverseDirection: Boolean
): Modifier = swipeable(state = state, anchors = anchors, thresholds = thresholds, orientation = orientation, reverseDirection = reverseDirection)

@Composable
actual fun BackHandler(enabled: Boolean, action: () -> Unit) {
    androidx.activity.compose.BackHandler(enabled, action)
}

@Composable
actual fun PlatformDialog(
    onDismissRequest: () -> Unit,
    use_platform_default_width: Boolean,
    dim_behind: Boolean,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest,
        DialogProperties(usePlatformDefaultWidth = use_platform_default_width)
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

@Composable
actual fun SwipeRefresh(
    state: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier,
    swipe_enabled: Boolean,
    content: @Composable () -> Unit
) {
    AccSwipeRefresh(
        rememberSwipeRefreshState(state),
        onRefresh,
        modifier,
        swipe_enabled,
        content = content
    )
}

@Composable
actual fun rememberImagePainter(url: String): Painter = rememberAsyncImagePainter(url)
