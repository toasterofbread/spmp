package com.spectre7.spmp.ui.component.multiselect

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.spectre7.spmp.model.MediaItem
import com.spectre7.spmp.model.Settings
import com.spectre7.spmp.resources.getStringTODO
import com.spectre7.utils.getContrasted
import com.spectre7.utils.setAlpha

class MediaItemMultiSelectContext(
    val selectedItemActions: @Composable RowScope.(MediaItemMultiSelectContext, onActionPerformed: () -> Unit) -> Unit,
    val allow_songs: Boolean = true,
    val allow_artists: Boolean = true,
    val allow_playlists: Boolean = true
) {
    private val selected_items: MutableList<Pair<MediaItem, Int?>> = mutableStateListOf()
    var is_active: Boolean by mutableStateOf(false)
        private set

    fun setActive(value: Boolean) {
        if (value) {
            selected_items.clear()
        }
        is_active = value
    }

    fun isItemSelected(item: MediaItem, key: Int? = null): Boolean {
        return selected_items.any { it.first == item && it.second == key }
    }

    fun getSelectedItems(): List<Pair<MediaItem, Int?>> = selected_items

    fun toggleItem(item: MediaItem, key: Int? = null) {
        val allowed = when (item.type) {
            MediaItem.Type.SONG -> allow_songs
            MediaItem.Type.ARTIST -> allow_artists
            MediaItem.Type.PLAYLIST -> allow_playlists
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
    fun InfoDisplay(modifier: Modifier) {
        DisposableEffect(Unit) {
            onDispose {
                if (!is_active) {
                    selected_items.clear()
                }
            }
        }

        Column(modifier.fillMaxWidth()) {
            Text(getStringTODO("${selected_items.size} items selected"), style = MaterialTheme.typography.labelLarge)
            Divider(Modifier.fillMaxWidth().padding(top = 5.dp))

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                selectedItemActions(this@MediaItemMultiSelectContext) {
                    if (Settings.KEY_MULTISELECT_CANCEL_ON_ACTION.get()) {
                        setActive(false)
                    }
                }

                Spacer(Modifier.fillMaxWidth().weight(1f))

                IconButton({ selected_items.clear() }) {
                    Icon(Icons.Default.Refresh, null)
                }
                IconButton({ setActive(false) }) {
                    Icon(Icons.Default.Close, null)
                }
            }
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
}
