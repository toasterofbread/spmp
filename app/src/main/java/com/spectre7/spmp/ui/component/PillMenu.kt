package com.spectre7.spmp.ui.component

import android.annotation.SuppressLint
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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.spectre7.utils.NoRipple
import com.spectre7.utils.RowOrColumn
import com.spectre7.utils.getContrasted

class PillMenu(
    private val action_count: Int = 0,
    private val getAction: @Composable (Action.(i: Int, action_count: Int) -> Unit) = { _, _ -> },
    private val expand_state: MutableState<Boolean>? = null,
    private val _background_colour: () -> Color = { Color.Unspecified },
    top: Boolean = false,
    left: Boolean = false,
    vertical: Boolean = false,
    private val container_modifier: Modifier = Modifier,
    private val toggleButton: (@Composable Action.(modifier: Modifier) -> Unit)? = null,
    private val alongsideContent: (@Composable Action.() -> Unit)? = null,
    private val modifier: Modifier = Modifier
) {

    var top by mutableStateOf(top)
    var left by mutableStateOf(left)
    var vertical by mutableStateOf(vertical)

    var is_open: Boolean
        get() = expand_state?.value == true
        set(value) {
            expand_state?.value = value
        }

    var showing: Boolean by mutableStateOf(true)

    private val extra_actions_inner = mutableStateListOf<@Composable Action.(action_count: Int) -> Unit>()
    private val extra_actions_outer = mutableStateListOf<@Composable Action.(action_count: Int) -> Unit>()
    private val extra_alongside_actions = mutableStateListOf<@Composable Action.() -> Unit>()
    private val action_overriders = mutableStateListOf<@Composable Action.(i: Int) -> Boolean>()

    private val background_colour = Animatable(Color.Unspecified)
    private var background_colour_override: Color? by mutableStateOf(null)

    fun setBackgroundColourOverride(colour: Color?) {
        background_colour_override = colour
    }
    
    fun addAlongsideAction(action: @Composable Action.() -> Unit) {
        extra_alongside_actions.add(action)
    }
    fun removeAlongsideAction(action: @Composable Action.() -> Unit) {
        extra_alongside_actions.remove(action)
    }
    fun clearAlongsideActions() {
        extra_alongside_actions.clear()
    }

    fun addExtraAction(inner: Boolean = true, action: @Composable Action.(action_count: Int) -> Unit): @Composable Action.(action_count: Int) -> Unit {
        (if (inner) extra_actions_inner else extra_actions_outer).add(action)
        return action
    }
    fun removeExtraAction(action: @Composable Action.(action_count: Int) -> Unit) {
        if (!extra_actions_inner.remove(action)) {
            extra_actions_outer.remove(action)
        }
    }
    fun clearExtraActions() {
        extra_actions_inner.clear()
        extra_actions_outer.clear()
    }

    fun addActionOverrider(overrider: @Composable Action.(i: Int) -> Boolean) {
        action_overriders.add(overrider)
    }
    fun removeActionOverrider(overrider: @Composable Action.(i: Int) -> Boolean) {
        action_overriders.remove(overrider)
    }
    fun clearActionOverriders() {
        action_overriders.clear()
    }

    inner class Action(
        val background_colour: Color,
        val content_colour: Color
    ) {
        var is_open: Boolean
            get() = this@PillMenu.is_open
            set(value) {
                this@PillMenu.is_open = value
            }

        @Composable
        fun ActionButton(icon: ImageVector, action: () -> Unit) {
            IconButton(onClick = {
                is_open = false
                action()
            }) {
                Icon(icon, null, tint = content_colour)
            }
        }

        @Composable
        fun ActionButton(icon: Painter, action: () -> Unit) {
            IconButton(onClick = {
                is_open = false
                action()
            }) {
                Icon(icon, null, tint = content_colour)
            }
        }
    }

    @SuppressLint("NotConstructor")
    @Composable
    fun PillMenu(
        action_count: Int = this.action_count,
        getAction: @Composable() (Action.(i: Int, action_count: Int) -> Unit) = this.getAction,
        expand_state: MutableState<Boolean>? = this.expand_state,
        _background_colour: () -> Color = this._background_colour,
        top: Boolean = this.top,
        left: Boolean = this.left,
        vertical: Boolean = this.vertical,
        container_modifier: Modifier = this.container_modifier,
        toggleButton: (@Composable Action.(modifier: Modifier) -> Unit)? = this.toggleButton,
        alongsideContent: (@Composable Action.() -> Unit)? = this.alongsideContent,
        modifier: Modifier = this.modifier,
    ) {
        LaunchedEffect(Unit) {
            background_colour.snapTo(_background_colour())
        }

        LaunchedEffect(background_colour_override, _background_colour()) {
            background_colour.animateTo(background_colour_override ?: _background_colour())
        }

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

            CrossfadeParams(vertical, top, left, alignment, toggleButton, enter, exit, action_count, expand_state)
        }

        InnerPillMenu(params, background_colour.value, modifier, container_modifier, getAction, alongsideContent)
    }

    private data class CrossfadeParams(
        val vertical: Boolean,
        val top: Boolean,
        val left: Boolean,
        val alignment: Alignment,
        val toggleButton: (@Composable Action.(modifier: Modifier) -> Unit)?,
        val enter: EnterTransition,
        val exit: ExitTransition,
        val action_count: Int,
        val expand_state: MutableState<Boolean>?
    )

    @Composable
    private fun InnerPillMenu(
        params: CrossfadeParams,
        background_colour: Color,
        modifier: Modifier,
        container_modifier: Modifier,
        getAction: @Composable Action.(i: Int, action_count: Int) -> Unit,
        alongsideContent: (@Composable Action.() -> Unit)? = null
    ) {
        val action = remember(background_colour) { Action(background_colour, background_colour.getContrasted()) }

        Crossfade(Pair(showing, params), Modifier.zIndex(1f)) { data ->
            val (visible, crossfade_params) = data
            val (vertical, top, left, alignment, toggleButton, enter, exit, action_count, expand_state) = crossfade_params
            val align_start = (vertical && top) || (!vertical && left)

            @Composable
            fun ToggleButton() {
                if (expand_state != null) {
                    if (toggleButton != null) {
                        toggleButton(action, Modifier.background(background_colour, shape = CircleShape))
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
                                    expand_state.value = !expand_state.value
                                },
                                modifier = Modifier.background(background_colour, shape = CircleShape)
                            ) {
                                Crossfade(expand_state.value) { expanded ->
                                    Icon(if (expanded) close else open, null, tint = background_colour.getContrasted())
                                }
                            }
                        }
                    }
                }
            }

            if (visible) {
                Box(
                    contentAlignment = alignment,
                    modifier = container_modifier
                        .fillMaxSize()
                        .padding(15.dp)
                ) {

                    ToggleButton()

                    val start = if (vertical) top else left
                    val fill_modifier = if (vertical) Modifier.fillMaxHeight() else Modifier.fillMaxWidth()

                    @Composable
                    fun Alongside() {
                        if (start && alongsideContent != null) {
                            alongsideContent(action)
                        }

                        for (extra in extra_alongside_actions.let { if (start) it.asReversed() else it }) {
                            extra(action)
                        }

                        if (!start && alongsideContent != null) {
                            alongsideContent(action)
                        }
                    }

                    RowOrColumn(!vertical, fill_modifier.height(IntrinsicSize.Max), Arrangement.spacedBy(10.dp)) { getWeightModifier ->
                        if (!start) {
                            Alongside()
                            Spacer(fill_modifier.then(getWeightModifier(1f)))
                        }

                        AnimatedVisibility(
                            visible = expand_state?.value ?: true,
                            enter = enter,
                            exit = exit
                        ) {
                            RowOrColumn(!vertical, modifier.background(background_colour, shape = CircleShape)) {
                                if (align_start) {
                                    for (extra in extra_actions_outer) {
                                        extra(action, action_count)
                                    }
                                    ToggleButton()
                                }
                                else {
                                    for (extra in extra_actions_inner) {
                                        extra(action, action_count)
                                    }
                                }

                                for (i in 0 until action_count) {
                                    var overridden = false
                                    for (overrider in action_overriders) {
                                        if (overrider(action, i)) {
                                            overridden = true
                                            break
                                        }
                                    }
                                    if (!overridden) {
                                        getAction(action, i, action_count)
                                    }
                                }

                                if (align_start) {
                                    for (extra in extra_actions_inner) {
                                        extra(action,  action_count)
                                    }
                                }
                                else {
                                    ToggleButton()
                                    for (extra in extra_actions_outer) {
                                        extra(action, action_count)
                                    }
                                }
                            }
                        }

                        if (start) {
                            Spacer(fill_modifier.then(getWeightModifier(1f)))
                            Alongside()
                        }
                    }
                }
            }
        }
    }
}
