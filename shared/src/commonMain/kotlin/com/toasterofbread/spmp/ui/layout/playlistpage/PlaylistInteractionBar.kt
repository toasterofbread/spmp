package com.toasterofbread.spmp.ui.layout.playlistpage

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.toasterofbread.spmp.model.mediaitem.Playlist
import com.toasterofbread.spmp.model.mediaitem.playlist.PlaylistEditor
import com.toasterofbread.spmp.platform.LargeDropdownMenu
import kotlinx.coroutines.launch

@Composable
internal fun InteractionBar(
    list_state: LazyListState,
    playlist: Playlist,
    playlist_editor: PlaylistEditor?,
    reorderable: Boolean,
    setReorderable: (Boolean) -> Unit,
    filter: String?,
    setFilter: (String?) -> Unit,
    sort_option: SortOption,
    setSortOption: (SortOption) -> Unit,
    modifier: Modifier = Modifier
) {
    // 0 -> search, 1 -> sort
    var opened_menu: Int? by remember { mutableStateOf(null) }
    val coroutine_scope = rememberCoroutineScope()

    Row(modifier) {
        // Filter button
        IconButton(
            {
                if (opened_menu == 0) opened_menu = null
                else opened_menu = 0
            },
            enabled = !reorderable
        ) {
            Crossfade(opened_menu == 0) { searching ->
                Icon(if (searching) Icons.Default.Done else Icons.Default.Search, null)
            }
        }

        // Animate between filter bar and remaining buttons
        Box(Modifier.fillMaxWidth().weight(1f)) {
            this@Row.AnimatedVisibility(opened_menu != 0) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    // Sort
                    IconButton(
                        {
                            if (opened_menu == 1) opened_menu = null
                            else opened_menu = 1
                        },
                        enabled = !reorderable
                    ) {
                        Icon(Icons.Default.Sort, null)
                    }

                    Spacer(Modifier.fillMaxWidth().weight(1f))

                    if (playlist_editor != null) {
                        // Reorder
                        IconButton({ setReorderable(!reorderable) }) {
                            Crossfade(reorderable) { reordering ->
                                Icon(if (reordering) Icons.Default.Done else Icons.Default.Reorder, null)
                            }
                        }
                        // Add
                        IconButton({ TODO() }) {
                            Icon(Icons.Default.Add, null)
                        }
                    }
                }
            }
            this@Row.AnimatedVisibility(opened_menu == 0) {
                InteractionBarFilterBox(filter, setFilter, Modifier.fillMaxWidth())
            }
        }

        AnimatedVisibility(opened_menu != 0) {
            Row {
                Crossfade(list_state.canScrollBackward) { enabled ->
                    IconButton(
                        { coroutine_scope.launch {
                            list_state.scrollToItem(0)
                        } },
                        enabled = enabled
                    ) {
                        Icon(Icons.Default.ArrowUpward, null)
                    }
                }
                Crossfade(list_state.canScrollForward) { enabled ->
                    IconButton(
                        { coroutine_scope.launch {
                            list_state.scrollToItem(Int.MAX_VALUE)
                        } },
                        enabled = enabled
                    ) {
                        Icon(Icons.Default.ArrowDownward, null)
                    }
                }
            }
        }
    }

    // Sort options
    LargeDropdownMenu(
        opened_menu == 1,
        { if (opened_menu == 1) opened_menu = null },
        SortOption.values().size,
        sort_option.ordinal,
        { SortOption.values()[it].getReadable() }
    ) {
        setSortOption(SortOption.values()[it])
        opened_menu = null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InteractionBarFilterBox(filter: String?, setFilter: (String?) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier) {
        TextField(
            filter ?: "",
            { setFilter(it.ifEmpty { null }) },
            Modifier.fillMaxWidth().weight(1f),
            singleLine = true
        )

        IconButton(
            { setFilter(null) },
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = Color.Transparent,
                contentColor = LocalContentColor.current
            )
        ) {
            Icon(Icons.Default.Close, null)
        }
    }
}
