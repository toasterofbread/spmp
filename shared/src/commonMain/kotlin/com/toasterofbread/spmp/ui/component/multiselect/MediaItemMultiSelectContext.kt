package com.toasterofbread.spmp.ui.component.multiselect

import LocalPlayerState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toasterofbread.composekit.platform.composable.BackHandler
import com.toasterofbread.composekit.utils.common.getContrasted
import com.toasterofbread.composekit.utils.common.lazyAssert
import com.toasterofbread.composekit.platform.composable.ScrollabilityIndicatorRow
import com.toasterofbread.composekit.utils.composable.ShapedIconButton
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemHolder
import com.toasterofbread.spmp.model.mediaitem.db.observePinnedToHome
import com.toasterofbread.spmp.model.mediaitem.db.setPinned
import com.toasterofbread.spmp.model.mediaitem.library.MediaItemLibrary
import com.toasterofbread.spmp.model.mediaitem.library.createLocalPlaylist
import com.toasterofbread.spmp.model.mediaitem.playlist.LocalPlaylist
import com.toasterofbread.spmp.model.mediaitem.playlist.Playlist
import com.toasterofbread.spmp.model.mediaitem.playlist.PlaylistEditor.Companion.getEditorOrNull
import com.toasterofbread.spmp.model.mediaitem.playlist.PlaylistEditor.Companion.isPlaylistEditable
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.settings.category.BehaviourSettings
import com.toasterofbread.spmp.platform.download.DownloadStatus
import com.toasterofbread.spmp.platform.download.rememberSongDownloads
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.component.multiselect_context.MultiSelectGeneralActions
import com.toasterofbread.spmp.ui.component.multiselect_context.MultiSelectOverflowActions
import com.toasterofbread.spmp.ui.layout.PlaylistSelectMenu
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.getOrReport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

class MediaItemMultiSelectContext(
    val additionalSelectedItemActions: (@Composable ColumnScope.(MediaItemMultiSelectContext) -> Unit)? = null
) {
    internal val selected_items: MutableList<Pair<MediaItem, Int?>> = mutableStateListOf()
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
        if (BehaviourSettings.Key.MULTISELECT_CANCEL_ON_ACTION.get()) {
            setActive(false)
        }
    }

    private fun onSelectedItemsChanged() {
        if (selected_items.isEmpty() && BehaviourSettings.Key.MULTISELECT_CANCEL_ON_NONE_SELECTED.get()) {
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

    fun setItemSelected(item: MediaItem, selected: Boolean, key: Int? = null): Boolean {
        if (selected) {
            if (!is_active) {
                setActive(true)
            }

            if (selected_items.any { it.first == item && it.second == key }) {
                return false
            }
            selected_items.add(Pair(item, key))
            onSelectedItemsChanged()
            return true
        }
        else if (selected_items.removeIf { it.first == item && it.second == key }) {
            onSelectedItemsChanged()
            return true
        }
        else {
            return false
        }
    }

    @Composable
    fun SelectableItemOverlay(item: MediaItem, modifier: Modifier = Modifier, key: Int? = null) {
        val selected by remember(item, key) { derivedStateOf { isItemSelected(item, key) } }
        val background_colour = LocalContentColor.current.getContrasted().copy(alpha = 0.5f)

        AnimatedVisibility(selected, modifier, enter = fadeIn(), exit = fadeOut()) {
            Box(Modifier.background(background_colour), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Check, null)
            }
        }
    }

    @Composable
    fun InfoDisplay(modifier: Modifier = Modifier, getAllSelectableItems: (() -> List<Pair<MediaItem, Int?>>)? = null) {
        DisposableEffect(Unit) {
            onDispose {
                if (!is_active) {
                    selected_items.clear()
                }
            }
        }

        BackHandler(is_active) {
            setActive(false)
        }

        Column(modifier.fillMaxWidth().animateContentSize()) {
            val title_text: String = getString("multiselect_x_items_selected").replace("\$x", selected_items.size.toString())
            
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    title_text, 
                    style = MaterialTheme.typography.labelLarge
                )

                getAllSelectableItems?.also { getSelectable ->
                    IconButton(
                        {
                            var changed = false
                            for (item in getSelectable()) {
                                if (setItemSelected(item.first, true, item.second)) {
                                    changed = true
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
                Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                var show_overflow_menu: Boolean by remember { mutableStateOf(false) }
                
                MultiSelectGeneralActions(this@MediaItemMultiSelectContext)

                Spacer(Modifier.fillMaxWidth().weight(1f))

                IconButton({ show_overflow_menu = !show_overflow_menu }) {
                    Icon(Icons.Default.MoreVert, null)
                }

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
                                MultiSelectOverflowActions(this@MediaItemMultiSelectContext, additionalSelectedItemActions)
                            }
                        }
                    )
                }
            }

            MultiSelectNextRowActions(this@MediaItemMultiSelectContext)
        }
    }

    @Composable
    fun CollectionToggleButton(items: List<MediaItemHolder>) {
        AnimatedVisibility(is_active, enter = expandHorizontally(), exit = shrinkHorizontally()) {
            val all_selected by remember { derivedStateOf {
                items.all {
                    it.item?.let { item -> isItemSelected(item) } ?: false
                }
            } }
            Crossfade(all_selected) { selected ->
                IconButton({
                    for (item in items) {
                        item.item?.also {
                            setItemSelected(it, !selected)
                        }
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
