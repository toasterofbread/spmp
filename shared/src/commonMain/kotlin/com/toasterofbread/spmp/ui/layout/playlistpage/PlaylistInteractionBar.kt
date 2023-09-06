package com.toasterofbread.spmp.ui.layout.playlistpage

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.toasterofbread.spmp.model.mediaitem.MediaItemSortType
import com.toasterofbread.spmp.ui.component.WaveBorder
import com.toasterofbread.utils.composable.SubtleLoadingIndicator
import com.toasterofbread.utils.composable.ResizableOutlinedTextField
import kotlinx.coroutines.launch

@Composable
internal fun PlaylistPage.PlaylistInteractionBar(
    list_state: LazyListState,
    loading: Boolean,
    modifier: Modifier = Modifier
) {
    val coroutine_scope = rememberCoroutineScope()

    // 0 -> search, 1 -> sort
    var opened_menu: Int? by remember { mutableStateOf(null) }

    Column(modifier) {
        Crossfade(multiselect_context.is_active) { selecting ->
            if (selecting) {
                multiselect_context.InfoDisplay()
            }
            else {
                Row {
                    // Filter button
                    IconButton(
                        {
                            if (opened_menu == 0) opened_menu = null
                            else opened_menu = 0
                        },
                        enabled = !reordering
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
                                    enabled = !reordering
                                ) {
                                    Icon(Icons.Default.Sort, null)
                                }

                                Spacer(Modifier.fillMaxWidth().weight(1f))

                                AnimatedVisibility(loading) {
                                    SubtleLoadingIndicator()
                                }

                                playlist_editor?.also { editor ->
                                    // Reorder
                                    AnimatedVisibility(editor.canMoveItems()) {
                                        IconButton({
                                            if (editor.canMoveItems()) {
                                                setReorderable(!reordering)
                                            }
                                        }) {
                                            Crossfade(reordering) { reordering ->
                                                Icon(if (reordering) Icons.Default.Done else Icons.Default.Reorder, null)
                                            }
                                        }
                                    }

                                    AnimatedVisibility(editor.canAddItems()) {
                                        IconButton({
                                            if (editor.canAddItems()) {
                                                TODO()
                                            }
                                        }) {
                                            Icon(Icons.Default.Add, null)
                                        }
                                    }
                                }
                            }
                        }
                        this@Row.AnimatedVisibility(opened_menu == 0) {
                            InteractionBarFilterBox(current_filter, ::setCurrentFilter, Modifier.fillMaxWidth())
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
            }
        }

        WaveBorder(Modifier.fillMaxWidth())
    }

    MediaItemSortType.SelectionMenu(
        opened_menu == 1,
        sort_type,
        { opened_menu = null },
        { setSortType(it) },
        "sort_option_playlist"
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InteractionBarFilterBox(filter: String?, setFilter: (String?) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier) {
        ResizableOutlinedTextField(
            filter ?: "",
            { setFilter(it.ifEmpty { null }) },
            Modifier.height(45.dp).fillMaxWidth().weight(1f),
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
