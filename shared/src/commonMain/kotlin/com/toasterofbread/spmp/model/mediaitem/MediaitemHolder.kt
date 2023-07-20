package com.toasterofbread.spmp.model.mediaitem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.toasterofbread.spmp.model.Settings

interface MediaItemHolder {
    // If item is null, consider it deleted
    val item: MediaItem?
}

fun isMediaItemHidden(item: MediaItem): Boolean {
    if (item.hidden) {
        return true
    }

    if (!Settings.KEY_FILTER_ENABLE.get<Boolean>()) {
        return false
    }

    val title = item.title ?: return false

    if (item is Artist && !Settings.KEY_FILTER_APPLY_TO_ARTISTS.get<Boolean>()) {
        return false
    }

    val keywords: Set<String> = Settings.KEY_FILTER_TITLE_KEYWORDS.get()
    for (keyword in keywords) {
        if (title.contains(keyword)) {
            return true
        }
    }

    return false
}

@Composable
fun List<MediaItemHolder>.rememberFilteredItems(apply_filter: Boolean): List<MediaItemHolder> {
    val hidden_items: Set<String> by Settings.INTERNAL_HIDDEN_ITEMS.rememberMutableState()
    return remember(apply_filter, hidden_items) {
        if (apply_filter) mapNotNull {
            val item = it.item
            if (item != null && isMediaItemHidden(item)) {
                return@mapNotNull null
            }
            return@mapNotNull it
        }
        else this
    }
}
