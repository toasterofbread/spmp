package com.toasterofbread.spmp.model.mediaitem

import com.toasterofbread.Database
import com.toasterofbread.spmp.model.mediaitem.loader.MediaItemLoader
import com.toasterofbread.spmp.resources.uilocalisation.LocalisedYoutubeString
import mediaitem.ByItemId
import mediaitem.PlaylistItemQueries
import java.time.LocalDate
import java.time.temporal.ChronoUnit

fun Boolean.toSQLBoolean(): Long? = if (this) 0L else null
fun Long?.fromSQLBoolean(): Boolean = this != null

fun Boolean?.toNullableSQLBoolean(): Long? =
    when (this) {
        false -> 0L
        true -> 1L
        null -> null
    }
fun Long?.fromNullableSQLBoolean(): Boolean? =
    when (this) {
        0L -> false
        1L -> true
        else -> null
    }

fun Long?.toLocalisedYoutubeString(key: String?): LocalisedYoutubeString? =
    if (this != null) LocalisedYoutubeString(key!!, LocalisedYoutubeString.Type.values()[this.toInt()])
    else null

suspend fun <T, ItemType: MediaItemData> Database.loadMediaItemValue(item: ItemType, getValue: ItemType.() -> T?): Result<T>? {
    // If the item is marked as already loaded, give up
    val loaded = mediaItemQueries.loadedById(item.id).executeAsOneOrNull()?.loaded.fromSQLBoolean()
    if (loaded) {
        return null
    }

    // Load item data
    val load_result = MediaItemLoader.loadUnknown(item, this)
    val loaded_item = load_result.fold(
        { it },
        { return Result.failure(it) }
    )

    val value = getValue(loaded_item)
    return value?.let { Result.success(it) }
}

fun Database.incrementMediaItemPlayCount(item_id: String, by: Int = 1) {
    require(by >= 1)
    mediaItemPlayCountQueries.transaction {
        val day = LocalDate.now().toEpochDay()
        mediaItemPlayCountQueries.insertOrIgnore(day, item_id)
        mediaItemPlayCountQueries.increment(by.toLong(), item_id, day)
    }
}

fun Database.getMediaItemPlayCount(item_id: String, range: ChronoUnit? = null): Int {
    val entries = if (range != null) {
        val since_day = LocalDate.now().minus(range.duration).toEpochDay()
        mediaItemPlayCountQueries.byItemIdSince(
            item_id, since_day,
            { day, play_count ->
                ByItemId(day, play_count)
            }
        ).executeAsList()
    }
    else {
        mediaItemPlayCountQueries.byItemId(item_id).executeAsList()
    }

    return entries.sumOf { it.play_count }.toInt()
}

fun PlaylistItemQueries.movePlaylistItem(playlist_id: String, from: Int, to: Int) = transaction {
    if (from == to) {
        return@transaction
    }

    fun move(from: Int, to: Int) =
        updateItemIndex(from = from.toLong(), to = to.toLong(), playlist_id = playlist_id)

    move(from, to)
    if (to > from) {
        for (i in from until to) {
            move(i, i - 1)
        }
    }
    else {
        for (i in to + 1 .. from) {
            move(i, i + 1)
        }
    }
}
