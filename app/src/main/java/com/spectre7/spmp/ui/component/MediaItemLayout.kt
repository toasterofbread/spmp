package com.spectre7.spmp.ui.component

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.model.MediaItem

@Composable
private fun ItemPreview(
    item: MediaItem,
    height: Dp,
    animate: MutableState<Boolean>?,
    modifier: Modifier = Modifier,
    onItemClick: ((item: MediaItem) -> Unit)? = null,
    onItemLongClick: ((item: MediaItem) -> Unit)? = null
) {
    val onClick = if (onItemClick != null) { { onItemClick.invoke(item) } } else null
    val onLongClick = if (onItemLongClick != null) { { onItemLongClick.invoke(item) } } else null

    Box(modifier.requiredHeight(height), contentAlignment = Alignment.Center) {
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
                item.PreviewSquare(MainActivity.theme.getOnBackground(false), onClick, onLongClick, Modifier)
            }
        }
        else {
            item.PreviewSquare(MainActivity.theme.getOnBackground(false), onClick, onLongClick, Modifier)
        }
    }
}

@Composable
fun MediaItemGrid(
    title: String,
    subtitle: String?,
    items: List<MediaItem>,
    modifier: Modifier = Modifier,
    onClick: ((item: MediaItem) -> Unit)? = null,
    onLongClick: ((item: MediaItem) -> Unit)? = null
) {
    val row_count = 2

    Column(modifier) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
//            if (subtitle != null) {
            Text(subtitle ?: "Subtitle", style = MaterialTheme.typography.headlineSmall)
//            }
        LazyHorizontalGrid(
            rows = GridCells.Fixed(row_count),
            modifier = Modifier.requiredHeight(140.dp * row_count)
        ) {
            items(items.size, { items[it].id }) {
                val item = items[it]
                ItemPreview(item, 130.dp, null, Modifier, onClick, onLongClick)
            }
        }
    }
}

//class YtItemRow(
//    val title: String,
//    val subtitle: String?,
//    val type: TYPE,
//    val onItemClicked: (item: MediaItem) -> Unit,
//    val items: MutableList<Pair<MediaItem, Long>> = mutableStateListOf()
//) {
//    enum class TYPE { SQUARE, LONG }
//
//    @Composable
//    private fun SquareList() {
//        val row_count = 2
//        LazyHorizontalGrid(
//            rows = GridCells.Fixed(row_count),
//            modifier = Modifier.requiredHeight(140.dp * row_count)
//        ) {
//            items(items.size, { items[it].first.id }) {
//                val item = items[it]
//                ItemPreview(item.first, 130.dp, remember { System.currentTimeMillis() - item.second < 250})
//            }
//        }
//    }
//
//    @OptIn(ExperimentalPagerApi::class)
//    @Composable
//    private fun LongList(rows: Int = 5, columns: Int = 1) {
//        HorizontalPager(ceil((items.size / rows.toFloat()) / columns.toFloat()).toInt()) { page ->
//            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
//                val start = page * rows * columns
//                for (column in 0 until columns) {
//                    Column(Modifier.weight(1f)) {
//                        for (i in start + rows * column until Integer.min(start + (rows * columns), start + (rows * (column + 1)))) {
//                            if (i < items.size) {
//                                val item = items[i]
//                                ItemPreview(item.first, 50.dp, remember { System.currentTimeMillis() - item.second < 250})
//                            }
//                            else {
//                                Spacer(Modifier.requiredHeight(50.dp))
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//    @OptIn(ExperimentalMaterial3Api::class)
//    @Composable
//    fun ItemRow() {
//        if (items.isEmpty()) {
//            return
//        }
//
//        Card(
//            colors = CardDefaults.cardColors(containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.onBackground),
//            modifier = Modifier.fillMaxWidth()
//        ) {
//            Column {
//                Column {
//                    AutoResizeText(
//                        text = title,
//                        maxLines = 1,
//                        fontSizeRange = FontSizeRange(
//                            20.sp, 30.sp
//                        ),
//                        fontWeight = FontWeight.Bold,
//                        modifier = Modifier.padding(10.dp)
//                    )
//
//                    if (subtitle != null) {
//                        Text(subtitle, fontSize = 15.sp, fontWeight = FontWeight.Light, modifier = Modifier.padding(10.dp), color = MaterialTheme.colorScheme.onBackground.setAlpha(0.5))
//                    }
//                }
//
//                when (type) {
//                    TYPE.SQUARE -> SquareList()
//                    TYPE.LONG -> LongList(3, 2)
//                }
//            }
//        }
//    }
//
//    fun add(item: MediaItem?) {
//        if (item != null && items.firstOrNull { it.first.id == item.id } == null) {
//            items.add(Pair(item, System.currentTimeMillis()))
//        }
//    }
//}
