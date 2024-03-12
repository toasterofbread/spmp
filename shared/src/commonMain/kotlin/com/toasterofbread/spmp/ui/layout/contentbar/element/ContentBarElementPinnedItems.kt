package com.toasterofbread.spmp.ui.layout.contentbar.element

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.padding
import com.toasterofbread.spmp.ui.component.PinnedItemsList

class ContentBarElementPinnedItems(data: ContentBarElementData): ContentBarElement(data) {
    override fun blocksIndicatorAnimation(): Boolean = true

    @Composable
    override fun ElementContent(vertical: Boolean, enable_interaction: Boolean, modifier: Modifier) {
        PinnedItemsList(
            vertical,
            modifier
                .run {
                    if (vertical) padding(vertical = 10.dp)
                    else padding(horizontal = 10.dp)
                },
            enable_interaction = enable_interaction,
            scrolling = size_mode != SizeMode.FILL
        )
    }
}
