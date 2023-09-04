package com.toasterofbread.utils.composable

import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.runtime.Composable

fun LazyGridScope.spanItem(key: Any? = null, contentType: Any? = null, content: @Composable LazyGridItemScope.() -> Unit) {
    item(
        key,
        { GridItemSpan(maxLineSpan) },
        contentType,
        content
    )
}
