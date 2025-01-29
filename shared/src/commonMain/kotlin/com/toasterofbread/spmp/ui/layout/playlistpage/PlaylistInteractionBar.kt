package com.toasterofbread.spmp.ui.layout.playlistpage

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.toastbits.composekit.components.utils.composable.SubtleLoadingIndicator
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemSortType
import com.toasterofbread.spmp.ui.component.WaveBorder
import dev.toastbits.composekit.components.utils.modifier.horizontal
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
internal fun PlaylistAppPage.PlaylistInteractionBar(
    items: List<MediaItem>?,
    list_state: LazyListState,
    loading: Boolean,
    modifier: Modifier = Modifier
) {
    val coroutine_scope = rememberCoroutineScope()

    // 0 -> search, 1 -> sort
    var opened_menu: Int? by remember { mutableStateOf(null) }

    Column(modifier) {
        multiselect_context.InfoDisplay(
            getAllItems = {
                listOfNotNull(items?.map { Pair(it, null) })
            },
            altContent = {
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

                                if (playlist_editor != null) {
                                    // Reorder
                                    IconButton({
                                        setReorderable(!reordering)
                                    }) {
                                        Crossfade(reordering) { reordering ->
                                            Icon(if (reordering) Icons.Default.Done else Icons.Default.Reorder, null)
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
                            IconButton({
                                for (item in items ?: emptyList()) {
                                    multiselect_context.setItemSelected(item, true)
                                }
                            }) {
                                Icon(Icons.Default.SelectAll, null)
                            }

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
        )

        WaveBorder(Modifier.fillMaxWidth())
    }

    MediaItemSortType.SelectionMenu(
        opened_menu == 1,
        sort_type,
        { opened_menu = null },
        { setSortType(it) },
        "sort_type_playlist"
    )
}

@Composable
private fun InteractionBarFilterBox(filter: String?, setFilter: (String?) -> Unit, modifier: Modifier = Modifier) {
    val state: TextFieldState = remember { TextFieldState(filter ?: "") }
    LaunchedEffect(state) {
        snapshotFlow { state.text }
            .collectLatest {
                setFilter(it.toString().ifEmpty { null })
            }
    }

    Row(modifier) {
        OutlinedTextField(
            state,
            Modifier.height(45.dp).fillMaxWidth().weight(1f),
            lineLimits = TextFieldLineLimits.SingleLine,
            contentPadding = OutlinedTextFieldDefaults.contentPadding().horizontal
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
