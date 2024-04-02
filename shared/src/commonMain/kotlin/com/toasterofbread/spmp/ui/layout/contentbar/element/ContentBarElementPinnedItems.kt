package com.toasterofbread.spmp.ui.layout.contentbar.element

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.padding
import com.toasterofbread.spmp.ui.component.PinnedItemsList
import kotlinx.serialization.Serializable

@Serializable
data class ContentBarElementPinnedItems(
    override val size_mode: ContentBarElement.SizeMode = DEFAULT_SIZE_MODE,
    override val size: Int = DEFAULT_SIZE,
): ContentBarElement() {
    override fun getType(): ContentBarElement.Type = ContentBarElement.Type.PINNED_ITEMS

    override fun copyWithSize(size_mode: ContentBarElement.SizeMode, size: Int): ContentBarElement =
        copy(size_mode = size_mode, size = size)

    override fun blocksIndicatorAnimation(): Boolean = true

    @Composable
    override fun ElementContent(vertical: Boolean, onPreviewClick: (() -> Unit)?, modifier: Modifier) {
        PinnedItemsList(
            vertical,
            modifier
                .run {
                    if (vertical) padding(vertical = 10.dp)
                    else padding(horizontal = 10.dp)
                },
            onClick = onPreviewClick,
            scrolling = false
        )
    }
}
