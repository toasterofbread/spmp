package com.toasterofbread.spmp.model.mediaitem

import androidx.compose.runtime.Composable
import androidx.compose.material3.Text
import dev.toastbits.composekit.components.utils.composable.LargeDropdownMenu
import com.toasterofbread.spmp.db.Database
import com.toasterofbread.spmp.model.mediaitem.db.getPlayCount
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.resources.rememberStringResourceByKey
import dev.toastbits.ytmkt.model.external.mediaitem.YtmMediaItem
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.sort_type
import spmp.shared.generated.resources.sort_type_alphabet
import spmp.shared.generated.resources.sort_type_artist
import spmp.shared.generated.resources.sort_type_duration
import spmp.shared.generated.resources.sort_type_playcount

enum class MediaItemSortType {
    NATIVE, ALPHABET, DURATION, ARTIST, PLAY_COUNT;

    @Composable
    fun getReadable(native_string_key: String? = null): String =
        when(this) {
            NATIVE ->     stringResource(rememberStringResourceByKey(native_string_key!!))
            ALPHABET ->   stringResource(Res.string.sort_type_alphabet)
            DURATION ->   stringResource(Res.string.sort_type_duration)
            ARTIST -> stringResource(Res.string.sort_type_artist)
            PLAY_COUNT -> stringResource(Res.string.sort_type_playcount)
        }

    fun <T: YtmMediaItem> sortItems(items: List<T>, db: Database, reversed: Boolean = false): List<T> {
        return sortItems(items, db, reversed) { it }
    }

    fun <T: YtmMediaItem, V> sortItems(items: List<V>, db: Database, reversed: Boolean = false, mapValue: (V) -> T): List<V> {
        var reverse: Boolean = reversed
        val selector: (V) -> Comparable<*> = when (this) {
            NATIVE ->
                return if (reversed) items.asReversed()
                else items

            ALPHABET -> {
                { mapValue(it).getItemActiveTitle(db) ?: "" }
            }

            DURATION -> {
                {
                    val value: T = mapValue(it)
                    if (value is Song) value.Duration.get(db) ?: 0 else 0
                }
            }

            ARTIST -> {
                { (mapValue(it) as? MediaItem.WithArtists)?.Artists?.get(db)?.firstOrNull()?.getActiveTitle(db) ?: "" }
            }

            PLAY_COUNT -> {
                reverse = !reverse
                { mapValue(it).getPlayCount(db) }
            }
        }
        return items.sortedWith(if (reverse) compareByDescending(selector) else compareBy(selector))
    }

    fun <T: YtmMediaItem> sortAndFilterItems(items: List<T>, filter: String?, db: Database, reversed: Boolean): List<T> {
        val sorted = sortItems(items, db, reversed)
        if (filter == null) {
            return sorted
        }

        return sorted.filter { it.getItemActiveTitle(db)?.contains(filter, true) == true }
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
                title = stringResource(Res.string.sort_type),
                isOpen = expanded,
                onDismissRequest = onDismissed,
                items = (0 until entries.size - index_offset).toList(),
                selectedItem = selected_option.ordinal - index_offset,
                onSelected = { _, index ->
                    onSelected(entries[index + index_offset])
                    onDismissed()
                }
            ) { index ->
                Text(entries[index + index_offset].getReadable(native_string_key))
            }
        }
    }
}
