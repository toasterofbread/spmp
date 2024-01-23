package com.toasterofbread.spmp.model.mediaitem

import androidx.compose.runtime.Composable
import com.toasterofbread.composekit.utils.composable.LargeDropdownMenu
import com.toasterofbread.db.Database
import com.toasterofbread.spmp.model.mediaitem.db.getPlayCount
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.resources.getString

enum class MediaItemSortType {
     NATIVE, ALPHABET, DURATION, ARTIST, PLAY_COUNT;

    fun getReadable(native_string_key: String? = null): String =
        getString(when(this) {
            NATIVE ->     native_string_key!!
            ALPHABET ->   "sort_type_alphabet"
            DURATION ->   "sort_type_duration"
            ARTIST -> "sort_type_artist"
            PLAY_COUNT -> "sort_type_playcount"
        })

    fun <T: MediaItem> sortItems(items: List<T>, db: Database, reversed: Boolean = false): List<T> {
        return sortItems(items, db, reversed) { it }
    }

    fun <T: MediaItem, V> sortItems(items: List<V>, db: Database, reversed: Boolean = false, mapValue: (V) -> T): List<V> {
        var reverse: Boolean = reversed
        val selector: (V) -> Comparable<*> = when (this) {
            NATIVE ->
                return if (reversed) items.asReversed()
                else items

            ALPHABET -> {
                { mapValue(it).getActiveTitle(db) ?: "" }
            }

            DURATION -> {
                {
                    val value: T = mapValue(it)
                    if (value is Song) value.Duration.get(db) ?: 0 else 0
                }
            }

            ARTIST -> {
                { (mapValue(it) as? MediaItem.WithArtist)?.Artist?.get(db)?.getActiveTitle(db) ?: "" }
            }

            PLAY_COUNT -> {
                reverse = !reverse
                { mapValue(it).getPlayCount(db) }
            }
        }
        return items.sortedWith(if (reverse) compareByDescending(selector) else compareBy(selector))
    }

    fun <T: MediaItem> sortAndFilterItems(items: List<T>, filter: String?, db: Database, reversed: Boolean): List<T> {
        val sorted = sortItems(items, db, reversed)
        if (filter == null) {
            return sorted
        }

        return sorted.filter { it.getActiveTitle(db)?.contains(filter, true) == true }
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
                entries.size - index_offset,
                selected_option.ordinal - index_offset,
                { entries[it + index_offset].getReadable(native_string_key) }
            ) {
                onSelected(entries[it + index_offset])
                onDismissed()
            }
        }
    }
}
