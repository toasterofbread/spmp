package com.toasterofbread.utils.composable

import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
fun <T> EmptyListCrossfade(
    list: List<T>,
    modifier: Modifier = Modifier,
    content: @Composable (list: List<T>?) -> Unit,
) {
    var current_list: List<T> by remember { mutableStateOf(list) }
    LaunchedEffect(list) {
        if (list.isNotEmpty()) {
            current_list = list
        }
    }

    Crossfade(list.isEmpty(), modifier) { is_empty ->
        content(if (is_empty) null else current_list)
    }
}
