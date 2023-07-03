package com.toasterofbread.spmp.ui.component.longpressmenu

import LocalPlayerState
import SpMp
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.model.*
import com.toasterofbread.spmp.model.mediaitem.*
import com.toasterofbread.spmp.platform.composable.PlatformAlertDialog
import com.toasterofbread.spmp.platform.composable.PlatformDialog
import com.toasterofbread.spmp.platform.composable.platformClickable
import com.toasterofbread.spmp.platform.vibrateShort
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.layout.artistpage.ArtistSubscribeButton
import com.toasterofbread.spmp.ui.layout.nowplaying.overlay.DEFAULT_THUMBNAIL_ROUNDING
import com.toasterofbread.spmp.ui.theme.Theme
import com.toasterofbread.utils.composable.Marquee
import com.toasterofbread.utils.composable.NoRipple
import com.toasterofbread.utils.contrastAgainst
import com.toasterofbread.utils.getContrasted
import com.toasterofbread.utils.setAlpha
import com.toasterofbread.utils.thenIf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val LONG_PRESS_ICON_MENU_OPEN_ANIM_MS: Int = 150
private const val MENU_ITEM_SPACING: Int = 20
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
    data: LongPressMenuData,
    modifier: Modifier = Modifier
) {
    var close_requested by remember { mutableStateOf(false) }
    var show_dialog by remember { mutableStateOf(showing) }
    var show_content by remember { mutableStateOf(true) }

    suspend fun closePopup() {
        show_content = false
        delay(LONG_PRESS_ICON_MENU_OPEN_ANIM_MS.toLong())
        show_dialog = false
        onDismissRequest()
    }

    LaunchedEffect(showing, close_requested) {
        if (!showing || close_requested) {
            closePopup()
            close_requested = false
        }
        else {
            show_dialog = true
            show_content = true
        }
    }

    if (show_dialog) {
        PlatformDialog(
            onDismissRequest = { close_requested = true },
            use_platform_default_width = false,
            dim_behind = false
        ) {
            AnimatedVisibility(
                show_content,
                // Can't find a way to disable Android Dialog's animations, or an alternative
                enter = EnterTransition.None,
                exit = slideOutVertically(tween(LONG_PRESS_ICON_MENU_OPEN_ANIM_MS)) { it }
            ) {
                val accent_colour: MutableState<Color?> = remember { mutableStateOf(null) }

                LaunchedEffect(Unit) {
                    if (data.item is Song && data.item.theme_colour != null) {
                        accent_colour.value = data.item.theme_colour!!
                    }
                }

                val thumb_quality = MediaItemThumbnailProvider.Quality.LOW
                LaunchedEffect(data.item.isThumbnailLoaded(thumb_quality)) {
                    if (!data.item.isThumbnailLoaded(thumb_quality)) {
                        data.item.loadThumbnail(MediaItemThumbnailProvider.Quality.LOW)
                    }
                    else {
                        accent_colour.value = (data.item.getDefaultThemeColour() ?: Theme.current.background)
                            .contrastAgainst(Theme.current.background, 0.2f)
                    }
                }

                LongPressMenuContent(
                    data,
                    accent_colour,
                    modifier,
                    { if (Settings.KEY_LPM_CLOSE_ON_ACTION.get()) close_requested = true },
                    { close_requested = true }
                )
            }
        }
    }
}
