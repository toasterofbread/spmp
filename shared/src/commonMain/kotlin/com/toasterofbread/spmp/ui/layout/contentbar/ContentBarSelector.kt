package com.toasterofbread.spmp.ui.layout.contentbar

import LocalPlayerState
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import com.toasterofbread.composekit.platform.composable.*
import com.toasterofbread.composekit.utils.common.*
import com.toasterofbread.composekit.utils.modifier.background
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.component.*
import com.toasterofbread.spmp.ui.layout.nowplaying.maintab.vertical
import com.toasterofbread.spmp.ui.theme.appHover

@Composable
internal fun ContentBarSelector(
    state: ContentBar.BarSelectionState,
    slot: LayoutSlot,
    content_padding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val player: PlayerState = LocalPlayerState.current
    val density: Density = LocalDensity.current
    val slot_colour_source: ColourSource by slot.rememberColourSource()

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

    BoxWithConstraints(
        modifier
            .clipToBounds()
            .fillMaxSize()
            .background(player.theme.background)
            .border(1.dp, player.theme.vibrant_accent)
            .padding(content_padding)
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

            val colour_button_background_colour: Color = slot_colour_source.get(player.theme)

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

            ContentBarSelectorMainRow(state, slot, rotate_modifier)
        }
    }
}

@Composable
private fun ContentBarSelectorMainRow(
    state: ContentBar.BarSelectionState,
    slot: LayoutSlot,
    rotate_modifier: Modifier,
    modifier: Modifier = Modifier
) {
    val player: PlayerState = LocalPlayerState.current
    val content_bar: ContentBar? by slot.observeContentBar()

    var show_bar_selector: Boolean by remember { mutableStateOf(false) }

    if (show_bar_selector) {
        BarSelectorPopup(
            state,
            slot,
            size_modifier = Modifier
                .widthIn(min = player.screen_size.width * 0.8f)
                .heightIn(max = player.screen_size.height * 0.8f),
            onSelected = { bar ->
                show_bar_selector = false
                state.onBarSelected(slot, bar)
            },
            onDismissed = {
                show_bar_selector = false
            }
        )
    }

    ConfigButton(
        modifier
            .platformClickable(
                onClick = {
                    show_bar_selector = true
                }
            )
            .fillMaxWidth()
    ) {
        val bar: ContentBar? = content_bar
        if (bar != null) {
            Icon(bar.getIcon(), null)
            Text(bar.getName())
        }
        else {
            Text(getString("content_bar_empty"))
        }
    }
}

@Composable
private fun ConfigButton(
    modifier: Modifier = Modifier,
    background_colour: Color = LocalPlayerState.current.theme.accent,
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BarSelectorPopup(
    state: ContentBar.BarSelectionState,
    slot: LayoutSlot,
    onSelected: (ContentBarReference?) -> Unit,
    onDismissed: () -> Unit,
    modifier: Modifier = Modifier,
    size_modifier: Modifier = Modifier
) {
    val player: PlayerState = LocalPlayerState.current

    AlertDialog(
        onDismissed,
        modifier = modifier,
        confirmButton = {
            Button(
                onDismissed,
                colors = ButtonDefaults.buttonColors(
                    containerColor = player.theme.background,
                    contentColor = player.theme.on_background
                ),
                modifier = Modifier.appHover(true)
            ) {
                Text(getString("action_cancel"))
            }
        },
        dismissButton = {
            Button({ onSelected(null) }) {
                Icon(Icons.Default.Close, null)
                Text(getString("content_bar_empty"))
            }
        },
        title = {
            Text(getString("content_bar_selection"))
        },
        text = {
            FlowRow(
                size_modifier.verticalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                ContentBarList(
                    state.built_in_bars,
                    getString("content_bar_selection_list_built_in")
                ) {
                    onSelected(state.built_in_bars[it])
                }

                CustomBarsContentBarList(
                    state,
                    onSelected = onSelected,
                    onDismissed = onDismissed,
                    lazy = false
                )
            }
        }
    )
}

@Composable
internal fun CustomBarsContentBarList(
    state: ContentBar.BarSelectionState,
    onSelected: ((ContentBarReference) -> Unit)?,
    onDismissed: () -> Unit,
    modifier: Modifier = Modifier,
    lazy: Boolean = false
) {
    val player: PlayerState = LocalPlayerState.current

    ContentBarList(
        state.custom_bars,
        getString("content_bar_selection_list_custom"),
        modifier,
        topContent = {
            val background_colour: Color = player.theme.vibrant_accent
            CompositionLocalProvider(LocalContentColor provides background_colour.getContrasted()) {
                ContentBarPreview(
                    getString("content_bar_selection_create_new"),
                    null,
                    Icons.Default.Add,
                    Modifier
                        .contentBarPreview(background_colour = background_colour)
                        .platformClickable(onClick = {
                            state.createCustomBar()
                        })
                )
            }
        },
        buttonBottomContent = { index ->
            Row {
                IconButton(
                    {
                        state.onCustomBarEditRequested(state.custom_bars[index])
                        onDismissed()
                    }
                ) {
                    Icon(Icons.Default.Edit, null)
                }

                IconButton(
                    {
                        state.deleteCustomBar(state.custom_bars[index])
                    }
                ) {
                    Icon(Icons.Default.Delete, null)
                }
            }
        },
        onSelected =
            if (onSelected != null) {{ onSelected(state.custom_bars[it]) }}
            else null,
        lazy = lazy
    )
}

@Composable
internal fun ContentBarList(
    bars: List<ContentBarReference>,
    title: String,
    modifier: Modifier = Modifier,
    lazy: Boolean = false,
    topContent: @Composable () -> Unit = {},
    buttonBottomContent: @Composable (Int) -> Unit = {},
    onSelected: ((Int) -> Unit)?
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title)

        @Composable
        fun Bar(bar: ContentBarReference, index: Int) {
            ContentBarPreview(
                bar.first.getName(),
                bar.first.getDescription(),
                bar.first.getIcon(),
                Modifier
                    .contentBarPreview(interactive = onSelected != null)
                    .thenWith(onSelected) {
                        clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { it(index) }
                    },
                bottomContent = {
                    buttonBottomContent(index)
                }
            )
        }

        val arrangement: Arrangement.Vertical = Arrangement.spacedBy(10.dp)
        if (lazy) {
            ScrollBarLazyColumn(verticalArrangement = arrangement) {
                item {
                    topContent()
                }

                itemsIndexed(bars) { index, bar ->
                    Bar(bar, index)
                }
            }
        }
        else {
            Column(verticalArrangement = arrangement) {
                topContent()

                for (bar in bars.withIndex()) {
                    Bar(bar.value, bar.index)
                }
            }
        }
    }
}

@Composable
private fun ContentBarPreview(
    name: String,
    description: String?,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    bottomContent: @Composable () -> Unit = {}
) {
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, null)

        Column(Modifier.fillMaxWidth().weight(1f), horizontalAlignment = Alignment.End) {
            Text(name, Modifier.align(Alignment.Start), style = MaterialTheme.typography.titleLarge)

            if (description != null) {
                Text(description, Modifier.align(Alignment.Start), style = MaterialTheme.typography.labelLarge)
            }

            bottomContent()
        }
    }
}

@Composable
private fun Modifier.contentBarPreview(
    background_colour: Color? = null,
    border_colour: Color? = null,
    interactive: Boolean = true
): Modifier {
    val player: PlayerState = LocalPlayerState.current
    val shape: Shape = RoundedCornerShape(20.dp)

    return (
        thenIf(interactive) {
            appHover(
                button = true,
                expand = true,
                hover_scale = 0.98f,
                animation_spec = tween(200)
            )
        }
        .thenWith(border_colour) {
            border(1.dp, it, shape)
        }
        .background(shape) { background_colour ?: player.theme.background }
        .padding(10.dp)
    )
}
