package com.spectre7.spmp.ui.component

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.spectre7.spmp.MainActivity
import com.spectre7.utils.NoRipple
import com.spectre7.utils.Theme

class PillMenuActionGetter {
    val background_colour: Color
    val content_colour: Color
    val close: () -> Unit

    constructor(
        background_colour: Color,
        content_colour: Color,
        close: () -> Unit
    ) {
        this.background_colour = background_colour
        this.content_colour = content_colour
        this.close = close
    }

    constructor(
        theme: Theme,
        themed: Boolean = false
    ) {
        this.background_colour = theme.getBackground(themed)
        this.content_colour = theme.getBackground(themed)
        this.close = {}
    }

    @Composable
    fun ActionButton(icon: ImageVector, action: () -> Unit) {
        IconButton(onClick = {
            action()
            close()
        }) {
            Icon(icon, null, tint = content_colour)
        }
    }
}

private data class PillMenuParams(
    val vertical: Boolean,
    val top: Boolean,
    val left: Boolean,
    val alignment: Alignment,
    val open_icon: ImageVector,
    val close_icon: ImageVector,
    val enter: EnterTransition,
    val exit: ExitTransition,
    val action_count: Int,
    val expand_state: MutableState<Boolean>?
)

@Composable
fun PillMenu(
    action_count: Int,
    getAction: @Composable PillMenuActionGetter.(i: Int, action_count: Int) -> Unit,
    expand_state: MutableState<Boolean>?,
    background_colour: Color,
    content_colour: Color,
    top: Boolean = false,
    left: Boolean = false,
    vertical: Boolean = false,
    container_modifier: Modifier = Modifier.fillMaxSize(),
    modifier: Modifier = Modifier
) {

    val params = remember(top, left, vertical, action_count, expand_state != null) {
        val alignment: Alignment
        val open_icon: ImageVector
        val close_icon: ImageVector
        val enter: EnterTransition
        val exit: ExitTransition

        val tween = tween<IntSize>(250)
        if (vertical) {
            if (top) {
                open_icon = Icons.Filled.KeyboardArrowDown
                close_icon = Icons.Filled.KeyboardArrowUp

                enter = expandVertically(tween, Alignment.Top)
                exit = shrinkVertically(tween, Alignment.Top)
            }
            else {
                open_icon = Icons.Filled.KeyboardArrowUp
                close_icon = Icons.Filled.KeyboardArrowDown

                enter = expandVertically(tween, Alignment.Bottom)
                exit = shrinkVertically(tween, Alignment.Bottom)
            }
        }
        else {
            if (left) {
                open_icon = Icons.Filled.KeyboardArrowRight
                close_icon = Icons.Filled.KeyboardArrowLeft

                enter = expandHorizontally(tween, Alignment.Start)
                exit = shrinkHorizontally(tween, Alignment.Start)
            }
            else {
                open_icon = Icons.Filled.KeyboardArrowLeft
                close_icon = Icons.Filled.KeyboardArrowRight

                enter = expandHorizontally(tween, Alignment.End)
                exit = shrinkHorizontally(tween, Alignment.End)
            }
        }

        if (top) {
            if (left) {
                alignment = Alignment.TopStart
            }
            else {
                alignment = Alignment.TopEnd
            }
        }
        else {
            if (left) {
                alignment = Alignment.BottomStart
            }
            else {
                alignment = Alignment.BottomEnd
            }
        }

        PillMenuParams(vertical, top, left, alignment, open_icon, close_icon, enter, exit, action_count, expand_state)
    }

    InnerPillMenu(params, content_colour, background_colour, modifier, container_modifier, getAction)
}

@Composable
private fun InnerPillMenu(
    params: PillMenuParams,
    content_colour: Color,
    background_colour: Color,
    modifier: Modifier,
    container_modifier: Modifier,
    getAction: @Composable PillMenuActionGetter.(Int, Int) -> Unit
) {
    Crossfade(params, Modifier.zIndex(1f)) {
        val (vertical, top, left, alignment, open_icon, close_icon, enter, exit, action_count, expand_state) = it
        val align_start = (vertical && top) || (!vertical && left)

        Box(
            contentAlignment = alignment,
            modifier = container_modifier.padding(15.dp)
        ) {

            if (expand_state != null) {
                NoRipple {
                    IconButton(
                        onClick = { expand_state.value = !expand_state.value },
                        modifier = Modifier.background(background_colour, shape = CircleShape)
                    ) {
                        Icon(open_icon, "", tint = content_colour)
                    }
                }
            }

            AnimatedVisibility(
                visible = expand_state?.value ?: true,
                enter = enter,
                exit = exit
            ) {

                @Composable
                fun closeButton() {
                    if (expand_state != null) {
                        NoRipple {
                            IconButton(onClick = { expand_state.value = false }) {
                                Icon(close_icon, "", tint = content_colour)
                            }
                        }
                    }
                }

                @Composable
                fun content() {
                    if (align_start) {
                        closeButton()
                    }

                    val getter = PillMenuActionGetter(background_colour, content_colour) { expand_state?.value = false }
                    for (i in 0 until action_count) {
                        getAction(getter, i, action_count)
                    }

                    if (!align_start) {
                        closeButton()
                    }
                }

                if (vertical) {
                    Column(
                        modifier.background(background_colour, shape = CircleShape),
                        verticalArrangement = Arrangement.SpaceEvenly, horizontalAlignment = Alignment.CenterHorizontally
                    ) { content() }
                }
                else {
                    Row(
                        modifier.background(background_colour, shape = CircleShape),
                        horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically
                    ) { content() }
                }
            }
        }
    }
}