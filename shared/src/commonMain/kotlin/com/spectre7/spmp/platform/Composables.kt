package com.spectre7.spmp.platform

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.dp

@Composable
expect fun BackHandler(enabled: Boolean = true, action: () -> Unit)

@Composable
expect fun PlatformDialog(
    onDismissRequest: () -> Unit,
    use_platform_default_width: Boolean = true,
    dim_behind: Boolean = true,
    content: @Composable () -> Unit
)

@Composable
expect fun PlatformAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    shape: Shape,
    containerColor: Color,
    iconContentColor: Color,
    titleContentColor: Color,
    textContentColor: Color
)

@Composable
expect fun PlatformAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable() (() -> Unit)? = null,
    icon: @Composable() (() -> Unit)? = null,
    title: @Composable() (() -> Unit)? = null,
    text: @Composable() (() -> Unit)? = null
)

@Composable
expect fun SwipeRefresh(
    state: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    swipe_enabled: Boolean = true,
    content: @Composable () -> Unit
)

@Composable
fun LargeDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    item_count: Int,
    selected: Int,
    getItem: (Int) -> String,
    modifier: Modifier = Modifier,
    item_colour: Color = MaterialTheme.colorScheme.primary,
    selected_item_colour: Color = MaterialTheme.colorScheme.onSurface,
    container_colour: Color = MaterialTheme.colorScheme.surface,
    onSelected: (index: Int) -> Unit
) {
    require(selected in 0 until item_count)

    if (expanded) {
        PlatformDialog(
            onDismissRequest = onDismissRequest,
        ) {
            Surface(
                modifier,
                shape = RoundedCornerShape(12.dp),
                color = container_colour
            ) {
                val listState = rememberLazyListState()
                LaunchedEffect("ScrollToSelected") {
                    listState.scrollToItem(index = selected)
                }

                LazyColumn(modifier = Modifier.fillMaxWidth(), state = listState) {
                    items(item_count) { index ->
                        Box(
                            Modifier
                                .clickable { onSelected(index) }
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = getItem(index),
                                style = MaterialTheme.typography.titleSmall,
                                color = if (index == selected) selected_item_colour else item_colour
                            )
                        }

                        if (index + 1 < item_count) {
                            Divider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LargeDropdownMenuItem(
    text: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val contentColor = when {
        selected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }

    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Box(modifier = Modifier
            .clickable(enabled) { onClick() }
            .fillMaxWidth()
            .padding(16.dp)) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall,
            )
        }
    }
}
