package com.toasterofbread.spmp.ui.component.multiselect

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.toastbits.composekit.util.getContrasted
import dev.toastbits.composekit.util.platform.lazyAssert
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemHolder
import com.toasterofbread.spmp.platform.AppContext
import dev.toastbits.ytmkt.model.external.mediaitem.YtmMediaItem
import kotlinx.coroutines.launch

typealias MultiSelectItem = Pair<YtmMediaItem, Int?>

open class MediaItemMultiSelectContext {
    val additionalSelectedItemActions: (@Composable ColumnScope.(MediaItemMultiSelectContext) -> Unit)?
    val context: AppContext

    constructor(context: AppContext) {
        this.context = context
        additionalSelectedItemActions = null
    }

    constructor(context: AppContext, additionalSelectedItemActions: (@Composable ColumnScope.(MediaItemMultiSelectContext) -> Unit)?) {
        this.context = context
        this.additionalSelectedItemActions = additionalSelectedItemActions
    }

    internal val selected_items: MutableList<MultiSelectItem> = mutableStateListOf()
    var is_active: Boolean by mutableStateOf(false)
        private set

    internal val ordered_selectable_items: MutableList<List<MultiSelectItem>> = mutableStateListOf()

    @Composable
    fun getActiveHintBorder(): BorderStroke? = if (is_active) BorderStroke(hint_path_thickness, LocalContentColor.current) else null
    internal val hint_path_thickness = 0.5.dp

    fun setActive(value: Boolean) {
        if (value) {
            selected_items.clear()
        }
        is_active = value
    }

    fun isItemSelected(item: YtmMediaItem, key: Int? = null): Boolean {
        if (!is_active) {
            return false
        }
        return selected_items.any { it.first.id == item.id && it.second == key }
    }

    fun onActionPerformed() {
        context.coroutineScope.launch {
            if (context.settings.Behaviour.MULTISELECT_CANCEL_ON_ACTION.get()) {
                setActive(false)
            }
        }
    }

    private fun onSelectedItemsChanged() {
        context.coroutineScope.launch {
            if (selected_items.isEmpty() && context.settings.Behaviour.MULTISELECT_CANCEL_ON_NONE_SELECTED.get()) {
                setActive(false)
            }
        }
    }

    fun getSelectedItems(): List<MultiSelectItem> = selected_items
    fun getUniqueSelectedItems(): Set<YtmMediaItem> = selected_items.map { it.first }.distinctBy { it.id }.toSet()

    fun updateKey(index: Int, key: Int?) {
        selected_items[index] = selected_items[index].copy(second = key)
        lazyAssert(condition = ::areItemsValid)
    }

    fun toggleItem(item: MediaItem, key: Int? = null) {
        val iterator = selected_items.iterator()
        while (iterator.hasNext()) {
            iterator.next().apply {
                if (first == item && second == key) {
                    iterator.remove()
                    onSelectedItemsChanged()
                    return
                }
            }
        }

        selected_items.add(Pair(item, key))
        onSelectedItemsChanged()
    }

    fun setItemSelected(item: MultiSelectItem, selected: Boolean): Boolean {
        if (selected) {
            if (selected_items.any { it.first.id == item.first.id && it.second == item.second }) {
                return false
            }

            if (!is_active) {
                setActive(true)
            }

            selected_items.add(item)
            onSelectedItemsChanged()

            return true
        }
        else if (selected_items.removeAll { it.first == item.first && it.second == item.second }) {
            onSelectedItemsChanged()
            return true
        }
        else {
            return false
        }
    }

    fun setItemSelected(item: YtmMediaItem, selected: Boolean, key: Int? = null): Boolean =
        setItemSelected(MultiSelectItem(item, key), selected)

    @Composable
    fun SelectableItemOverlay(item: MediaItem, modifier: Modifier = Modifier, key: Int? = null) {
        val selected: Boolean by remember(item, key) { derivedStateOf { isItemSelected(item, key) } }
        val background_colour: Color = LocalContentColor.current.getContrasted().copy(alpha = 0.5f)

        AnimatedVisibility(selected, modifier, enter = fadeIn(), exit = fadeOut()) {
            Box(Modifier.background(background_colour), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Check, null)
            }
        }
    }

    @Composable
    protected open fun InfoDisplayContent(
        modifier: Modifier,
        content_modifier: Modifier,
        getAllItems: (() -> List<List<MultiSelectItem>>)?,
        wrapContent: @Composable (@Composable () -> Unit) -> Unit,
        show_alt_content: Boolean,
        altContent: (@Composable () -> Unit)?
    ): Boolean =
        MultiSelectInfoDisplay(modifier, content_modifier, getAllItems, wrapContent, show_alt_content, altContent)

    @Composable
    fun InfoDisplay(
        modifier: Modifier = Modifier,
        content_modifier: Modifier = Modifier,
        getAllItems: (() -> List<List<MultiSelectItem>>)? = null,
        wrapContent: @Composable (@Composable () -> Unit) -> Unit = { it() },
        show_alt_content: Boolean = true,
        altContent: (@Composable () -> Unit)? = null
    ): Boolean {
        DisposableEffect(Unit) {
            onDispose {
                if (!is_active) {
                    selected_items.clear()
                }
            }
        }

        return InfoDisplayContent(modifier, content_modifier, getAllItems, wrapContent, show_alt_content, altContent)
    }

    @Composable
    fun CollectionToggleButton(
        items: Collection<MediaItemHolder>,
        ordered: Boolean = true,
        show: Boolean = true,
        enter: EnterTransition = expandHorizontally(),
        exit: ExitTransition = shrinkHorizontally()
    ) {
        CollectionToggleButton(
            remember(items) { items.mapNotNull { it.item?.let { MultiSelectItem(it, null) } } },
            ordered,
            show,
            enter,
            exit
        )
    }

    @Composable
    fun CollectionToggleButton(
        items: List<MultiSelectItem>,
        ordered: Boolean = true,
        show: Boolean = true,
        enter: EnterTransition = expandHorizontally(),
        exit: ExitTransition = shrinkHorizontally()
    ) {
        DisposableEffect(items, true) {
            if (ordered) {
                ordered_selectable_items.add(items)
            }

            onDispose {
                ordered_selectable_items.remove(items)
            }
        }

        AnimatedVisibility(show && is_active, enter = enter, exit = exit) {
            val all_selected: Boolean by remember { derivedStateOf {
                items.isNotEmpty() && items.all {
                    isItemSelected(it.first, it.second)
                }
            } }
            Crossfade(all_selected) { selected ->
                IconButton({
                    for (item in items) {
                        setItemSelected(item, !selected)
                    }
                }) {
                    Icon(if (selected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked, null)
                }
            }
        }
    }

    private fun areItemsValid(): Boolean {
        val keys: MutableMap<YtmMediaItem, MutableList<Int?>> = mutableMapOf()
        for (item in selected_items) {
            val item_keys: MutableList<Int?>? = keys[item.first]
            if (item_keys == null) {
                keys[item.first] = mutableListOf(item.second)
                continue
            }

            if (item_keys.contains(item.second)) {
                return false
            }
            item_keys.add(item.second)
        }
        return true
    }
}
