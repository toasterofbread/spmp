package com.toasterofbread.spmp.ui.component.longpressmenu

import LocalPlayerState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FiniteAnimationSpec
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.toastbits.composekit.platform.composable.BackHandler
import dev.toastbits.composekit.utils.common.contrastAgainst
import dev.toastbits.composekit.utils.common.launchSingle
import dev.toastbits.composekit.utils.composable.getBottom
import dev.toastbits.composekit.utils.composable.getEnd
import dev.toastbits.composekit.utils.composable.getStart
import com.toasterofbread.spmp.model.mediaitem.db.rememberThemeColour
import com.toasterofbread.spmp.model.settings.category.BehaviourSettings
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.BarColourState
import com.toasterofbread.spmp.ui.layout.contentbar.layoutslot.CustomColourSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay

private const val MENU_OPEN_ANIM_MS: Int = 150
private const val MENU_CONTENT_PADDING_DP: Float = 25f

@Composable
internal fun AndroidLongPressMenu(
    showing: Boolean,
    onDismissRequest: () -> Unit,
    data: LongPressMenuData
) {
    val player: PlayerState = LocalPlayerState.current

    val coroutine_scope: CoroutineScope = rememberCoroutineScope()
    var show_dialog: Boolean by remember { mutableStateOf(showing) }
    var show_content: Boolean by remember{ mutableStateOf(false) }

    fun close() {
        coroutine_scope.launchSingle {
            show_content = false
            delay(MENU_OPEN_ANIM_MS.toLong())
            show_dialog = false
            onDismissRequest()
        }
    }

    BackHandler(show_content) {
        close()
    }

    LaunchedEffect(showing) {
        if (!showing) {
            close()
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
            LongPressMenuBackground {
                close()
            }
        }

        val slide_spring: FiniteAnimationSpec<IntOffset> = spring()
        AnimatedVisibility(
            show_content,
            Modifier.fillMaxSize(),
            enter = slideInVertically(slide_spring) { it / 2 },
            exit = slideOutVertically(slide_spring) { it / 2 }
        ) {
            var accent_colour: Color? = data.item.rememberThemeColour()?.contrastAgainst(player.theme.background)

            DisposableEffect(Unit) {
                val theme_colour = data.item.ThemeColour.get(player.database)
                if (theme_colour != null) {
                    accent_colour = theme_colour.contrastAgainst(player.theme.background)
                }

                player.bar_colour_state.nav_bar.setLevelColour(CustomColourSource(player.theme.background), BarColourState.NavBarLevel.LPM)

                onDispose {
                    player.bar_colour_state.nav_bar.setLevelColour(null, BarColourState.NavBarLevel.LPM)
                }
            }

            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                LongPressMenuContent(
                    data,
                    RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    player.theme.background,
                    PaddingValues(
                        start = MENU_CONTENT_PADDING_DP.dp + WindowInsets.systemBars.getStart(),
                        end = MENU_CONTENT_PADDING_DP.dp + WindowInsets.systemBars.getEnd(),
                        top = MENU_CONTENT_PADDING_DP.dp,
                        bottom = MENU_CONTENT_PADDING_DP.dp + WindowInsets.systemBars.getBottom()
                    ),
                    { accent_colour },
                    Modifier
                        // Prevent click-through to backrgound
                        .clickable(
                            remember { MutableInteractionSource() },
                            null
                        ) {},
                    {
                        if (BehaviourSettings.Key.LPM_CLOSE_ON_ACTION.get()) {
                            close()
                        }
                    }
                )
            }
        }
    }
}
