package com.spectre7.spmp.api

import com.beust.klaxon.JsonObject
import com.spectre7.spmp.model.*
import okhttp3.Request
import java.time.Duration

private val CACHE_LIFETIME = Duration.ofDays(1)

data class HomeFeedRow(
    val title: String,
    val subtitle: String?,
    val browse_id: String?,
    val items: List<MediaItem.Serialisable>
)

data class WatchEndpoint(val videoId: String? = null, val playlistId: String? = null)
data class BrowseEndpointContextMusicConfig(val pageType: String)
data class BrowseEndpointContextSupportedConfigs(val browseEndpointContextMusicConfig: BrowseEndpointContextMusicConfig)
data class BrowseEndpoint(val browseId: String, val browseEndpointContextSupportedConfigs: BrowseEndpointContextSupportedConfigs? = null)
data class NavigationEndpoint(val watchEndpoint: WatchEndpoint? = null, val browseEndpoint: BrowseEndpoint? = null)
data class Header(val musicCarouselShelfBasicHeaderRenderer: MusicCarouselShelfBasicHeaderRenderer)
data class MusicCarouselShelfBasicHeaderRenderer(val title: TextRuns)
data class TextRuns(val runs: List<TextRun>? = null) {
    val first_text: String get() = runs!![0].text
}
data class TextRun(val text: String, val strapline: TextRuns? = null, val navigationEndpoint: NavigationEndpoint? = null)

data class MusicShelfRenderer(val title: TextRuns, val contents: List<ContentsItem>)
data class MusicCarouselShelfRenderer(val header: Header, val contents: List<ContentsItem>)
data class MusicDescriptionShelfRenderer(val header: TextRuns, val subheader: TextRuns, val description: TextRuns)

data class MusicTwoRowItemRenderer(val navigationEndpoint: NavigationEndpoint)
data class MusicResponsiveListItemRenderer(val playlistItemData: PlaylistItemData, val flexColumns: List<FlexColumn>? = null)
data class PlaylistItemData(val videoId: String)
data class FlexColumn(val musicResponsiveListItemFlexColumnRenderer: MusicResponsiveListItemFlexColumnRenderer)
data class MusicResponsiveListItemFlexColumnRenderer(val text: TextRuns)

data class YoutubeiShelf(
    val musicShelfRenderer: MusicShelfRenderer? = null,
    val musicCarouselShelfRenderer: MusicCarouselShelfRenderer? = null,
    val musicDescriptionShelfRenderer: MusicDescriptionShelfRenderer? = null
) {
    init {
        assert(musicShelfRenderer != null || musicCarouselShelfRenderer != null || musicDescriptionShelfRenderer != null)
    }

    val title: TextRun get() =
        if (musicShelfRenderer != null) musicShelfRenderer.title.runs!![0]
        else if (musicCarouselShelfRenderer != null) musicCarouselShelfRenderer.header.musicCarouselShelfBasicHeaderRenderer.title.runs!![0]
        else musicDescriptionShelfRenderer!!.header.runs!![0]

    val contents: List<ContentsItem> get() = musicShelfRenderer?.contents ?: musicCarouselShelfRenderer!!.contents

    fun getRenderer(): Any {
        return musicShelfRenderer ?: musicCarouselShelfRenderer ?: musicDescriptionShelfRenderer!!
    }
}

data class ContentsItem(val musicTwoRowItemRenderer: MusicTwoRowItemRenderer? = null, val musicResponsiveListItemRenderer: MusicResponsiveListItemRenderer? = null) {
    fun toMediaItem(): MediaItem {
        if (musicTwoRowItemRenderer != null) {
            val _item = musicTwoRowItemRenderer

            // Video
            if (_item.navigationEndpoint.watchEndpoint?.videoId != null) {
                return Song.fromId(_item.navigationEndpoint.watchEndpoint.videoId)
            }

            val id = convertBrowseId(_item.navigationEndpoint.browseEndpoint!!.browseId).getDataOrThrow()

            // Playlist or artist
            return when (_item.navigationEndpoint.browseEndpoint.browseEndpointContextSupportedConfigs!!.browseEndpointContextMusicConfig.pageType) {
                "MUSIC_PAGE_TYPE_ALBUM", "MUSIC_PAGE_TYPE_PLAYLIST" -> Playlist.fromId(id)
                "MUSIC_PAGE_TYPE_ARTIST" -> Artist.fromId(id)
                else -> throw NotImplementedError(_item.navigationEndpoint.browseEndpoint.browseEndpointContextSupportedConfigs!!.browseEndpointContextMusicConfig.pageType)
            }
        }
        else if (musicResponsiveListItemRenderer != null) {
            return Song.fromId(musicResponsiveListItemRenderer.playlistItemData.videoId)
        }
        else {
            throw NotImplementedError()
        }
    }
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

    fun processRows(rows: List<YoutubeiShelf>): List<HomeFeedRow> {
        val ret = mutableListOf<HomeFeedRow>()
        for (row in rows) {
            val items: List<MediaItem.Serialisable> = when (row.getRenderer()) {
                is MusicDescriptionShelfRenderer -> continue
                is MusicShelfRenderer, is MusicCarouselShelfRenderer ->
                    List(row.contents.size) { row.contents[it].toMediaItem().toSerialisable() }
                else -> throw NotImplementedError()
            }

            ret.add(
                HomeFeedRow(
                    row.title.text,
                    row.title.strapline?.runs?.get(0)?.text,
                    row.title.navigationEndpoint?.browseEndpoint?.browseId,
                    items
                )
            )
        }

        return ret
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