package com.toasterofbread.spmp.model.mediaitem

import LocalPlayerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.toasterofbread.spmp.model.mediaitem.db.isMediaItemHidden
import com.toasterofbread.spmp.model.mediaitem.db.rememberHiddenItems

interface MediaItemHolder {
    // If item is null, consider it deleted
    val item: MediaItem?
}

@Composable
fun List<MediaItemHolder>.rememberFilteredItems(apply_filter: Boolean): List<MediaItemHolder> {
    val player = LocalPlayerState.current
    val hidden_items: List<MediaItem> = rememberHiddenItems()
    return remember(this, apply_filter, hidden_items) {
        if (apply_filter) mapNotNull {
            val item = it.item
            if (item != null && isMediaItemHidden(item, player.database, hidden_items)) {
                return@mapNotNull null
            }
            return@mapNotNull it
        }
        else this
    }
}
