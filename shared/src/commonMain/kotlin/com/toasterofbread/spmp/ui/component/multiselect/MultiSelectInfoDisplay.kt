package com.toasterofbread.spmp.ui.component.multiselect

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
import com.toasterofbread.composekit.platform.composable.BackHandler
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.component.multiselect_context.MultiSelectGeneralActions
import com.toasterofbread.spmp.ui.component.multiselect_context.MultiSelectOverflowActions

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
                    MultiSelectInfoDisplayContent(content_modifier, getAllItems)
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
fun MediaItemMultiSelectContext.MultiSelectInfoDisplayContent(modifier: Modifier, getAllItems: (() -> List<List<MultiSelectItem>>)? = null) {
    Column(modifier.animateContentSize()) {
        val title_text: String = getString("multiselect_x_items_selected").replace("\$x", selected_items.size.toString())

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                title_text, 
                style = MaterialTheme.typography.labelLarge
            )

            Spacer(Modifier.fillMaxWidth().weight(1f))

            // Select between
            AnimatedVisibility(ordered_selectable_items.isNotEmpty() || getAllItems != null) {
                IconButton(
                    {
                        val selectable_items = getAllItems?.invoke() ?: getAllSelectableItems()
                        for (item in getItemsBetweenSelectableItems(selectable_items)) {
                            setItemSelected(item, true)
                        }
                    },
                    Modifier.size(24.dp)
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
                    Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.SelectAll, null)
                }
            }
        }

        Divider(Modifier.padding(top = 5.dp), color = LocalContentColor.current, thickness = hint_path_thickness)

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
                            Text(getString("action_close"))
                        }
                    },
                    title = {
                        Text(title_text)
                    },
                    text = {
                        Column {
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
    ordered_selectable_items.map { items -> items.mapNotNull { it.item?.let { MultiSelectItem(it, null) } } }

private fun MediaItemMultiSelectContext.getItemsBetweenSelectableItems(selectable_items: List<List<MultiSelectItem>>): List<MultiSelectItem> {
    val selected: Set<MediaItem> = getUniqueSelectedItems()

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
