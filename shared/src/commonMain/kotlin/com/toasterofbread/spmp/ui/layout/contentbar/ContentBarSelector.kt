package com.toasterofbread.spmp.ui.layout.contentbar

import LocalPlayerState
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.toasterofbread.composekit.platform.composable.platformClickable
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

    // var show_expanded_options: Boolean by remember { mutableStateOf(false) }
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
                .fillMaxSize()
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

                ContentBarSelectorMainRow(state, slot, rotate_modifier = rotate_modifier)
            }
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
            slot = slot,
            bars = state.available_bars,
            modifier = Modifier.requiredWidth(player.screen_size.width * 0.6f)
        ) { bar ->
            show_bar_selector = false
            state.onBarSelected(slot, bar)
        }
    }

    Crossfade(content_bar) { bar ->
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
            if (bar != null) {
                Icon(bar.getIcon(), null)
                Text(bar.getName())
            }
            else {
                Text(getString("content_bar_empty"))
            }
        }
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
                        bar.first.getName(),
                        bar.first.getDescription(),
                        bar.first.getIcon(),
                        Modifier
                            .contentBarPreview()
                            .platformClickable(onClick = { onSelected(bar) })
                    )
                }

                item {
                    ContentBarPreview(
                        getString("content_bar_empty"),
                        null,
                        Icons.Default.Close,
                        Modifier
                            .padding(top = 20.dp)
                            .contentBarPreview()
                            .platformClickable(onClick = { onSelected(null) })
                    )
                }
            }
        }
    )
}

@Composable
private fun ContentBarPreview(name: String, description: String?, icon: ImageVector, modifier: Modifier = Modifier) {
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, null)

        Column(Modifier.fillMaxWidth().weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleLarge)

            if (description != null) {
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
