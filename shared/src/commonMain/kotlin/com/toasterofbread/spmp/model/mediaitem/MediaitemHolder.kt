package com.toasterofbread.spmp.model.mediaitem

import LocalPlayerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.toasterofbread.spmp.db.Database
import com.toasterofbread.spmp.model.mediaitem.db.isMediaItemHidden
import com.toasterofbread.spmp.model.mediaitem.db.rememberHiddenItems
import com.toasterofbread.spmp.service.playercontroller.PlayerState

interface MediaItemHolder {
    // If item is null, consider it deleted
    val item: MediaItem?
}

private fun List<MediaItemHolder>.filterItems(apply_filter: Boolean, hidden_items: List<MediaItem>, database: Database): List<MediaItem> {
    return mapNotNull {
        val item = it.item
        if (item == null || (apply_filter && isMediaItemHidden(item, database, hidden_items))) {
            return@mapNotNull null
        }
        return@mapNotNull it.item
    }
}

@Composable
fun List<MediaItemHolder>.rememberFilteredItems(apply_filter: Boolean = true): State<List<MediaItem>> {
    val player: PlayerState = LocalPlayerState.current
    val hidden_items: List<MediaItem> = rememberHiddenItems()
    val items_state: MutableState<List<MediaItem>> = remember { mutableStateOf(filterItems(apply_filter, hidden_items, player.database)) }

    LaunchedEffect(this, apply_filter, hidden_items) {
        items_state.value = filterItems(apply_filter, hidden_items, player.database)
    }

    return items_state
}
