package com.spectre7.spmp.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.spectre7.spmp.MainActivity
import com.spectre7.utils.NoRipple

class ActionGetter(
    val background_colour: Color,
    val content_colour: Color,
    val close: () -> Unit
) {
    @Composable
    fun ActionButton(icon: ImageVector, action: () -> Unit) {
        IconButton(onClick = {
            action()
            close()
        }) {
            Icon(icon, "", tint = MainActivity.theme.getOnAccent())
        }
    }
}

@Composable
fun PillMenu(
    action_count: Int,
    get_action: @Composable ActionGetter.(Int) -> Unit,
    expand_state: MutableState<Boolean>?,
    background_colour: Color,
    content_colour: Color,
    top: Boolean = false,
    left: Boolean = false,
    vertical: Boolean = false
) {

    val alignment: Alignment
    val open_icon: ImageVector
    val close_icon: ImageVector
    val enter: EnterTransition
    val exit: ExitTransition

    val align_start = (vertical && top) || (!vertical && left)
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

    Box(
        contentAlignment = alignment,
        modifier = Modifier
            .fillMaxSize()
            .zIndex(1f)
            .padding(15.dp)
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

                val getter = ActionGetter(background_colour, content_colour) { expand_state?.value = false }
                for (i in 0 until action_count) {
                    get_action(getter, i)
                }

                if (!align_start) {
                    closeButton()
                }
            }

            if (vertical) {
                Column(
                    Modifier.background(background_colour, shape = CircleShape),
                    verticalArrangement = if (align_start) Arrangement.Top else Arrangement.Bottom, horizontalAlignment = Alignment.CenterHorizontally
                ) { content() }
            }
            else {
                Row(
                    Modifier.background(background_colour, shape = CircleShape),
                    horizontalArrangement = if (align_start) Arrangement.Start else Arrangement.End, verticalAlignment = Alignment.CenterVertically
                ) { content() }
            }
        }
    }
}