package com.toasterofbread.spmp.model.mediaitem

import androidx.compose.runtime.Composable
import com.toasterofbread.Database
import com.toasterofbread.spmp.model.mediaitem.db.getPlayCount
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.platform.LargeDropdownMenu
import com.toasterofbread.spmp.resources.getString

enum class MediaItemSortType {
    NATIVE, ALPHABET, DURATION, PLAY_COUNT;

    fun getReadable(native_string_key: String? = null): String =
        getString(when(this) {
            NATIVE ->     native_string_key!!
            ALPHABET ->   "sort_option_alphabet"
            DURATION ->   "sort_option_duration"
            PLAY_COUNT -> "sort_option_playcount"
        })

    fun <T: MediaItem> sortItems(items: List<T>, db: Database, reversed: Boolean = false): List<T> {
        var reverse: Boolean = reversed
        val selector: (T) -> Comparable<*> = when (this) {
            NATIVE ->
                return if (reversed) items.asReversed()
                else items
            ALPHABET -> {
                { it.getActiveTitle(db) ?: "" }
            }
            DURATION -> {
                { if (it is Song) it.Duration.get(db) ?: 0 else 0 }
            }
            PLAY_COUNT -> {
                reverse = !reverse
                { it.getPlayCount(db) }
            }
        }
        return items.sortedWith(if (reverse) compareByDescending(selector) else compareBy(selector))
    }

    companion object {
        @Composable
        fun SelectionMenu(
            expanded: Boolean,
            selected_option: MediaItemSortType,
            onDismissed: () -> Unit,
            onSelected: (MediaItemSortType) -> Unit,
            native_string_key: String? = null
        ) {
            val index_offset = if (native_string_key == null) 1 else 0

            LargeDropdownMenu(
                expanded,
                onDismissed,
                values().size - index_offset,
                selected_option.ordinal - index_offset,
                { values()[it + index_offset].getReadable(native_string_key) }
            ) {
                onSelected(values()[it + index_offset])
                onDismissed()
            }
        }
    }
}
