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

class PillMenu(
    val action_count: Int = 0,
    val getAction: @Composable PillMenu.(i: Int) -> Unit = {},
    val expand_state: MutableState<Boolean>? = null,
    val background_colour: Color = Color.Unspecified,
    val content_colour: Color = Color.Unspecified,
    val top: Boolean = false,
    val left: Boolean = false,
    val vertical: Boolean = false,
    val container_modifier: Modifier = Modifier.fillMaxSize(),
    val toggleButton: (@Composable PillMenu.(modifier: Modifier) -> Unit)? = null,
    val modifier: Modifier = Modifier
) {

    val is_open: Boolean
        get() = expand_state?.value == true
        set(value) {
            expand_state?.value = value
        }

    private val extra_actions = mutableListOf<@Composable PillMenu.() -> Unit>()
    private val action_overriders = mutableListOf<@Composable PillMenu.(i: Int) -> Boolean>()

    fun addExtraAction(action: @Composable PillMenu.() -> Unit) {
        extra_actions.add(action)
    }
    fun removeExtraAction(action: @Composable PillMenu.() -> Unit) {
        extra_actions.remove(action)
    }
    fun clearExtraActions() {
        extra_actions.clear()
    }

    fun addActionOverrider(overrider: @Composable PillMenu.(i: Int) -> Boolean) {
        action_overriders.add(overrider)
    }
    fun removeActionOverrider(overrider: @Composable PillMenu.(i: Int) -> Boolean) {
        action_overriders.remove(overrider)
    }
    fun clearActionOverriders() {
        action_overriders.clear()
    }

    @Composable
    fun ActionButton(icon: ImageVector, action: () -> Unit) {
        IconButton(onClick = {
            action()
            is_open = !is_open
        }) {
            Icon(icon, null, tint = content_colour)
        }
    }

    @Composable
    fun PillMenu(
        action_count: Int = action_count,
        getAction: @Composable PillMenu.(i: Int) -> Unit = getAction,
        expand_state: MutableState<Boolean>?,
        background_colour: Color = background_colour,
        content_colour: Color = content_colour,
        top: Boolean = top,
        left: Boolean = left,
        vertical: Boolean = vertical,
        container_modifier: Modifier = ontainer_modifier,
        toggleButton: (@Composable PillMenu.(modifier: Modifier) -> Unit)? = toggleButton,
        modifier: Modifier = modifier,
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

            PillMenuParams(vertical, top, left, alignment, toggleButton, enter, exit, action_count, expand_state)
        }

        InnerPillMenu(params, content_colour, background_colour, modifier, container_modifier, getAction)
    }

    private data class CrossfadeParams(
        val vertical: Boolean,
        val top: Boolean,
        val left: Boolean,
        val alignment: Alignment,
        val toggleButton: (@Composable PillMenu.(modifier: Modifier) -> Unit)?,
        val enter: EnterTransition,
        val exit: ExitTransition,
        val action_count: Int,
        val expand_state: MutableState<Boolean>?
    )

    @Composable
    private fun InnerPillMenu(
        params: PillMenuParams,
        content_colour: Color,
        background_colour: Color,
        modifier: Modifier,
        container_modifier: Modifier,
        getAction: @Composable PillMenu.(i: Int) -> Unit
    ) {
        Crossfade(params, Modifier.zIndex(1f)) {
            val (vertical, top, left, alignment, toggleButton, enter, exit, action_count, expand_state) = it
            val align_start = (vertical && top) || (!vertical && left)

            @Composable
            fun ToggleButton() {
                if (expand_state != null) {
                    if (toggleButton != null) {
                        toggleButton(getter, Modifier.background(background_colour, shape = CircleShape))
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
            }

            Box(
                contentAlignment = alignment,
                modifier = container_modifier.padding(15.dp)
            ) {

                ToggleButton()

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
                            var overridden = false
                            for (overrider in action_overriders) {
                                if (overrider(this, i)) {
                                    overridden = true
                                    break
                                }
                            }
                            if (!overridden) {
                                getAction(this, i)
                            }
                        }

                        for (extra in extra_actions) {
                            extra(this)
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
}