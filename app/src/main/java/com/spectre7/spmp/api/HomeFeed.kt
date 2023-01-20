package com.spectre7.spmp.api

import com.beust.klaxon.JsonObject
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.model.*
import okhttp3.Request

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

private class RawHomeFeedRow(
    val musicCarouselShelfRenderer: MusicCarouselShelfRenderer
) {
    class MusicCarouselShelfRenderer(val header: Header, val contents: List<ContentsItem>)

    class Header(val musicCarouselShelfBasicHeaderRenderer: MusicCarouselShelfBasicHeaderRenderer)
    class MusicCarouselShelfBasicHeaderRenderer(val title: Runs)
    class Runs(val runs: List<Title>)
    class Title(val text: String, val strapline: Runs? = null, val navigationEndpoint: NavigationEndpoint? = null)

    class ContentsItem(val musicTwoRowItemRenderer: MusicTwoRowItemRenderer? = null, val musicResponsiveListItemRenderer: MusicResponsiveListItemRenderer? = null)

    class MusicTwoRowItemRenderer(val navigationEndpoint: NavigationEndpoint)
    class NavigationEndpoint(val watchEndpoint: WatchEndpoint? = null, val browseEndpoint: BrowseEndpoint? = null)
    class WatchEndpoint(val videoId: String? = null, val playlistId: String? = null)
    class BrowseEndpoint(val browseId: String, val browseEndpointContextSupportedConfigs: BrowseEndpointContextSupportedConfigs? = null)
    class BrowseEndpointContextSupportedConfigs(val browseEndpointContextMusicConfig: BrowseEndpointContextMusicConfig)
    class BrowseEndpointContextMusicConfig(val pageType: String)

    class MusicResponsiveListItemRenderer(val playlistItemData: PlaylistItemData)
    class PlaylistItemData(val videoId: String)

    val title: Title get() = musicCarouselShelfRenderer.header.musicCarouselShelfBasicHeaderRenderer.title.runs[0]
    val items: List<ContentsItem> get() = musicCarouselShelfRenderer.contents
}

fun getHomeFeed(min_rows: Int = -1): Result<List<HomeFeedRow>> {

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

                var item_id = _item.navigationEndpoint.browseEndpoint.browseId
                if (item_id.startsWith("MPREb_")) {

                    val request = Request.Builder()
                        .url("https://music.youtube.com/browse/$item_id")
                        .header("Cookie", "CONSENT=YES+1")
                        .header("User-Agent", USER_AGENT)
                        .build()

                    val result = client.newCall(request).execute()
                    if (result.code != 200) {
                        throw RuntimeException("${result.message} | ${result.body?.string()}")
                    }

                    val text = result.body!!.string()

                    val target = "urlCanonical\\x22:\\x22https:\\/\\/music.youtube.com\\/playlist?list\\x3d"
                    val start = text.indexOf(target) + target.length
                    val end = text.indexOf("\\", start + 1)

                    item_id = text.substring(start, end)
                }

                return HomeFeedRow.Item(
                    item_type,
                    item_id,
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

    var data = postRequest(null) ?: return error!!
    val rows = processRows(klaxon.parseFromJsonArray(data.array<JsonObject>("contents")!!)!!).toMutableList()

    while (min_rows >= 1 && rows.size < min_rows) {
        val ctoken = data
            .array<JsonObject>("continuations")
            ?.get(0)
            ?.obj("nextContinuationData")
            ?.string("continuation")
            ?: break

        data = postRequest(ctoken) ?: return error!!
        rows.addAll(processRows(klaxon.parseFromJsonArray(data.array<JsonObject>("contents")!!)!!))
    }

    return Result.success(rows)
}