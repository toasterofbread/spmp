package com.spectre7.spmp.platform

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.spectre7.spmp.platform.composable.PlatformDialog
import com.spectre7.spmp.platform.composable.BackHandler

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
            onDismissRequest = onDismissRequest
        ) {
            Surface(
                modifier,
                shape = RoundedCornerShape(12.dp),
                color = container_colour
            ) {
                val list_state = rememberLazyListState()
                LaunchedEffect(Unit) {
                    list_state.scrollToItem(index = selected)
                }

                LazyColumn(modifier = Modifier.fillMaxWidth(), state = list_state) {
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
    val content_colour = when {
        selected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }

    CompositionLocalProvider(LocalContentColor provides content_colour) {
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

@Composable
fun composeScope(block: @Composable () -> Unit) {
    block()
}

@Composable
fun BackHandler(getEnabled: @Composable () -> Boolean, action: () -> Unit) {
    BackHandler(getEnabled(), action)
}
