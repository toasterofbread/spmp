package com.toasterofbread.spmp.api

import SpMp
import com.toasterofbread.Database
import com.toasterofbread.spmp.api.Api.Companion.addYtHeaders
import com.toasterofbread.spmp.api.Api.Companion.getStream
import com.toasterofbread.spmp.api.Api.Companion.ytUrl
import com.toasterofbread.spmp.api.model.MusicDescriptionShelfRenderer
import com.toasterofbread.spmp.api.model.NavigationEndpoint
import com.toasterofbread.spmp.api.model.TextRuns
import com.toasterofbread.spmp.api.model.YoutubeiBrowseResponse
import com.toasterofbread.spmp.api.model.YoutubeiHeaderContainer
import com.toasterofbread.spmp.api.model.YoutubeiShelf
import com.toasterofbread.spmp.api.model.YoutubeiShelfContentsItem
import com.toasterofbread.spmp.api.radio.YoutubeiNextResponse
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.resources.uilocalisation.LocalisedYoutubeString
import com.toasterofbread.spmp.ui.component.mediaitemlayout.MediaItemLayout
import com.toasterofbread.spmp.model.FilterChip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import okio.use
import java.time.Duration

private val CACHE_LIFETIME = Duration.ofDays(1)

data class HomeFeedLoadResult(
    val layouts: List<MediaItemLayout>,
    val ctoken: String?,
    val filter_chips: List<FilterChip>?
)

suspend fun getHomeFeed(
    db: Database,
    min_rows: Int = -1,
    allow_cached: Boolean = true,
    params: String? = null,
    continuation: String? = null
): Result<HomeFeedLoadResult> = withContext(Dispatchers.IO) {
    val hl = SpMp.data_language
    val suffix = params ?: ""
    val rows_cache_key = "feed_rows$suffix"
    val ctoken_cache_key = "feed_ctoken$suffix"
    val chips_cache_key = "feed_chips$suffix"

//    if (allow_cached && continuation == null) {
//        Cache.get(rows_cache_key)?.use { cached_rows ->
//            val rows = Api.klaxon.parseArray<MediaItemLayout>(cached_rows)!!
//
//            val ctoken = Cache.get(ctoken_cache_key)?.use {
//                it.readText()
//            }
//
//            val chips: List<FilterChip>? = Cache.get(chips_cache_key)?.use {
//                Api.klaxon.parseArray(it)!!
//            }
//
//            return@withContext Result.success(HomeFeedLoadResult(rows, ctoken, chips))
//        }
//    }

    var last_request: Request? = null

    fun performRequest(ctoken: String?): Result<YoutubeiBrowseResponse> {
        last_request = null

        val endpoint = "/youtubei/v1/browse"
        val request = Request.Builder()
            .ytUrl(if (ctoken == null) endpoint else "$endpoint?ctoken=$ctoken&continuation=$ctoken&type=next")
            .addYtHeaders()
            .post(
                Api.getYoutubeiRequestBody(params?.let {
                    mapOf("params" to it)
                })
            )
            .build()

        val result = Api.request(request)
        val stream = result.getOrNull()?.getStream() ?: return result.cast()

        last_request = request

        try {
            return stream.use {
                Result.success(Api.klaxon.parse(it)!!)
            }
        }
        catch (e: Throwable) {
            return Result.failure(e)
        }
    }

    try {
        var data = performRequest(continuation).getOrThrow()

        val rows: MutableList<MediaItemLayout> = processRows(data.getShelves(continuation != null), hl, db).toMutableList()
        check(rows.isNotEmpty())

        val chips = data.getHeaderChips()

        var ctoken: String? = data.ctoken
        while (min_rows >= 1 && rows.size < min_rows) {
            if (ctoken == null) {
                break
            }

            data = performRequest(ctoken).getOrThrow()

            val shelves = data.getShelves(true)
            check(shelves.isNotEmpty())
            rows.addAll(processRows(shelves, hl, db))

            ctoken = data.ctoken
        }

        if (continuation == null) {
//            Cache.set(rows_cache_key, Api.klaxon.toJsonString(rows).reader(), CACHE_LIFETIME)
//            Cache.set(ctoken_cache_key, ctoken?.reader(), CACHE_LIFETIME)
//            Cache.set(chips_cache_key, Api.klaxon.toJsonString(chips).reader(), CACHE_LIFETIME)
        }

        return@withContext Result.success(HomeFeedLoadResult(rows, ctoken, chips))
    }
    catch (error: Throwable) {
        val request = last_request ?: return@withContext Result.failure(error)
        return@withContext Result.failure(
            DataParseException.ofYoutubeJsonRequest(
                request,
                cause = error,
                klaxon = Api.klaxon
            )
        )
    }
}

private fun processRows(rows: List<YoutubeiShelf>, hl: String, db: Database): List<MediaItemLayout> {
    val ret = mutableListOf<MediaItemLayout>()
    for (row in rows) {
        if (!row.implemented) {
            continue
        }

        when (val renderer = row.getRenderer()) {
            is MusicDescriptionShelfRenderer -> continue
            is YoutubeiHeaderContainer -> {
                val header = renderer.header?.header_renderer ?: continue

                fun add(
                    title: LocalisedYoutubeString,
                    subtitle: LocalisedYoutubeString? = null,
                    view_more: MediaItemLayout.ViewMore? = null,
                    type: MediaItemLayout.Type? = null
                ) {
                    val items = row.getMediaItems(hl).toMutableList()
                    db.transaction {
                        for (item in items) {
                            item.saveToDatabase(db)
                        }
                    }

                    ret.add(
                        MediaItemLayout(
                            items,
                            title, subtitle,
                            view_more = view_more,
                            type = type
                        )
                    )
                }

                val browse_endpoint = header.title?.runs?.first()?.navigationEndpoint?.browseEndpoint
                if (browse_endpoint == null) {
                    add(
                        LocalisedYoutubeString.Type.HOME_FEED.create(header.title!!.first_text),
                        header.subtitle?.first_text?.let { LocalisedYoutubeString.Type.HOME_FEED.create(it) }
                    )
                    continue
                }

                val view_more_page_title_key = when (browse_endpoint.browseId) {
                    "FEmusic_listen_again" -> if (Settings.KEY_FEED_SHOW_LISTEN_ROW.get()) "home_feed_listen_again" else null
                    "FEmusic_mixed_for_you" -> if (Settings.KEY_FEED_SHOW_MIX_ROW.get()) "home_feed_mixed_for_you" else null
                    "FEmusic_new_releases_albums" -> if (Settings.KEY_FEED_SHOW_NEW_ROW.get()) "home_feed_new_releases" else null
                    "FEmusic_moods_and_genres" -> if (Settings.KEY_FEED_SHOW_MOODS_ROW.get()) "home_feed_moods_and_genres" else null
                    "FEmusic_charts" -> if (Settings.KEY_FEED_SHOW_CHARTS_ROW.get()) "home_feed_charts" else null
                    else -> null
                }

                if (view_more_page_title_key != null) {
                    add(
                        LocalisedYoutubeString.Type.APP.create(view_more_page_title_key),
                        null,
                        view_more = MediaItemLayout.ListPageBrowseIdViewMore(browse_endpoint.browseId, browse_endpoint.params),
                        type = when(browse_endpoint.browseId) {
                            "FEmusic_listen_again" -> MediaItemLayout.Type.GRID_ALT
                            else -> null
                        }
                    )
                    continue
                }

                val page_type = browse_endpoint.browseEndpointContextSupportedConfigs?.browseEndpointContextMusicConfig?.pageType!!

                val media_item = MediaItemType.fromBrowseEndpointType(page_type)!!.referenceFromId(browse_endpoint.browseId).apply {
                    Title.set(header.title.runs?.getOrNull(0)?.text, db)
                }

                add(
                    LocalisedYoutubeString.Type.RAW.create(header.title.first_text),
                    header.subtitle?.first_text?.let { LocalisedYoutubeString.Type.HOME_FEED.create(it) },
                    view_more = MediaItemLayout.MediaItemViewMore(media_item)
                )
            }
            else -> throw NotImplementedError(row.getRenderer().toString())
        }
    }

    return ret
}

data class MusicShelfRenderer(
    val title: TextRuns? = null,
    val contents: List<YoutubeiShelfContentsItem>? = null,
    val continuations: List<YoutubeiNextResponse.Continuation>? = null,
    val bottomEndpoint: NavigationEndpoint? = null
)
