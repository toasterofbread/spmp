package com.spectre7.spmp.ui.component

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.spectre7.spmp.model.MediaItem
import com.spectre7.spmp.ui.layout.PlayerViewContext
import com.spectre7.spmp.ui.theme.Theme
import com.spectre7.utils.WidthShrinkText
import com.spectre7.utils.copy

data class MediaItemLayout(
    val title: String?,
    val subtitle: String?,
    val type: Type? = null,
    val items: MutableList<MediaItem> = mutableListOf(),
    val thumbnail_provider: MediaItem.ThumbnailProvider? = null // TODO
) {
    enum class Type {
        GRID,
        LIST,
        NUMBERED_LIST
    }

    fun addItem(item: MediaItem) {
        if (items.any { it.id == item.id }) {
            return
        }
        items.add(item)
        return
    }

    companion object {
        @Composable
        fun ItemPreview(
            item: MediaItem,
            width: Dp,
            animate: MutableState<Boolean>?,
            playerProvider: () -> PlayerViewContext,
            modifier: Modifier = Modifier
        ) {
            Box(modifier.requiredWidth(width), contentAlignment = Alignment.Center) {
                if(animate?.value == true) {
                    LaunchedEffect(Unit) {
                        animate.value = false
                    }

                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(visible) {
                        visible = true
                    }
                    AnimatedVisibility(
                        visible,
                        enter = fadeIn() + expandIn(expandFrom = Alignment.Center),
                        exit = fadeOut() + shrinkOut(shrinkTowards = Alignment.Center)
                    ) {
                        item.PreviewSquare(Theme.current.on_background_provider, playerProvider, true, Modifier)
                    }
                }
                else {
                    item.PreviewSquare(Theme.current.on_background_provider, playerProvider, true, Modifier)
                }
            }
        }
    }
}

fun Collection<MediaItemLayout>.removeInvalid() {
    for (layout in this) {
        layout.items.removeAll { !it.is_valid }
    }
}

@Composable
fun MediaItemLayoutColumn(
    layouts: List<MediaItemLayout>,
    playerProvider: () -> PlayerViewContext,
    modifier: Modifier = Modifier
) {
    for (layout in layouts) {
        assert(layout.type != null)
    }

    Column(modifier) {
        for (layout in layouts) {
            when (layout.type!!) {
                MediaItemLayout.Type.GRID -> MediaItemGrid(layout, playerProvider)
                MediaItemLayout.Type.LIST -> MediaItemList(layout, false, playerProvider)
                MediaItemLayout.Type.NUMBERED_LIST -> MediaItemList(layout, true, playerProvider)
            }
        }
    }
}

@Composable
fun LazyMediaItemLayoutColumn(
    layouts: List<MediaItemLayout>,
    playerProvider: () -> PlayerViewContext,
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(0.dp),
    onContinuationRequested: (() -> Unit)? = null,
    loading_continuation: Boolean = false,
    continuation_alignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    scroll_state: LazyListState = rememberLazyListState(),
    vertical_arrangement: Arrangement.Vertical = Arrangement.Top,
    getType: ((MediaItemLayout) -> MediaItemLayout.Type)? = null
) {
    require(getType != null || layouts.all { it.type != null })

    LazyColumn(
        modifier,
        state = scroll_state,
        contentPadding = padding.copy(bottom = 0.dp),
        verticalArrangement = vertical_arrangement
    ) {
        items(layouts) { layout ->
            when (getType?.invoke(layout) ?: layout.type!!) {
                MediaItemLayout.Type.GRID -> MediaItemGrid(layout, playerProvider)
                MediaItemLayout.Type.LIST -> MediaItemList(layout, false, playerProvider)
                MediaItemLayout.Type.NUMBERED_LIST -> MediaItemList(layout, true, playerProvider)
            }
        }

        val bottom_padding = padding.calculateBottomPadding()
        if (bottom_padding != 0.dp) {
            item {
                Spacer(Modifier.height(bottom_padding))
            }
        }

        item {
            Crossfade(Pair(onContinuationRequested, loading_continuation)) { data ->
                if (data.second) {
                    CircularProgressIndicator(color = Theme.current.on_background)
                }
                else if (data.first != null) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = continuation_alignment) {
                        IconButton({ data.first!!.invoke() }) {
                            Icon(Icons.Filled.KeyboardDoubleArrowDown, null, tint = Theme.current.on_background)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MediaItemGrid(
    layout: MediaItemLayout,
    playerProvider: () -> PlayerViewContext,
    modifier: Modifier = Modifier
) {
    val row_count = if (layout.items.size <= 3) 1 else 2
    val item_width = 125.dp

    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            if (layout.title != null) {
                WidthShrinkText(layout.title, style = MaterialTheme.typography.headlineMedium.copy(color = Theme.current.on_background))
            }
            if (layout.subtitle != null) {
                WidthShrinkText(layout.subtitle, style = MaterialTheme.typography.headlineSmall.copy(color = Theme.current.on_background))
            }
        }

        LazyHorizontalGrid(
            rows = GridCells.Fixed(row_count),
            modifier = Modifier.requiredHeight(item_width * row_count * 1.1f)
        ) {
            items(layout.items.size, { layout.items[it].id }) {
                MediaItemLayout.ItemPreview(layout.items[it], item_width, null, playerProvider, Modifier)
            }
        }
    }
}

@Composable
fun MediaItemList(
    layout: MediaItemLayout,
    numbered: Boolean,
    playerProvider: () -> PlayerViewContext,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            if (layout.title != null) {
                WidthShrinkText(layout.title, style = MaterialTheme.typography.headlineMedium.copy(color = Theme.current.on_background))
            }
            if (layout.subtitle != null) {
                WidthShrinkText(layout.subtitle, style = MaterialTheme.typography.headlineSmall.copy(color = Theme.current.on_background))
            }
        }

        for (item in layout.items.withIndex()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (numbered) {
                    Text((item.index + 1).toString().padStart((layout.items.size + 1).toString().length, '0'), fontWeight = FontWeight.Light)
                }

                Column {
                    item.value.PreviewLong(Theme.current.on_background_provider, playerProvider, true, Modifier)
                }
            }
        }
    }
}