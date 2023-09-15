package com.toasterofbread.spmp.ui.component.longpressmenu

import LocalPlayerState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.model.mediaitem.db.rememberThemeColour
import com.toasterofbread.spmp.platform.getNavigationBarHeightDp
import com.toasterofbread.spmp.ui.layout.nowplaying.overlay.DEFAULT_THUMBNAIL_ROUNDING
import com.toasterofbread.spmp.ui.theme.Theme
import com.toasterofbread.utils.common.contrastAgainst
import kotlinx.coroutines.delay

private const val MENU_OPEN_ANIM_MS: Int = 150
private const val MENU_CONTENT_PADDING_DP: Float = 25f
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
    val player = LocalPlayerState.current

    var close_requested by remember { mutableStateOf(false) }
    var show_dialog by remember { mutableStateOf(showing) }
    var show_content by remember{ mutableStateOf(false) }

    suspend fun closePopup() {
        show_content = false
        delay(MENU_OPEN_ANIM_MS.toLong())
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
        AnimatedVisibility(
            show_content,
            Modifier
                .fillMaxWidth()
                .requiredHeight(
                    player.screen_size.height
                ),
            enter = fadeIn(tween(MENU_OPEN_ANIM_MS)),
            exit = fadeOut(tween(MENU_OPEN_ANIM_MS))
        ) {
            LongPressMenuBackground(
                Modifier,
                { close_requested = true }
            )
        }

        AnimatedVisibility(
            show_content,
            Modifier.fillMaxSize(),
            enter = slideInVertically(spring()) { it / 2 },
            exit = slideOutVertically(spring()) { it / 2 }
        ) {
            var accent_colour: Color? = data.item.rememberThemeColour()?.contrastAgainst(Theme.background)

            DisposableEffect(Unit) {
                val theme_colour = data.item.ThemeColour.get(player.database)
                if (theme_colour != null) {
                    accent_colour = theme_colour
                }

                player.onNavigationBarTargetColourChanged(Theme.background, true)
                onDispose {
                    player.onNavigationBarTargetColourChanged(null, true)
                }
            }

            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                LongPressMenuContent(
                    data,
                    PaddingValues(
                        start = MENU_CONTENT_PADDING_DP.dp,
                        end = MENU_CONTENT_PADDING_DP.dp,
                        top = MENU_CONTENT_PADDING_DP.dp,
                        bottom = MENU_CONTENT_PADDING_DP.dp + player.context.getNavigationBarHeightDp()
                    ),
                    { accent_colour },
                    Modifier
                        // Prevent click-through to backrgound
                        .clickable(
                            remember { MutableInteractionSource() },
                            null
                        ) {},
                    { if (Settings.KEY_LPM_CLOSE_ON_ACTION.get()) close_requested = true }
                )
            }
        }
    }
}
