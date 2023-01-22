package com.spectre7.spmp.api

import com.beust.klaxon.JsonObject
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.model.*
import okhttp3.Request
import java.time.Duration

private val CACHE_LIFETIME = Duration.ofDays(1)

data class HomeFeedRow(val title: String, val subtitle: String?, val browse_id: String?, val items: List<Item>) {
    data class Item(val type: String, val id: String, val playlist_id: String? = null) {
        fun getPreviewable(): MediaItem {
            when (type) {
                "song" -> return Song.fromId(id)
                "artist" -> return Artist.fromId(id)
                "playlist" -> return Playlist.fromId(id)
            }
            throw RuntimeException(type)
        }
    }
}

class WatchEndpoint(val videoId: String? = null, val playlistId: String? = null)
class BrowseEndpointContextMusicConfig(val pageType: String)
class BrowseEndpointContextSupportedConfigs(val browseEndpointContextMusicConfig: BrowseEndpointContextMusicConfig)
class BrowseEndpoint(val browseId: String, val browseEndpointContextSupportedConfigs: BrowseEndpointContextSupportedConfigs? = null)
class NavigationEndpoint(val watchEndpoint: WatchEndpoint? = null, val browseEndpoint: BrowseEndpoint? = null)

class RawHomeFeedRow(
    val musicCarouselShelfRenderer: MusicCarouselShelfRenderer
) {
    class MusicCarouselShelfRenderer(val header: Header, val contents: List<ContentsItem>)

    class Header(val musicCarouselShelfBasicHeaderRenderer: MusicCarouselShelfBasicHeaderRenderer)
    class MusicCarouselShelfBasicHeaderRenderer(val title: Runs)
    class Runs(val runs: List<Title>)
    class Title(val text: String, val strapline: Runs? = null, val navigationEndpoint: NavigationEndpoint? = null)

    class ContentsItem(val musicTwoRowItemRenderer: MusicTwoRowItemRenderer? = null, val musicResponsiveListItemRenderer: MusicResponsiveListItemRenderer? = null)

    class MusicTwoRowItemRenderer(val navigationEndpoint: NavigationEndpoint)

    class MusicResponsiveListItemRenderer(val playlistItemData: PlaylistItemData)
    class PlaylistItemData(val videoId: String)

    val title: Title get() = musicCarouselShelfRenderer.header.musicCarouselShelfBasicHeaderRenderer.title.runs[0]
    val items: List<ContentsItem> get() = musicCarouselShelfRenderer.contents
}

fun getHomeFeed(min_rows: Int = -1, allow_cached: Boolean = true): Result<List<HomeFeedRow>> {

    var error: Result<List<HomeFeedRow>>? = null
    fun postRequest(ctoken: String?): JsonObject? {
        val url = "https://music.youtube.com/youtubei/v1/browse"
        val request = Request.Builder()
            .url(if (ctoken == null) url else "$url?ctoken=$ctoken&continuation=$ctoken&type=next")
            .headers(getYTMHeaders())
            .post(getYoutubeiRequestBody())
            .build()

        val response = client.newCall(request).execute()
        if (response.code != 200) {
            error = Result.failure(response)
            return null
        }

        val parsed = klaxon.parseJsonObject(response.body!!.charStream())
        if (ctoken != null) {
            return parsed.obj("continuationContents")!!.obj("sectionListContinuation")!!
        }
        else {
            return parsed
                .obj("contents")!!
                .obj("singleColumnBrowseResultsRenderer")!!
                .array<JsonObject>("tabs")!![0]
                .obj("tabRenderer")!!
                .obj("content")!!
                .obj("sectionListRenderer")!!
        }
    }

    fun processRows(rows: List<RawHomeFeedRow>): List<HomeFeedRow> {

        fun getItem(item: RawHomeFeedRow.ContentsItem): HomeFeedRow.Item {
            if (item.musicTwoRowItemRenderer != null) {
                val _item = item.musicTwoRowItemRenderer

                // Video
                if (_item.navigationEndpoint.watchEndpoint?.videoId != null) {
                    return HomeFeedRow.Item(
                        "song",
                        _item.navigationEndpoint.watchEndpoint.videoId,
                        _item.navigationEndpoint.watchEndpoint.playlistId
                    )
                }

                // Playlist or artist
                val item_type = when (_item.navigationEndpoint.browseEndpoint!!.browseEndpointContextSupportedConfigs!!.browseEndpointContextMusicConfig.pageType) {
                    "MUSIC_PAGE_TYPE_ALBUM", "MUSIC_PAGE_TYPE_PLAYLIST" -> "playlist"
                    "MUSIC_PAGE_TYPE_ARTIST" -> "artist"
                    else -> throw NotImplementedError(_item.navigationEndpoint.browseEndpoint.browseEndpointContextSupportedConfigs!!.browseEndpointContextMusicConfig.pageType)
                }

                return HomeFeedRow.Item(
                    item_type,
                    convertBrowseId(_item.navigationEndpoint.browseEndpoint.browseId).getDataOrThrow(),
                    null
                )
            }
            else if (item.musicResponsiveListItemRenderer != null) {
                return HomeFeedRow.Item(
                    "song",
                    item.musicResponsiveListItemRenderer.playlistItemData.videoId,
                    null
                )
            }
            else {
                throw NotImplementedError()
            }
        }

        return List(rows.size) { i ->
            val row = rows[i]

            HomeFeedRow(
                row.title.text,
                row.title.strapline?.runs?.get(0)?.text,
                row.title.navigationEndpoint?.browseEndpoint?.browseId,
                List(row.items.size) { j -> getItem(row.items[j]) }
            )
        }
    }


    var rows: MutableList<HomeFeedRow>? = null
    var data: JsonObject? = null

    val cache_key = "feed"
    if (allow_cached) {
        val cached = Cache.get(cache_key)
        if (cached != null) {
            rows = klaxon.parseArray<HomeFeedRow>(cached)?.toMutableList()
        }
    }

    if (rows == null) {
        data = postRequest(null) ?: return error!!
        rows = processRows(klaxon.parseFromJsonArray(data.array<JsonObject>("contents")!!)!!).toMutableList()
    }

    while (min_rows >= 1 && rows.size < min_rows) {
        if (data == null) {
            data = postRequest(null) ?: return error!!
        }

        val ctoken = data
            .array<JsonObject>("continuations")
            ?.get(0)
            ?.obj("nextContinuationData")
            ?.string("continuation")
            ?: break

        data = postRequest(ctoken) ?: return error!!
        rows.addAll(processRows(klaxon.parseFromJsonArray(data.array<JsonObject>("contents")!!)!!))
    }

    Cache.set(cache_key, klaxon.toJsonString(rows), CACHE_LIFETIME)

    return Result.success(rows)
}