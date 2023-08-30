package com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.endpoint

import SpMp
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.resources.uilocalisation.LocalisedYoutubeString
import com.toasterofbread.spmp.ui.component.mediaitemlayout.MediaItemLayout
import com.toasterofbread.spmp.youtubeapi.endpoint.HomeFeedEndpoint
import com.toasterofbread.spmp.youtubeapi.endpoint.HomeFeedLoadResult
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.DataParseException
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.YoutubeMusicApi
import com.toasterofbread.spmp.youtubeapi.model.MusicDescriptionShelfRenderer
import com.toasterofbread.spmp.youtubeapi.model.NavigationEndpoint
import com.toasterofbread.spmp.youtubeapi.model.TextRuns
import com.toasterofbread.spmp.youtubeapi.model.YoutubeiBrowseResponse
import com.toasterofbread.spmp.youtubeapi.model.YoutubeiHeaderContainer
import com.toasterofbread.spmp.youtubeapi.model.YoutubeiShelf
import com.toasterofbread.spmp.youtubeapi.model.YoutubeiShelfContentsItem
import com.toasterofbread.spmp.youtubeapi.radio.YoutubeiNextResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

class YTMGetHomeFeedEndpoint(override val api: YoutubeMusicApi): HomeFeedEndpoint() {
    override suspend fun getHomeFeed(
        min_rows: Int,
        allow_cached: Boolean,
        params: String?,
        continuation: String?
    ): Result<HomeFeedLoadResult> = withContext(Dispatchers.IO) {
        val hl = SpMp.data_language
        val suffix = params ?: ""
//    val rows_cache_key = "feed_rows$suffix"
//    val ctoken_cache_key = "feed_ctoken$suffix"
//    val chips_cache_key = "feed_chips$suffix"
//
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

        suspend fun performRequest(ctoken: String?): Result<YoutubeiBrowseResponse> {
            last_request = null

            val endpoint = "/youtubei/v1/browse"
            val request = Request.Builder()
                .endpointUrl(if (ctoken == null) endpoint else "$endpoint?ctoken=$ctoken&continuation=$ctoken&type=next")
                .addAuthApiHeaders(listOf())
                .postWithBody(
                    params?.let {
                        mapOf("params" to it)
                    }
                )
                .build()

            last_request = request

            val result = api.performRequest(request)
            val parsed: YoutubeiBrowseResponse = result.parseJsonResponse {
                return Result.failure(it)
            }

            return Result.success(parsed)
        }

        try {
            var data = performRequest(continuation).getOrThrow()
            val header_chips = data.getHeaderChips()

            val rows: MutableList<MediaItemLayout> = processRows(data.getShelves(continuation != null), hl).toMutableList()

            var ctoken: String? = data.ctoken
            while (ctoken != null && min_rows >= 1 && rows.size < min_rows) {
                data = performRequest(ctoken).getOrThrow()
                ctoken = data.ctoken

                val shelves = data.getShelves(true)
                if (shelves.isEmpty()) {
                    break
                }

                rows.addAll(processRows(shelves, hl))
            }

//        if (continuation == null) {
//            Cache.set(rows_cache_key, Api.klaxon.toJsonString(rows).reader(), CACHE_LIFETIME)
//            Cache.set(ctoken_cache_key, ctoken?.reader(), CACHE_LIFETIME)
//            Cache.set(chips_cache_key, Api.klaxon.toJsonString(chips).reader(), CACHE_LIFETIME)
//        }

            return@withContext Result.success(HomeFeedLoadResult(rows, ctoken, header_chips))
        }
        catch (error: Throwable) {
            val request = last_request ?: return@withContext Result.failure(error)
            return@withContext Result.failure(
                DataParseException.ofYoutubeJsonRequest(
                    request,
                    api,
                    cause = error
                )
            )
        }
    }

    private fun processRows(rows: List<YoutubeiShelf>, hl: String): List<MediaItemLayout> {
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
                        api.db.transaction {
                            for (item in items) {
                                item.saveToDatabase(api.db)
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

                    val media_item = MediaItemType.fromBrowseEndpointType(page_type).referenceFromId(browse_endpoint.browseId).apply {
                        Title.set(header.title.runs?.getOrNull(0)?.text, api.db)
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
}
