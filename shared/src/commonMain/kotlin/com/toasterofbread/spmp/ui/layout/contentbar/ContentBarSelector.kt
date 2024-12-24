package com.toasterofbread.spmp.ui.layout.contentbar

import LocalPlayerState
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import androidx.compose.ui.unit.sp
import dev.toastbits.composekit.components.platform.composable.*
import dev.toastbits.composekit.util.*
import dev.toastbits.composekit.util.getContrasted
import dev.toastbits.composekit.components.utils.modifier.background
import dev.toastbits.composekit.components.utils.composable.NoRipple
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.component.*
import com.toasterofbread.spmp.ui.layout.contentbar.layoutslot.LayoutSlot
import com.toasterofbread.spmp.ui.layout.contentbar.layoutslot.observeContentBar
import com.toasterofbread.spmp.ui.layout.contentbar.layoutslot.ColourSource
import com.toasterofbread.spmp.ui.layout.contentbar.layoutslot.rememberColourSource
import com.toasterofbread.spmp.ui.layout.nowplaying.maintab.vertical
import com.toasterofbread.spmp.ui.theme.appHover
import dev.toastbits.composekit.theme.core.readableName
import dev.toastbits.composekit.theme.core.vibrantAccent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.action_close
import spmp.shared.generated.resources.content_bar_empty
import spmp.shared.generated.resources.action_cancel
import spmp.shared.generated.resources.content_bar_selection
import spmp.shared.generated.resources.content_bar_selection_list_built_in
import spmp.shared.generated.resources.content_bar_selection_list_custom
import spmp.shared.generated.resources.content_bar_selection_create_new

@Composable
internal fun ContentBarSelector(
    state: ContentBar.BarSelectionState,
    slot: LayoutSlot,
    modifier: Modifier = Modifier,
    slot_config: JsonElement? = null,
    content_padding: PaddingValues = PaddingValues(),
    base_content_padding: PaddingValues = PaddingValues()
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
            onColourSelected = {
                state.onColourSelected(slot, it)
                show_colour_selector = false
            }
        )
    }

    NoRipple {
        BoxWithConstraints(
            modifier
                .clipToBounds()
                .fillMaxSize()
                .background(player.theme.background)
                .padding(content_padding)
                .border(1.dp, player.theme.vibrantAccent)
                .padding(base_content_padding)
        ) {
            CompositionLocalProvider(LocalContentColor provides player.theme.background.getContrasted()) {
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

                    // Text(slot.getName())

                    val colour_button_background_colour: Color = slot_colour_source.get(player)

                    ConfigButton(
                        Modifier
                            .clickable {
                                show_colour_selector = !show_colour_selector
                            },
                        background_colour = colour_button_background_colour,
                        border_colour = colour_button_background_colour.getContrasted()
                    ) {
                        Icon(Icons.Default.Palette, null, rotate_modifier)

                        slot_colour_source.theme_colour?.also {
                            Text(it.readableName, lineHeight = 10.sp)
                        }
                    }

                    ContentBarSelectorMainRow(state, slot, slot_config, rotate_modifier)
                }
            }
        }
    }
}

@Composable
private fun ContentBarSelectorMainRow(
    state: ContentBar.BarSelectionState,
    slot: LayoutSlot,
    slot_config: JsonElement?,
    rotate_modifier: Modifier,
    modifier: Modifier = Modifier
) {
    val player: PlayerState = LocalPlayerState.current
    val coroutine_scope: CoroutineScope = rememberCoroutineScope()
    val content_bar: ContentBar? by slot.observeContentBar()

    var show_bar_selector: Boolean by remember { mutableStateOf(false) }
    var show_slot_config: Boolean by remember { mutableStateOf(false) }

    if (show_bar_selector) {
        BarSelectorPopup(
            state,
            slot,
            size_modifier = Modifier
                .widthIn(min = player.screen_size.width * 0.8f)
                .heightIn(max = player.screen_size.height * 0.8f),
            onSelected = { bar ->
                show_bar_selector = false
                coroutine_scope.launch {
                    state.onBarSelected(slot, bar)
                }
            },
            onDismissed = {
                show_bar_selector = false
            }
        )
    }

    if (show_slot_config) {
        AlertDialog(
            onDismissRequest = { show_slot_config = false },
            confirmButton = {
                Button({ show_slot_config = false }) {
                    Text(stringResource(Res.string.action_close))
                }
            },
            title = { Text(slot.getName()) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    slot.ConfigurationItems(
                        slot_config,
                        Modifier.fillMaxWidth()
                    ) {
                        state.onSlotConfigChanged(slot, it)
                    }
                }
            }
        )
    }

    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (slot.hasConfig()) {
            ConfigButton(
                Modifier.clickable { show_slot_config = true }
            ) {
                Icon(Icons.Default.Settings, null)
            }
        }

        ConfigButton(
            Modifier
                .clickable { show_bar_selector = true }
                .fillMaxWidth()
                .weight(1f)
        ) {
            val bar: ContentBar? = content_bar
            if (bar != null) {
                Icon(bar.getIcon(), null)
                Text(bar.getName(), lineHeight = 10.sp)
            }
            else {
                Text(stringResource(Res.string.content_bar_empty))
            }
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
                    contentColor = player.theme.onBackground
                ),
                modifier = Modifier.appHover(true)
            ) {
                Text(stringResource(Res.string.action_cancel))
            }
        },
        dismissButton = {
            Button({ onSelected(null) }) {
                Icon(Icons.Default.Close, null)
                Text(stringResource(Res.string.content_bar_empty))
            }
        },
        title = {
            Text(stringResource(Res.string.content_bar_selection))
        },
        text = {
            FlowRow(
                size_modifier.verticalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                ContentBarList(
                    state.built_in_bars,
                    stringResource(Res.string.content_bar_selection_list_built_in)
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
    lazy: Boolean = false,
    bar_background_colour: Color? = null
) {
    val player: PlayerState = LocalPlayerState.current
    val coroutine_scope: CoroutineScope = rememberCoroutineScope()

    ContentBarList(
        state.custom_bars,
        stringResource(Res.string.content_bar_selection_list_custom),
        modifier,
        topContent = {
            val background_colour: Color = player.theme.vibrantAccent
            CompositionLocalProvider(LocalContentColor provides background_colour.getContrasted()) {
                ContentBarPreview(
                    stringResource(Res.string.content_bar_selection_create_new),
                    null,
                    Icons.Default.Add,
                    Modifier
                        .contentBarPreview(background_colour = background_colour)
                        .clickable {
                            coroutine_scope.launch {
                                state.createCustomBar()
                            }
                        }
                )
            }
        },
        buttonEndContent = { modifier, index ->
            Row(modifier) {
                IconButton(
                    {
                        state.onCustomBarEditRequested(state.custom_bars[index])
                        player.switchNowPlayingPage(0)
                        onDismissed()
                    }
                ) {
                    Icon(Icons.Default.Edit, null)
                }

                IconButton(
                    {
                        coroutine_scope.launch {
                            state.deleteCustomBar(state.custom_bars[index])
                        }
                    }
                ) {
                    Icon(Icons.Default.Delete, null)
                }
            }
        },
        onSelected =
            if (onSelected != null) {{ onSelected(state.custom_bars[it]) }}
            else null,
        lazy = lazy,
        bar_background_colour = bar_background_colour
    )
}

@Composable
internal fun ContentBarList(
    bar_references: List<ContentBarReference>,
    title: String,
    modifier: Modifier = Modifier,
    lazy: Boolean = false,
    bar_background_colour: Color? = null,
    topContent: @Composable () -> Unit = {},
    buttonEndContent: @Composable (Modifier, Int) -> Unit = { _, _ -> },
    onSelected: ((Int) -> Unit)?
) {
    val player: PlayerState = LocalPlayerState.current
    val custom_bars: List<CustomContentBar> by player.settings.Layout.CUSTOM_BARS.observe()
    val bars: List<ContentBar> = remember(bar_references, custom_bars) {
        bar_references.mapNotNull { it.getBar(custom_bars) }
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title)

        @Composable
        fun Bar(bar: ContentBar, index: Int) {
            ContentBarPreview(
                bar.getName(),
                bar.getDescription(),
                bar.getIcon(),
                Modifier
                    .contentBarPreview(interactive = onSelected != null, background_colour = bar_background_colour)
                    .thenWith(onSelected) {
                        clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { it(index) }
                    },
                endContent = {
                    buttonEndContent(it, index)
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

                for ((index, bar) in bars.withIndex()) {
                    Bar(bar, index)
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
    endContent: @Composable (Modifier) -> Unit = {}
) {
    FlowRow(
        modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, null, Modifier.align(Alignment.CenterVertically))

        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .align(Alignment.CenterVertically),
            horizontalAlignment = Alignment.End
        ) {
            Text(name, Modifier.align(Alignment.Start), style = MaterialTheme.typography.titleLarge)

            if (description != null) {
                Text(description, Modifier.align(Alignment.Start), style = MaterialTheme.typography.labelLarge)
            }
        }

        endContent(Modifier.align(Alignment.CenterVertically))
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
