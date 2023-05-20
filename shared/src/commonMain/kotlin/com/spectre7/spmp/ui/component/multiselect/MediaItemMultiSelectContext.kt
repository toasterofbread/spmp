package com.spectre7.spmp.ui.component.multiselect

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.spectre7.spmp.PlayerServiceHost
import com.spectre7.spmp.model.MediaItem
import com.spectre7.spmp.model.MediaItemType
import com.spectre7.spmp.model.Settings
import com.spectre7.spmp.model.Song
import com.spectre7.spmp.resources.getString
import com.spectre7.utils.getContrasted
import com.spectre7.utils.lazyAssert
import com.spectre7.utils.setAlpha

class MediaItemMultiSelectContext(
    val allow_songs: Boolean = true,
    val allow_artists: Boolean = true,
    val allow_playlists: Boolean = true,
    val nextRowSelectedItemActions: (@Composable ColumnScope.(MediaItemMultiSelectContext) -> Unit)? = null,
    val selectedItemActions: (@Composable RowScope.(MediaItemMultiSelectContext) -> Unit)? = null
) {
    private val selected_items: MutableList<Pair<MediaItem, Int?>> = mutableStateListOf()
    var is_active: Boolean by mutableStateOf(false)
        private set

    @Composable
    fun getActiveHintBorder(): BorderStroke? = if (is_active) BorderStroke(hint_path_thickness, LocalContentColor.current) else null
    private val hint_path_thickness = 0.5.dp

    fun setActive(value: Boolean) {
        if (value) {
            selected_items.clear()
        }
        is_active = value
    }

    fun isItemSelected(item: MediaItem, key: Int? = null): Boolean {
        if (!is_active) {
            return false
        }
        return selected_items.any { it.first == item && it.second == key }
    }

    fun onActionPerformed() {
        if (Settings.KEY_MULTISELECT_CANCEL_ON_ACTION.get()) {
            setActive(false)
        }
    }

    fun getSelectedItems(): List<Pair<MediaItem, Int?>> = selected_items
    fun getUniqueSelectedItems(): Set<MediaItem> = selected_items.map { it.first }.toSet()

    fun updateKey(index: Int, key: Int?) {
        selected_items[index] = selected_items[index].copy(second = key)
        lazyAssert(condition = ::areItemsValid)
    }

    fun toggleItem(item: MediaItem, key: Int? = null) {
        val allowed = when (item.type) {
            MediaItemType.SONG -> allow_songs
            MediaItemType.ARTIST -> allow_artists
            MediaItemType.PLAYLIST -> allow_playlists
        }

        if (!allowed) {
            return
        }

        val iterator = selected_items.iterator()
        while (iterator.hasNext()) {
            iterator.next().apply {
                if (first == item && second == key) {
                    iterator.remove()
                    return
                }
            }
        }

        selected_items.add(Pair(item, key))
    }

    fun setItemSelected(item: MediaItem, selected: Boolean, key: Int? = null) {
        if (selected) {
            if (selected_items.any { it.first == item && it.second == key }) {
                return
            }
            selected_items.add(Pair(item, key))
        }
        else {
            selected_items.removeIf { it.first == item && it.second == key }
        }
    }

    @Composable
    fun SelectableItemOverlay(item: MediaItem, modifier: Modifier = Modifier, key: Int? = null) {
        val selected by remember(item, key) { derivedStateOf { isItemSelected(item, key) } }
        val background_colour = LocalContentColor.current.getContrasted().setAlpha(0.5f)

        AnimatedVisibility(selected, modifier, enter = fadeIn(), exit = fadeOut()) {
            Box(Modifier.fillMaxSize().background(background_colour), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Check, null)
            }
        }
    }

    @Composable
    fun InfoDisplay(modifier: Modifier = Modifier) {
        DisposableEffect(Unit) {
            onDispose {
                if (!is_active) {
                    selected_items.clear()
                }
            }
        }

        Column(modifier.fillMaxWidth().animateContentSize()) {
            Text(
                getString("multiselect_x_items_selected").replace("\$x", selected_items.size.toString()), 
                style = MaterialTheme.typography.labelLarge
            )
            Divider(Modifier.padding(top = 5.dp), color = LocalContentColor.current, thickness = hint_path_thickness)

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                GeneralSelectedItemActions()

                Spacer(Modifier.fillMaxWidth().weight(1f))
                selectedItemActions?.invoke(this, this@MediaItemMultiSelectContext)
                Spacer(Modifier.fillMaxWidth().weight(1f))

                IconButton({ selected_items.clear() }) {
                    Icon(Icons.Default.Refresh, null)
                }
                IconButton({ setActive(false) }) {
                    Icon(Icons.Default.Close, null)
                }
            }

            nextRowSelectedItemActions?.invoke(this, this@MediaItemMultiSelectContext)
        }
    }

    @Composable
    private fun GeneralSelectedItemActions() {
        val all_pinned by remember { derivedStateOf {
            getUniqueSelectedItems().all { it.pinned_to_home }
        } }

        IconButton({
            all_pinned.also { pinned ->
                for (item in getUniqueSelectedItems()) {
                    item.setPinnedToHome(!pinned)
                }
            }
            onActionPerformed()
        }) {
            Icon(if (all_pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin, null)
        }

        IconButton({
            for (item in getUniqueSelectedItems()) {
                if (item is Song) {
                    PlayerServiceHost.download_manager.startDownload(item.id)
                }
            }
            onActionPerformed()
        }) {
            Icon(Icons.Default.Download, null)
        }
    }

    @Composable
    fun CollectionToggleButton(items: List<MediaItem>) {
        AnimatedVisibility(is_active) {
            val all_selected by remember { derivedStateOf { items.all { isItemSelected(it) } } }
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
        val keys = mutableMapOf<MediaItem, MutableList<Int?>>()
        for (item in selected_items) {
            val item_keys = keys[item.first]
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
