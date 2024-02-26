package com.toasterofbread.spmp.ui.layout.contentbar

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import com.toasterofbread.spmp.ui.layout.apppage.AppPage
import androidx.compose.ui.Modifier
import androidx.compose.runtime.*
import androidx.compose.material3.Text
import androidx.compose.animation.Crossfade
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.width
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.DpSize
import com.toasterofbread.composekit.utils.composable.RowOrColumn
import com.toasterofbread.composekit.utils.common.thenIf
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.draw.rotate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.foundation.lazy.items
import com.toasterofbread.spmp.resources.getString
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import com.toasterofbread.spmp.ui.layout.nowplaying.maintab.vertical
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.ui.unit.IntSize
import LocalPlayerState
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalContentColor
import com.toasterofbread.composekit.utils.modifier.background
import androidx.compose.foundation.background
import com.toasterofbread.composekit.utils.common.getContrasted
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import com.toasterofbread.composekit.utils.modifier.bounceOnClick
import com.toasterofbread.spmp.ui.theme.appHover
import androidx.compose.animation.core.tween
import com.toasterofbread.composekit.platform.composable.platformClickable
import androidx.compose.ui.graphics.Shape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.unit.Dp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.Icons
import androidx.compose.ui.draw.clipToBounds
import com.toasterofbread.composekit.settings.ui.Theme
import androidx.compose.material.icons.filled.Palette
import androidx.compose.ui.graphics.Color
import com.toasterofbread.spmp.ui.component.ColourSelectionDialog
import com.toasterofbread.composekit.utils.composable.ShapedIconButton
import androidx.compose.material3.IconButtonDefaults
import com.toasterofbread.spmp.model.settings.category.LayoutSettings
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import com.toasterofbread.spmp.ui.component.getReadable
import androidx.compose.material.icons.filled.Done
import androidx.compose.runtime.State
import androidx.compose.foundation.layout.RowScope
import com.toasterofbread.composekit.utils.common.thenWith
import androidx.compose.animation.animateContentSize

sealed class ContentBar {
    abstract fun getName(): String
    abstract fun getDescription(): String?
    abstract fun getIcon(): ImageVector

    @Composable
    fun Bar(slot: LayoutSlot, modifier: Modifier = Modifier) {
        val player: PlayerState = LocalPlayerState.current

        Crossfade(bar_selection_state) { selection_state ->
            if (selection_state == null) {
                val slot_colour_source: ColourSource? by slot.rememberColourSource()

                val background_colour: Color by remember { derivedStateOf {
                    slot_colour_source?.get(player.theme)
                        ?: slot.getDefaultBackgroundColour(player.theme)
                } }

                CompositionLocalProvider(LocalContentColor provides background_colour) {
                    BarContent(
                        slot,
                        modifier.background(background_colour)
                    )
                }

                return@Crossfade
            }

            val selctor_size: Dp = 70.dp
            val selector_modifier: Modifier =
                if (slot.is_vertical) modifier.width(selctor_size)
                else modifier.height(selctor_size)

            ContentBarSelector(
                selection_state,
                slot,
                selector_modifier
            )
        }
    }

    @Composable
    protected abstract fun BarContent(slot: LayoutSlot, modifier: Modifier)

    companion object {
        var _bar_selection_state: BarSelectionState? by mutableStateOf(null)
        var bar_selection_state: BarSelectionState?
            get() = if (disable_bar_selection) null else _bar_selection_state
            set(value) { _bar_selection_state = value }

        var disable_bar_selection: Boolean by mutableStateOf(false)

        fun deserialise(data: String): ContentBar {
            val internal_bar_index: Int? = data.toIntOrNull()
            if (internal_bar_index != null) {
                return InternalContentBar.getAll()[internal_bar_index]
            }

            return Json.decodeFromString<CustomContentBar>(data)
        }
    }

    interface BarSelectionState {
        val available_bars: List<Pair<ContentBar, Int>>
        fun onBarSelected(slot: LayoutSlot, bar: Pair<ContentBar, Int>?)
        fun onThemeColourSelected(slot: LayoutSlot, colour: Theme.Colour)
        fun onCustomColourSelected(slot: LayoutSlot, colour: Color)
    }

    @Composable
    private fun ContentBarSelector(state: BarSelectionState, slot: LayoutSlot, modifier: Modifier = Modifier) {
        val player: PlayerState = LocalPlayerState.current
        val density: Density = LocalDensity.current
        val slot_colour_source: ColourSource? by slot.rememberColourSource()

        var show_expanded_options: Boolean by remember { mutableStateOf(false) }
        var show_colour_selector: Boolean by remember { mutableStateOf(false) }

        if (show_colour_selector) {
            ColourSelectionDialog(
                onDismissed = {
                    show_colour_selector = false
                },
                onThemeColourSelected = {
                    state.onThemeColourSelected(slot, it)
                    show_colour_selector = false
                },
                onCustomColourSelected = {
                    state.onCustomColourSelected(slot, it)
                    show_colour_selector = false
                }
            )
        }

        CompositionLocalProvider(LocalContentColor provides player.theme.on_accent) {
            BoxWithConstraints(
                modifier
                    .clipToBounds()
                    .background { player.theme.accent }
                    .padding(10.dp)
                    .fillMaxSize()
                    .thenIf(slot.is_vertical) {
                        padding(bottom = player.nowPlayingBottomPadding(true))
                    }
            ) {
                Row(
                    Modifier
                        .thenIf(slot.is_vertical) {
                            rotate(-90f)
                            .vertical()
                            .requiredSize(maxHeight, maxWidth)
                            .offset { with (density) {
                                IntOffset(
                                    (maxWidth - maxHeight).roundToPx() / 2,
                                    0
                                )
                            } }
                            .wrapContentHeight()
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val rotate_modifier: Modifier = Modifier.thenIf(slot.is_vertical) { rotate(90f) }

                    Text(slot.getName())

                    val colour_button_background_colour: Color =
                        slot_colour_source?.get(player.theme) ?: player.theme.background

                    ConfigButton(
                        Modifier
                            .platformClickable(
                                onClick = { show_colour_selector = !show_colour_selector }
                            ),
                        background_colour = colour_button_background_colour,
                        border_colour = colour_button_background_colour.getContrasted()
                    ) {
                        Icon(Icons.Default.Palette, null, rotate_modifier)

                        slot_colour_source?.theme_colour?.also {
                            Text(it.getReadable())
                        }
                    }

                    ContentBarSelectorMainRow(state, slot, rotate_modifier = rotate_modifier)
                }
            }
        }
    }

    @Composable
    private fun ContentBarSelectorMainRow(
        state: BarSelectionState,
        slot: LayoutSlot,
        rotate_modifier: Modifier,
        modifier: Modifier = Modifier
    ) {
        val player: PlayerState = LocalPlayerState.current
        var show_bar_selector: Boolean by remember { mutableStateOf(false) }

        if (show_bar_selector) {
            BarSelectorPopup(
                slot = slot,
                bars = state.available_bars,
                modifier = Modifier.requiredWidth(player.screen_size.width * 0.6f)
            ) { bar ->
                show_bar_selector = false
                if (bar != null) {
                    state.onBarSelected(slot, bar)
                }
            }
        }

        ConfigButton(
            modifier
                .platformClickable(
                    onClick = {
                        show_bar_selector = true
                    },
                    onAltClick = {
                        state.onBarSelected(slot, null)
                    }
                )
                .fillMaxWidth()
        ) {
            Icon(getIcon(), null)
            Text(getName())
        }
    }

    @Composable
    private fun ConfigButton(
        modifier: Modifier = Modifier,
        background_colour: Color = LocalPlayerState.current.theme.background,
        border_colour: Color? = null,
        content: @Composable RowScope.() -> Unit
    ) {
        Row(
            modifier
                .contentBarPreview(background_colour = background_colour, border_colour = border_colour)
                .animateContentSize(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompositionLocalProvider(LocalContentColor provides background_colour.getContrasted()) {
                content()
            }
        }
    }
}

@Composable
private fun BarSelectorPopup(
    slot: LayoutSlot,
    bars: List<Pair<ContentBar, Int>>,
    modifier: Modifier = Modifier,
    onSelected: (Pair<ContentBar, Int>?) -> Unit
) {
    val player: PlayerState = LocalPlayerState.current

    AlertDialog(
        { onSelected(null) },
        modifier = modifier,
        confirmButton = {
            Button(
                { onSelected(null) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = player.theme.background,
                    contentColor = player.theme.on_background
                ),
                modifier = Modifier.appHover(true)
            ) {
                Text(getString("action_cancel"))
            }
        },
        title = {
            Text(getString("layout_slot_content_bar_selection"))
        },
        text = {
            LazyColumn(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(bars) { bar ->
                    ContentBarPreview(
                        bar.first,
                        Modifier
                            .contentBarPreview()
                            .platformClickable(onClick = { onSelected(bar) })
                    )
                }
            }
        }
    )
}

@Composable
private fun ContentBarPreview(bar: ContentBar, modifier: Modifier = Modifier) {
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(bar.getIcon(), null)

        Column(Modifier.fillMaxWidth().weight(1f)) {
            Text(bar.getName(), style = MaterialTheme.typography.titleLarge)

            bar.getDescription()?.also { description ->
                Text(description, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun Modifier.contentBarPreview(background_colour: Color? = null, border_colour: Color? = null): Modifier {
    val player: PlayerState = LocalPlayerState.current
    val shape: Shape = RoundedCornerShape(20.dp)

    return (
        appHover(
            button = true,
            expand = true,
            hover_scale = 0.98f,
            animation_spec = tween(200)
        )
        .thenWith(border_colour) {
            border(2.dp, it, shape)
        }
        .background(shape) { background_colour ?: player.theme.background }
        .padding(10.dp)
    )
}
