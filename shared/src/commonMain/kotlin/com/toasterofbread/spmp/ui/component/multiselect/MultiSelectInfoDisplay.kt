package com.toasterofbread.spmp.ui.component.multiselect

import LocalPlayerState
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Expand
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import dev.toastbits.composekit.components.platform.composable.BackHandler
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.ui.component.multiselect_context.MultiSelectGeneralActions
import com.toasterofbread.spmp.ui.component.multiselect_context.MultiSelectOverflowActions
import com.toasterofbread.spmp.ui.component.WaveBorder
import dev.toastbits.ytmkt.model.external.mediaitem.YtmMediaItem
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.multiselect_x_items_selected
import spmp.shared.generated.resources.action_close

@Composable
internal fun MediaItemMultiSelectContext.MultiSelectInfoDisplay(
    modifier: Modifier = Modifier,
    content_modifier: Modifier = Modifier,
    getAllItems: (() -> List<List<MultiSelectItem>>)?,
    wrapContent: @Composable (@Composable () -> Unit) -> Unit,
    show_alt_content: Boolean = true,
    altContent: (@Composable () -> Unit)? = null
): Boolean {
    BackHandler(is_active) {
        setActive(false)
    }

    val show: Boolean = is_active || (altContent != null && show_alt_content)

    AnimatedVisibility(
        show,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        wrapContent {
            Crossfade(is_active || altContent == null || !show_alt_content, modifier) { active ->
                if (active) {
                    MultiSelectInfoDisplayContent(content_modifier, getAllItems = getAllItems)
                }
                else {
                    altContent?.invoke()
                }
            }
        }
    }

    return show
}

@Composable
fun MediaItemMultiSelectContext.MultiSelectInfoDisplayContent(
    modifier: Modifier,
    background_colour: Color = LocalPlayerState.current.theme.background,
    getAllItems: (() -> List<List<MultiSelectItem>>)? = null
) {
    Column(modifier.animateContentSize()) {
        val title_text: String = stringResource(Res.string.multiselect_x_items_selected).replace("\$x", selected_items.size.toString())

        var wave_border_offset: Float by remember { mutableStateOf(0f) }
        LaunchedEffect(Unit) {
            val update_interval: Long = 1000 / 30
            while (true) {
                wave_border_offset += update_interval * 0.02f
                delay(update_interval)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                title_text,
                style = MaterialTheme.typography.labelLarge
            )

            WaveBorder(
                Modifier.fillMaxWidth().weight(1f),
                waves = 4,
                border_thickness = hint_path_thickness + 1.dp,
                border_colour = LocalContentColor.current.copy(alpha = 0.5f),
                getColour = { background_colour },
                getOffset = { 0f },
                getWaveOffset = { wave_border_offset },
                width_multiplier = 2f,
                clip_content = true
            )

            // Select between
            AnimatedVisibility(ordered_selectable_items.isNotEmpty() || getAllItems != null) {
                IconButton(
                    {
                        val selectable_items = getAllItems?.invoke() ?: getAllSelectableItems()
                        for (item in getItemsBetweenSelectableItems(selectable_items)) {
                            setItemSelected(item, true)
                        }
                    },
                    // Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.Expand, null)
                }
            }

            // Select all
            AnimatedVisibility(ordered_selectable_items.isNotEmpty() || getAllItems != null) {
                IconButton(
                    {
                        val selectable_items: List<List<MultiSelectItem>> =
                            getAllItems?.invoke() ?: getAllSelectableItems()

                        var changed: Boolean = false
                        for (items in selectable_items) {
                            for (item in items) {
                                if (setItemSelected(item, true)) {
                                    changed = true
                                }
                            }
                        }

                        if (!changed) {
                            selected_items.clear()
                        }
                    },
                    // Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.SelectAll, null)
                }
            }
        }

        // Divider(Modifier.padding(top = 5.dp), color = local_content_colour, thickness = hint_path_thickness)

        Row(
            Modifier.height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            var show_overflow_menu: Boolean by remember { mutableStateOf(false) }

            MultiSelectGeneralActions(this@MultiSelectInfoDisplayContent)

            Spacer(Modifier.fillMaxWidth().weight(1f))

            // Open overflow menu
            IconButton({ show_overflow_menu = !show_overflow_menu }) {
                Icon(Icons.Default.MoreVert, null)
            }

            // Close
            IconButton({ setActive(false) }) {
                Icon(Icons.Default.Close, null)
            }

            if (show_overflow_menu) {
                AlertDialog(
                    onDismissRequest = { show_overflow_menu = false },
                    confirmButton = {
                        Button({ show_overflow_menu = false }) {
                            Text(stringResource(Res.string.action_close))
                        }
                    },
                    title = {
                        Text(title_text)
                    },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MultiSelectOverflowActions(this@MultiSelectInfoDisplayContent, additionalSelectedItemActions)
                        }
                    }
                )
            }
        }

        MultiSelectNextRowActions(this@MultiSelectInfoDisplayContent)
    }
}

private fun MediaItemMultiSelectContext.getAllSelectableItems(): List<List<MultiSelectItem>> =
    ordered_selectable_items

private fun MediaItemMultiSelectContext.getItemsBetweenSelectableItems(selectable_items: List<List<MultiSelectItem>>): List<MultiSelectItem> {
    val selected: Set<YtmMediaItem> = getUniqueSelectedItems()

    return selectable_items.flatMap { items ->
        var max: Int = -1
        var min: Int = -1
        for (item in selected) {
            val index: Int = items.indexOfFirst { it.first.id == item.id }
            if (index == -1) {
                continue
            }

            if (max == -1) {
                max = index
                min = index
            }
            else if (index > max) {
                max = index
            }
            else if (index < min) {
                min = index
            }
        }

        if (max == -1) {
            return@flatMap emptyList()
        }

        return@flatMap items.subList(min, max + 1)
    }
}
