package com.toasterofbread.spmp.ui.component.longpressmenu

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.model.mediaitem.MediaItemThumbnailProvider
import com.toasterofbread.spmp.model.mediaitem.Song
import com.toasterofbread.spmp.platform.composable.PlatformDialog
import com.toasterofbread.spmp.ui.layout.nowplaying.overlay.DEFAULT_THUMBNAIL_ROUNDING
import com.toasterofbread.spmp.ui.theme.Theme
import com.toasterofbread.utils.contrastAgainst
import kotlinx.coroutines.delay

private const val LONG_PRESS_ICON_MENU_OPEN_ANIM_MS: Int = 150
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
                Modifier.fillMaxSize(),
                // Can't find a way to disable Android Dialog's animations, or an alternative
                enter = EnterTransition.None,
                exit = slideOutVertically(tween(LONG_PRESS_ICON_MENU_OPEN_ANIM_MS)) { it }
            ) {
                var accent_colour: Color? by remember { mutableStateOf(null) }

                DisposableEffect(Unit) {
                    if (data.item is Song && data.item.theme_colour != null) {
                        accent_colour = data.item.theme_colour!!
                    }

                    SpMp.context.setNavigationBarColour(Theme.current.background)
                    onDispose {
                        SpMp.context.setNavigationBarColour(null)
                    }
                }

                val thumb_quality = MediaItemThumbnailProvider.Quality.LOW
                LaunchedEffect(data.item.isThumbnailLoaded(thumb_quality)) {
                    if (!data.item.isThumbnailLoaded(thumb_quality)) {
                        data.item.loadThumbnail(MediaItemThumbnailProvider.Quality.LOW)
                    }
                    else {
                        accent_colour = (data.item.getDefaultThemeColour() ?: Theme.current.background)
                            .contrastAgainst(Theme.current.background, 0.2f)
                    }
                }

                LongPressMenuContent(
                    data,
                    { accent_colour },
                    modifier,
                    { if (Settings.KEY_LPM_CLOSE_ON_ACTION.get()) close_requested = true },
                    { close_requested = true }
                )
            }
        }
    }
}
