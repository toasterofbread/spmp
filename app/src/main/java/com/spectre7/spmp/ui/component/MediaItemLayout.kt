package com.spectre7.spmp.ui.component

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.model.MediaItem
import com.spectre7.spmp.ui.layout.PlayerViewContext
import com.spectre7.utils.WidthShrinkText


data class MediaItemLayout(
    val title: String?,
    val subtitle: String?,
    val type: Type? = null,
    val items: MutableList<MediaItem> = mutableListOf()
) {
    enum class Type {
        GRID
    }

    fun addItem(item: MediaItem) {
        if (items.any { it.id == item.id }) {
            return
        }
        items.add(item)
        return
    }
}

@Composable
fun MediaItemLayoutColumn(
    layouts: List<MediaItemLayout>,
    player: PlayerViewContext,
    modifier: Modifier = Modifier,
    top_padding: Dp = 0.dp,
    scroll_state: LazyListState = rememberLazyListState()
) {
    for (layout in layouts) {
        assert(layout.type != null)
    }

    LazyColumn(modifier, state = scroll_state) {
        if (top_padding > 0.dp) {
            item {
                Spacer(Modifier.requiredHeight(top_padding))
            }
        }

        items(layouts) { layout ->
            when (layout.type!!) {
                MediaItemLayout.Type.GRID -> MediaItemGrid(layout, player)
            }
        }
    }
}

@Composable
fun MediaItemGrid(
    layout: MediaItemLayout,
    player: PlayerViewContext,
    modifier: Modifier = Modifier
) {
    val row_count = if (layout.items.size <= 3) 1 else 2
    val item_width = 125.dp

    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {

        Column(Modifier.padding(horizontal = 20.dp)) {
            if (layout.title != null) {
                WidthShrinkText(layout.title, style = MaterialTheme.typography.headlineMedium)
            }
            if (layout.subtitle != null) {
                WidthShrinkText(layout.subtitle, style = MaterialTheme.typography.headlineSmall)
            }
        }

        LazyHorizontalGrid(
            rows = GridCells.Fixed(row_count),
            modifier = Modifier.requiredHeight(item_width * row_count * 1.1f)
        ) {
            items(layout.items.size, { layout.items[it].id }) {
                ItemPreview(layout.items[it], item_width, null, player, Modifier)
            }
        }
    }
}

@Composable
private fun ItemPreview(
    item: MediaItem,
    width: Dp,
    animate: MutableState<Boolean>?,
    player: PlayerViewContext,
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
                item.PreviewSquare(MainActivity.theme.getOnBackground(false), player, true, Modifier)
            }
        }
        else {
            item.PreviewSquare(MainActivity.theme.getOnBackground(false), player, true, Modifier)
        }
    }
}