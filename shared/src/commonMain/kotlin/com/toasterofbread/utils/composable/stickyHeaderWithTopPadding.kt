package com.toasterofbread.utils.composable

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.compose.ui.zIndex
import com.toasterofbread.utils.modifier.background
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
fun LazyListScope.stickyHeaderWithTopPadding(
    list_state: LazyListState,
    top_padding: Dp,
    modifier: Modifier = Modifier,
    getBackgroundColour: (() -> Color)? = null,
    content: @Composable () -> Unit
) {
    stickyHeader {
        var content_height: Int by remember { mutableStateOf(0) }

        Box(modifier) {
            if (getBackgroundColour != null) {
                Box(Modifier.fillMaxWidth().height(top_padding).background(getBackgroundColour))
            }

            Box(
                Modifier
                    .onSizeChanged {
                        content_height = it.height
                    }
                    .offset {
                        val index = list_state.firstVisibleItemIndex
                        IntOffset(
                            0,
                            if (index == 2) (list_state.firstVisibleItemScrollOffset / content_height.toFloat() * top_padding.toPx()).roundToInt()
                            else if (index > 2) top_padding.toPx().roundToInt()
                            else 0
                        )
                    }
                    .run {
                        if (getBackgroundColour != null) background(getBackgroundColour)
                        else this@run
                    }
            ) {
                content()
            }
        }
    }
}
