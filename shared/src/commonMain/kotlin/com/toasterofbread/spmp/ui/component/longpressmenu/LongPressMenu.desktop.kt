package com.toasterofbread.spmp.ui.component.longpressmenu

import LocalPlayerState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.toastbits.composekit.components.platform.composable.BackHandler
import dev.toastbits.composekit.util.blendWith
import dev.toastbits.composekit.util.contrastAgainst
import dev.toastbits.composekit.util.getContrasted
import dev.toastbits.composekit.util.platform.launchSingle
import dev.toastbits.composekit.util.composable.snapOrAnimateTo
import dev.toastbits.composekit.util.composable.OnChangedEffect
import dev.toastbits.composekit.components.utils.composable.ShapedIconButton
import dev.toastbits.composekit.components.utils.composable.getEnd
import dev.toastbits.composekit.components.utils.composable.getStart
import com.toasterofbread.spmp.model.mediaitem.db.rememberThemeColour
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.MINIMISED_NOW_PLAYING_HEIGHT_DP
import com.toasterofbread.spmp.ui.layout.contentbar.layoutslot.CustomColourSource
import com.toasterofbread.spmp.ui.layout.BarColourState
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import dev.toastbits.composekit.components.utils.composable.getBottom
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val MENU_OPEN_ANIM_MS: Int = 150
private const val MENU_CONTENT_PADDING_DP: Float = 25f
private const val MENU_WIDTH_DP: Float = 400f

@Composable
internal fun DesktopLongPressMenu(
    showing: Boolean,
    onDismissRequest: () -> Unit,
    data: LongPressMenuData
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val player: PlayerState = LocalPlayerState.current
        val density: Density = LocalDensity.current

        val coroutine_scope: CoroutineScope = rememberCoroutineScope()
        var show_dialog: Boolean by remember { mutableStateOf(showing) }
        var show_content: Boolean by remember{ mutableStateOf(false) }
        var show_background: Boolean by remember{ mutableStateOf(false) }

        var menu_width: Dp by remember { mutableStateOf(0.dp) }
        var menu_height: Dp by remember { mutableStateOf(0.dp) }

        fun Density.getTargetPosition(): Offset {
            val layout_offset: Offset = data.layout_offset ?: return Offset.Zero

            val left: Float = layout_offset.x + data.click_offset.x
            val right: Float = left + menu_width.toPx()
            val top: Float = layout_offset.y + data.click_offset.y
            val bottom: Float = top + menu_height.toPx()

            val max_width: Float = this@BoxWithConstraints.maxWidth.toPx()
            val max_height: Float = this@BoxWithConstraints.maxHeight.toPx()

            return Offset(
                if (left < 0) 0f
                else if (right > max_width) left - (right - max_width)
                else left,

                if (top < 0) 0f
                else if (bottom > max_height) top - (bottom - max_height)
                else top,
            )
        }

        val offset_x: Animatable<Float, AnimationVector1D> = remember { Animatable(density.getTargetPosition().x) }
        val offset_y: Animatable<Float, AnimationVector1D> = remember { Animatable(density.getTargetPosition().y) }

        fun close() {
            coroutine_scope.launchSingle {
                show_content = false
                show_background = false
                delay(MENU_OPEN_ANIM_MS.toLong())
                show_dialog = false
                onDismissRequest()
            }
        }

        val minimised_now_playing_height: Dp = MINIMISED_NOW_PLAYING_HEIGHT_DP.dp
        fun updatePosition(snap: Boolean = false) {
            coroutine_scope.launch {
                with (density) {
                    if (show_background) {
                        val target_position: Offset = density.getTargetPosition()
                        launch {
                            offset_x.snapOrAnimateTo(target_position.x, snap)
                        }
                        launch {
                            offset_y.snapOrAnimateTo(target_position.y, snap)
                        }
                    }
                    else {
                        val padding: Dp = 20.dp
                        launch {
                            val x: Dp = maxWidth - menu_width - padding
                            offset_x.snapOrAnimateTo(x.toPx(), snap)
                        }
                        launch {
                            val y: Dp = maxHeight - menu_height - padding - (if (player.session_started) minimised_now_playing_height else 0.dp)
                            offset_y.snapOrAnimateTo(y.toPx(), snap)
                        }
                    }
                }
            }
        }

        BackHandler(show_content && show_background) {
            close()
        }

        LaunchedEffect(showing) {
            if (!showing) {
                close()
            }
            else {
                show_dialog = true
                show_content = true
                show_background = true
            }
        }

        OnChangedEffect(data) {
            show_background = true
            updatePosition()
        }

        OnChangedEffect(menu_width, menu_height, player.session_started) {
            updatePosition()
        }

        if (show_dialog) {
            val fade_tween: FiniteAnimationSpec<Float> = tween(100)
            val keep_on_background_scroll: Boolean by player.settings.Behaviour.DESKTOP_LPM_KEEP_ON_BACKGROUND_SCROLL.observe()

            AnimatedVisibility(
                show_background,
                Modifier.fillMaxSize().zIndex(-1f),
                enter = fadeIn(fade_tween),
                exit = fadeOut(fade_tween)
            ) {

                LongPressMenuBackground(
                    Modifier.fillMaxSize(),
                    enable_input = show_background,
                    onScroll = {
                        if (keep_on_background_scroll) {
                            show_background = false
                            updatePosition()
                        }
                        else {
                            close()
                        }
                    }
                ) { close() }
            }

            AnimatedVisibility(
                show_content,
                Modifier.fillMaxSize(),
                enter = fadeIn(fade_tween),
                exit = fadeOut(fade_tween)
            ) {
                var accent_colour: Color? = data.item.rememberThemeColour()?.contrastAgainst(player.theme.background)

                DisposableEffect(Unit) {
                    val theme_colour = data.item.ThemeColour.get(player.database)
                    if (theme_colour != null) {
                        accent_colour = theme_colour
                    }

                    player.bar_colour_state.nav_bar.setLevelColour(CustomColourSource(player.theme.background), BarColourState.NavBarLevel.LPM)
                    onDispose {
                        player.bar_colour_state.nav_bar.setLevelColour(null, BarColourState.NavBarLevel.LPM)
                    }
                }

                val focus_requester: FocusRequester = remember { FocusRequester() }
                var focused: Boolean by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    focus_requester.requestFocus()
                }

                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopStart
                ) {
                    var prev_data: LongPressMenuData? by remember { mutableStateOf(null) }
                    LaunchedEffect(this@BoxWithConstraints.maxWidth, this@BoxWithConstraints.maxHeight, menu_width, menu_height) {
                        if (prev_data == data) {
                            updatePosition(true)
                        }
                        prev_data = data
                    }

                    val shape: RoundedCornerShape = RoundedCornerShape(5.dp)
                    Column(
                        Modifier
                            .offset {
                                IntOffset(
                                    offset_x.value.roundToInt(),
                                    offset_y.value.roundToInt()
                                )
                            }
                            .width(MENU_WIDTH_DP.dp)
                            .focusRequester(focus_requester)
                            .onFocusChanged {
                                if (focused && !it.hasFocus && show_background) {
                                    close()
                                }
                                focused = it.hasFocus
                            }
                            .onSizeChanged {
                                menu_width = with (density) { it.width.toDp() }
                                menu_height = with (density) { it.height.toDp() }
                            }
                            // Prevent click-through to backrgound
                            .clickable(
                                remember { MutableInteractionSource() },
                                null
                            ) {},
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val background_colour: Color = player.theme.accent.blendWith(player.theme.background, 0.1f)
                        val close_on_action: Boolean by player.settings.Behaviour.LPM_CLOSE_ON_ACTION.observe()

                        LongPressMenuContent(
                            data,
                            shape,
                            background_colour,
                            PaddingValues(
                                start = MENU_CONTENT_PADDING_DP.dp + WindowInsets.systemBars.getStart(),
                                end = MENU_CONTENT_PADDING_DP.dp + WindowInsets.systemBars.getEnd(),
                                top = MENU_CONTENT_PADDING_DP.dp,
                                bottom = MENU_CONTENT_PADDING_DP.dp + WindowInsets.systemBars.getBottom()
                            ),
                            { accent_colour },
                            modifier = Modifier.border(2.dp, player.theme.onBackground.copy(alpha = 0.1f), shape),
                            onAction = {
                                if (show_background && close_on_action) {
                                    close()
                                }
                            }
                        )

                        ShapedIconButton(
                            { close() },
                            IconButtonDefaults.iconButtonColors(
                                containerColor = background_colour,
                                contentColor = background_colour.getContrasted()
                            )
                        ) {
                            Icon(Icons.Default.Close, null)
                        }
                    }
                }
            }
        }
    }
}
