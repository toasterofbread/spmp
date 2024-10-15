package com.toasterofbread.spmp.model.mediaitem.db

import com.toasterofbread.spmp.db.Database
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.model.mediaitem.getType
import dev.toastbits.ytmkt.model.external.mediaitem.MediaItemLayout
import com.toasterofbread.spmp.model.mediaitem.layout.ContinuableMediaItemLayout
import com.toasterofbread.spmp.model.mediaitem.layout.YoutubePageType
import com.toasterofbread.spmp.model.mediaitem.layout.AppMediaItemLayout
import com.toasterofbread.spmp.model.mediaitem.MediaItemData
import com.toasterofbread.spmp.model.deserialise
import com.toasterofbread.spmp.model.serialise
import dev.toastbits.ytmkt.endpoint.SongFeedFilterChip
import dev.toastbits.ytmkt.model.external.ItemLayoutType
import dev.toastbits.ytmkt.uistrings.UiString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.datetime.Clock
import kotlin.time.Duration
import PlatformIO

data class SongFeedData(
    val layouts: List<ContinuableMediaItemLayout>,
    val filter_chips: List<SongFeedFilterChip>,
    val continuation_token: String?
)

object SongFeedCache {
    private val CACHE_LIFETIME: Duration = with (Duration) { 3.hours }

    suspend fun saveFeedLayouts(
        layouts: List<AppMediaItemLayout>,
        filter_chips: List<SongFeedFilterChip>?,
        continuation_token: String?,
        database: Database,
    ) = withContext(Dispatchers.PlatformIO) {
        database.transaction {
            database.songFeedRowQueries.clearAllFeedData()
            val now = Clock.System.now().toEpochMilliseconds()

            for (row in layouts.withIndex()) {
                val title: UiString? = row.value.title
                val view_more = row.value.view_more?.let {
                    YoutubePageType.fromPage(it)
                }

                database.songFeedRowQueries.insert(
                    row.index.toLong(),
                    now,
                    if (row.index + 1 == layouts.size) continuation_token else null,
                    row.value.type?.ordinal?.toLong(),
                    title?.serialise(),
                    view_more?.first,
                    view_more?.second
                )

                for (item in row.value.items.withIndex()) {
                    database.songFeedRowItemQueries.insert(
                        row.index.toLong(),
                        item.index.toLong(),
                        item.value.id,
                        item.value.getType().ordinal.toLong()
                    )
                }
            }

            if (!filter_chips.isNullOrEmpty()) {
                for (filter in filter_chips.withIndex()) {
                    val text = filter.value.text

                    database.songFeedFilterQueries.insert(
                        filter.index.toLong(),
                        filter.value.params,
                        text.serialise()
                    )
                }
            }
        }
    }

    suspend fun loadFeedLayouts(database: Database): SongFeedData? = withContext(Dispatchers.PlatformIO) {
        return@withContext database.transactionWithResult {
            val oldest_usable_time: Instant = Clock.System.now() - CACHE_LIFETIME
            var continuation_token: String? = null

            val layouts = database.songFeedRowQueries
                .getAll()
                .executeAsList()
                .map { row ->
                    val creation_time: Instant = Instant.fromEpochMilliseconds(row.creation_time)
                    if (creation_time < oldest_usable_time) {
                        return@transactionWithResult null
                    }

                    if (row.continuation_token != null) {
                        continuation_token = row.continuation_token
                    }

                    val items: List<MediaItemData> = database.songFeedRowItemQueries
                        .byRowIndex(row.row_index)
                        .executeAsList()
                        .map { item ->
                            MediaItemType.entries[item.item_type.toInt()].dataFromId(item.item_id)
                        }

                    ContinuableMediaItemLayout(
                        AppMediaItemLayout(
                            items,
                            row.title_data?.let {
                                UiString.deserialise(it)
                            },
                            null,
                            row.layout_type?.let { ItemLayoutType.entries[it.toInt()] },
                            view_more = row.view_more_type?.let { type ->
                                row.view_more_data?.let { data ->
                                    YoutubePageType.entries[type.toInt()].getPage(data)
                                }
                            }
                        )
                    )
                }

            val filter_chips = database.songFeedFilterQueries
                .getAll()
                .executeAsList()
                .map { filter ->
                    SongFeedFilterChip(
                        UiString.deserialise(filter.text_data),
                        filter.params
                    )
                }

            return@transactionWithResult SongFeedData(layouts, filter_chips, continuation_token)
        }
    }
}
