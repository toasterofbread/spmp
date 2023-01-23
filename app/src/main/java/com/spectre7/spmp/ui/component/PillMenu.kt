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
import com.spectre7.utils.NoRipple
import com.spectre7.utils.Theme

class PillMenuActionGetter {
    val background_colour: Color
    val content_colour: Color
    val toggle: () -> Unit

    constructor(
        background_colour: Color,
        content_colour: Color,
        close: () -> Unit
    ) {
        this.background_colour = background_colour
        this.content_colour = content_colour
        this.toggle = close
    }

    constructor(
        theme: Theme,
        themed: Boolean = false
    ) {
        this.background_colour = theme.getBackground(themed)
        this.content_colour = theme.getBackground(themed)
        this.toggle = {}
    }

    @Composable
    fun ActionButton(icon: ImageVector, action: () -> Unit) {
        IconButton(onClick = {
            action()
            toggle()
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
    val toggle_button: (@Composable PillMenuActionGetter.(modifier: Modifier) -> Unit)?,
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
    toggle_button: (@Composable PillMenuActionGetter.(modifier: Modifier) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val params = remember(top, left, vertical, action_count, expand_state != null) {
        val alignment: Alignment
        val enter: EnterTransition
        val exit: ExitTransition

        val tween = tween<IntSize>(250)
        if (vertical) {
            if (top) {
                enter = expandVertically(tween, Alignment.Top)
                exit = shrinkVertically(tween, Alignment.Top)
            }
            else {
                enter = expandVertically(tween, Alignment.Bottom)
                exit = shrinkVertically(tween, Alignment.Bottom)
            }
        }
        else {
            if (left) {
                enter = expandHorizontally(tween, Alignment.Start)
                exit = shrinkHorizontally(tween, Alignment.Start)
            }
            else {
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

        PillMenuParams(vertical, top, left, alignment, toggle_button, enter, exit, action_count, expand_state)
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
        val (vertical, top, left, alignment, toggle_button, enter, exit, action_count, expand_state) = it
        val align_start = (vertical && top) || (!vertical && left)

        val getter = PillMenuActionGetter(background_colour, content_colour) { expand_state?.value = expand_state?.value != true }

        @Composable
        fun ToggleButton() {
            if (toggle_button != null) {
                toggle_button(getter, Modifier.background(background_colour, shape = CircleShape))
            }
            else {
                NoRipple {
                    val (open, close) = remember(top, left, vertical) {
                        if (vertical) {
                            if (top) {
                                Pair(Icons.Filled.KeyboardArrowDown, Icons.Filled.KeyboardArrowUp)
                            }
                            else {
                                Pair(Icons.Filled.KeyboardArrowUp, Icons.Filled.KeyboardArrowDown)
                            }
                        }
                        else {
                            if (left) {
                                Pair(Icons.Filled.KeyboardArrowRight, Icons.Filled.KeyboardArrowLeft)
                            }
                            else {
                                Pair(Icons.Filled.KeyboardArrowLeft, Icons.Filled.KeyboardArrowRight)
                            }
                        }
                    }

                    IconButton(
                        onClick = {
                            if (expand_state != null) expand_state.value = !expand_state.value
                        },
                        modifier = Modifier.background(background_colour, shape = CircleShape)
                    ) {
                        Crossfade(expand_state?.value == true) {
                            Icon(if (it) close else open, null, tint = content_colour)
                        }
                    }
                }
            }
        }

        Box(
            contentAlignment = alignment,
            modifier = container_modifier.padding(15.dp)
        ) {

            if (expand_state != null) {
                ToggleButton()
            }

            AnimatedVisibility(
                visible = expand_state?.value ?: true,
                enter = enter,
                exit = exit
            ) {

                @Composable
                fun content() {
                    if (align_start) {
                        ToggleButton()
                    }

                    for (i in 0 until action_count) {
                        getAction(getter, i, action_count)
                    }

                    if (!align_start) {
                        ToggleButton()
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