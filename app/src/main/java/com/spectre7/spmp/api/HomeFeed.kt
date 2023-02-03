package com.spectre7.spmp.api

import com.beust.klaxon.JsonObject
import com.spectre7.spmp.model.*
import okhttp3.Request
import java.io.BufferedReader
import java.time.Duration

private val CACHE_LIFETIME = Duration.ofDays(1)

data class YoutubeiHomeBrowseResponse(
    val contents: Contents,
    val continuationContents: ContinuationContents? = null
) {
    fun getShelves(has_continuation: Boolean): List<YoutubeiShelf> {
        return if (has_continuation) continuationContents!!.sectionListContinuation.contents else contents.singleColumnBrowseResultsRenderer.tabs.first().tabRenderer.content.sectionListRenderer.contents
    }

    data class Contents(val singleColumnBrowseResultsRenderer: SingleColumnBrowseResultsRenderer)
    data class SingleColumnBrowseResultsRenderer(val tabs: List<Tab>)
    data class Tab(val tabRenderer: TabRenderer)
    data class TabRenderer(val content: Content)
    data class Content(val sectionListRenderer: SectionListRenderer)
    data class SectionListRenderer(val contents: List<YoutubeiShelf>)

    data class ContinuationContents(val sectionListContinuation: SectionListContinuation)
    data class SectionListContinuation(val contents: List<YoutubeiShelf>, val continuations: List<YoutubeiNextResponse.Continuation>)
}

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
        else if (musicCarouselShelfRenderer != null) musicCarouselShelfRenderer.header.getRenderer().title.runs!![0]
        else musicDescriptionShelfRenderer!!.header.runs!![0]

    val contents: List<ContentsItem> get() = musicShelfRenderer?.contents ?: musicCarouselShelfRenderer!!.contents

    fun getRenderer(): Any {
        return musicShelfRenderer ?: musicCarouselShelfRenderer ?: musicDescriptionShelfRenderer!!
    }
}

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
data class Header(
    val musicCarouselShelfBasicHeaderRenderer: HeaderRenderer? = null,
    val musicImmersiveHeaderRenderer: HeaderRenderer? = null,
    val musicVisualHeaderRenderer: HeaderRenderer? = null,
    val musicDetailHeaderRenderer: HeaderRenderer? = null
) {
    fun getRenderer(): HeaderRenderer {
        return musicCarouselShelfBasicHeaderRenderer ?: musicImmersiveHeaderRenderer ?: musicVisualHeaderRenderer ?: musicDetailHeaderRenderer!!
    }
}

//val thumbnails = (header.obj("thumbnail") ?: header.obj("foregroundThumbnail")!!)
//    .obj("musicThumbnailRenderer")!!
//    .obj("thumbnail")!!
//    .array<JsonObject>("thumbnails")!!

data class HeaderRenderer(val title: TextRuns, val description: TextRuns? = null, val thumbnail: Thumbnails? = null, val foregroundThumbnail: Thumbnails? = null) {
    fun getThumbnails(): List<MediaItem.ThumbnailProvider.Thumbnail> {
        return (thumbnail ?: foregroundThumbnail!!).musicThumbnailRenderer.thumbnail.thumbnails
    }
}
data class Thumbnails(val musicThumbnailRenderer: MusicThumbnailRenderer)
data class MusicThumbnailRenderer(val thumbnail: Thumbnail) {
    data class Thumbnail(val thumbnails: List<MediaItem.ThumbnailProvider.Thumbnail>)
}
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

data class ContentsItem(val musicTwoRowItemRenderer: MusicTwoRowItemRenderer? = null, val musicResponsiveListItemRenderer: MusicResponsiveListItemRenderer? = null) {
    fun toSerialisableMediaItem(): MediaItem.Serialisable {
        if (musicTwoRowItemRenderer != null) {
            val _item = musicTwoRowItemRenderer

            // Video
            if (_item.navigationEndpoint.watchEndpoint?.videoId != null) {
                return Song.serialisable(_item.navigationEndpoint.watchEndpoint.videoId)
            }

            val browse_id = _item.navigationEndpoint.browseEndpoint!!.browseId

            // Playlist or artist
            return when (_item.navigationEndpoint.browseEndpoint.browseEndpointContextSupportedConfigs!!.browseEndpointContextMusicConfig.pageType) {
                "MUSIC_PAGE_TYPE_ALBUM", "MUSIC_PAGE_TYPE_PLAYLIST", "MUSIC_PAGE_TYPE_AUDIOBOOK" -> Playlist.serialisable(browse_id)
                "MUSIC_PAGE_TYPE_ARTIST" -> Artist.serialisable(browse_id)
                else -> throw NotImplementedError("$browse_id: ${_item.navigationEndpoint.browseEndpoint.browseEndpointContextSupportedConfigs.browseEndpointContextMusicConfig.pageType}")
            }
        }
        else if (musicResponsiveListItemRenderer != null) {
            return Song.serialisable(musicResponsiveListItemRenderer.playlistItemData.videoId)
        }
        else {
            throw NotImplementedError()
        }
    }
}

fun getHomeFeed(min_rows: Int = -1, allow_cached: Boolean = true): DataApi.Result<List<HomeFeedRow>> {

    fun postRequest(ctoken: String?): DataApi.Result<BufferedReader> {
        val url = "https://music.youtube.com/youtubei/v1/browse"
        val request = Request.Builder()
            .url(if (ctoken == null) url else "$url?ctoken=$ctoken&continuation=$ctoken&type=next")
            .headers(DataApi.getYTMHeaders())
            .post(DataApi.getYoutubeiRequestBody())
            .build()

        val response = DataApi.client.newCall(request).execute()
        if (response.code != 200) {
            return DataApi.Result.failure(response)
        }

        return DataApi.Result.success(BufferedReader(response.body!!.charStream()))
    }

    fun processRows(rows: List<YoutubeiShelf>): List<HomeFeedRow> {
        val ret = mutableListOf<HomeFeedRow>()
        for (row in rows) {
            val items: List<MediaItem.Serialisable> = when (row.getRenderer()) {
                is MusicDescriptionShelfRenderer -> continue
                is MusicShelfRenderer, is MusicCarouselShelfRenderer ->
                    List(row.contents.size) { row.contents[it].toSerialisableMediaItem() }
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

    val rows: MutableList<HomeFeedRow>
    var response_reader: BufferedReader? = null

    val cache_key = "feed"
    if (allow_cached) {
        response_reader = Cache.get(cache_key)
    }

    if (response_reader == null) {
        val result = postRequest(null)
        if (!result.success) {
            return DataApi.Result.failure(result.exception)
        }

        response_reader = result.data
        Cache.set(cache_key, response_reader, CACHE_LIFETIME)
        response_reader.close()

        response_reader = Cache.get(cache_key)!!
    }

    var data: YoutubeiHomeBrowseResponse = DataApi.klaxon.parse(response_reader)!!
    rows = processRows(data.getShelves(false)).toMutableList()

    while (min_rows >= 1 && rows.size < min_rows) {
        val ctoken = data.continuationContents?.sectionListContinuation?.continuations?.firstOrNull()?.nextContinuationData?.continuation ?: break

        val result = postRequest(ctoken)
        if (!result.success) {
            return DataApi.Result.failure(result.exception)
        }
        data = DataApi.klaxon.parse(result.data)!!
        rows.addAll(processRows(data.getShelves(true)))
    }

    return DataApi.Result.success(rows)
}