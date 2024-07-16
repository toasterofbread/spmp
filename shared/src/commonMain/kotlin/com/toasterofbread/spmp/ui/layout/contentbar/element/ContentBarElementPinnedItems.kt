package com.toasterofbread.spmp.ui.layout.contentbar.element

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.padding
import com.toasterofbread.spmp.ui.component.PinnedItemsList
import com.toasterofbread.spmp.ui.layout.contentbar.layoutslot.LayoutSlot
import kotlinx.serialization.Serializable

@Serializable
data class ContentBarElementPinnedItems(
    override val config: ContentBarElementConfig = ContentBarElementConfig()
): ContentBarElement() {
    override fun getType(): ContentBarElement.Type = ContentBarElement.Type.PINNED_ITEMS

    override fun copyWithConfig(config: ContentBarElementConfig): ContentBarElement =
        copy(config = config)

    override fun blocksIndicatorAnimation(): Boolean = true

    @Composable
    override fun ElementContent(vertical: Boolean, slot: LayoutSlot?, bar_size: DpSize, onPreviewClick: (() -> Unit)?, modifier: Modifier) {
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
