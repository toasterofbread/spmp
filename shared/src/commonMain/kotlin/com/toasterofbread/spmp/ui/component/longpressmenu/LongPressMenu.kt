package com.toasterofbread.spmp.ui.component.longpressmenu

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import dev.toastbits.composekit.util.platform.Platform
import com.toasterofbread.spmp.ui.layout.nowplaying.overlay.songtheme.DEFAULT_THUMBNAIL_ROUNDING

private const val LONG_PRESS_ICON_INDICATION_SCALE: Float = 0.4f

@Composable
fun Modifier.longPressMenuIcon(data: LongPressMenuData, enabled: Boolean = true): Modifier {
    val scale by animateFloatAsState(1f + (if (!enabled) 0f else data.getInteractionHintScale() * LONG_PRESS_ICON_INDICATION_SCALE))
    return this
        .clip(data.thumb_shape ?: RoundedCornerShape(DEFAULT_THUMBNAIL_ROUNDING))
        .scale(scale)
}

@Composable
fun LongPressMenu(
    showing: Boolean,
    onDismissRequest: () -> Unit,
    data: LongPressMenuData
) {
    when (Platform.current) {
        Platform.ANDROID -> AndroidLongPressMenu(showing, onDismissRequest, data)
        Platform.DESKTOP,
        Platform.WEB -> DesktopLongPressMenu(showing, onDismissRequest, data)
    }
}
