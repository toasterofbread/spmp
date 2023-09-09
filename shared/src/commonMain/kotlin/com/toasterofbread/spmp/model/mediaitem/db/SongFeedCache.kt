package com.toasterofbread.spmp.model.mediaitem.db

import com.toasterofbread.Database
import com.toasterofbread.spmp.model.FilterChip
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.model.mediaitem.layout.MediaItemLayout
import com.toasterofbread.spmp.model.mediaitem.layout.ViewMoreType
import com.toasterofbread.spmp.resources.uilocalisation.LocalisedYoutubeString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

data class SongFeedData(
    val layouts: List<MediaItemLayout>,
    val filter_chips: List<FilterChip>,
    val continuation_token: String?
)

object SongFeedCache {
    private val CACHE_LIFETIME = Duration.ofHours(3)

    suspend fun saveFeedLayouts(
        layouts: List<MediaItemLayout>,
        filter_chips: List<FilterChip>?,
        continuation_token: String?,
        database: Database,
    ) = withContext(Dispatchers.IO) {
        database.transaction {
            database.songFeedRowQueries.clearAllFeedData()
            val now = Instant.now().toEpochMilli()

            for (row in layouts.withIndex()) {
                val title = row.value.title
                val view_more = row.value.view_more?.let {
                    ViewMoreType.fromViewMore(it)
                }

                database.songFeedRowQueries.insert(
                    row.index.toLong(),
                    now,
                    if (row.index + 1 == layouts.size) continuation_token else null,
                    title?.type?.ordinal?.toLong(),
                    title?.key,
                    title?.source_language,
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
                        text.type.ordinal.toLong(),
                        text.key,
                        text.source_language
                    )
                }
            }
        }
    }

    suspend fun loadFeedLayouts(database: Database): SongFeedData? = withContext(Dispatchers.IO) {
        return@withContext database.transactionWithResult {
            val oldest_usable_time = Instant.now().minusMillis(CACHE_LIFETIME.toMillis())
            var continuation_token: String? = null

            val layouts = database.songFeedRowQueries
                .getAll()
                .executeAsList()
                .map { row ->
                    val creation_time = Instant.ofEpochMilli(row.creation_time)
                    if (creation_time.isBefore(oldest_usable_time)) {
                        return@transactionWithResult null
                    }

                    if (row.continuation_token != null) {
                        continuation_token = row.continuation_token
                    }

                    val items = database.songFeedRowItemQueries
                        .byRowIndex(row.row_index)
                        .executeAsList()
                        .map { item ->
                            MediaItemType.values()[item.item_type.toInt()].referenceFromId(item.item_id)
                        }

                    MediaItemLayout(
                        items,
                        row.title_key?.let { title_key ->
                            LocalisedYoutubeString(
                                title_key,
                                LocalisedYoutubeString.Type.values()[row.title_type?.toInt() ?: 0],
                                row.title_lang!!
                            )
                        },
                        null,
                        view_more = row.view_more_type?.let { view_more_type ->
                            ViewMoreType.values()[view_more_type.toInt()].getViewMore(row.view_more_data!!)
                        }
                    )
                }

            val filter_chips = database.songFeedFilterQueries
                .getAll()
                .executeAsList()
                .map { filter ->
                    FilterChip(
                        LocalisedYoutubeString(filter.text_key, LocalisedYoutubeString.Type.values()[filter.text_type.toInt()], filter.text_lang),
                        filter.params
                    )
                }

            return@transactionWithResult SongFeedData(layouts, filter_chips, continuation_token)
        }
    }
}
