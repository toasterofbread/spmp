package com.spectre7.spmp.api

import com.spectre7.spmp.R
import com.spectre7.spmp.model.MediaItem
import com.spectre7.spmp.ui.component.MediaItemLayout
import com.spectre7.utils.getString
import okhttp3.Request
import java.net.URLEncoder

data class SearchResults(val items: List<Result>) {
    data class Result(val id: ResultId, val snippet: Snippet)
    data class ResultId(val kind: String, val videoId: String = "", val channelId: String = "", val playlistId: String = "")
    data class Snippet(val publishedAt: String, val channelId: String, val title: String, val description: String, val thumbnails: Map<String, MediaItem.ThumbnailProvider.Thumbnail>)
}

fun searchYoutube(query: String, type: MediaItem.Type, max_results: Int = 10, channel_id: String? = null): Result<List<SearchResults.Result>> {
    val type_name = when (type) {
        MediaItem.Type.SONG -> "video"
        MediaItem.Type.ARTIST -> "channel"
        MediaItem.Type.PLAYLIST -> "playlist"
    }

    var url = "https://www.googleapis.com/youtube/v3/search?key=${getString(R.string.yt_api_key)}&part=snippet&type=$type_name&q=${URLEncoder.encode(query, "UTF-8")}&maxResults=$max_results&safeSearch=none"
    if (channel_id != null) {
        url += "&channelId=$channel_id"
    }

    val result = DataApi.request(Request.Builder().url(url).build())
    if (result.isFailure) {
        return result.cast()
    }

    return Result.success(DataApi.klaxon.parse<SearchResults>(result.getOrThrowHere().body!!.charStream())!!.items)
}

private data class YoutubeiSearchResponse(
    val contents: Contents
) {
    data class Contents(val tabbedSearchResultsRenderer: TabbedSearchResultsRenderer)
    data class TabbedSearchResultsRenderer(val tabs: List<Tab>)
    data class Tab(val tabRenderer: TabRenderer)
    data class TabRenderer(val content: Content? = null)
    data class Content(val sectionListRenderer: SectionListRenderer)
    data class SectionListRenderer(
        val contents: List<YoutubeiShelf>? = null,
        val header: Header? = null
    )
    data class Header(val chipCloudRenderer: ChipCloudRenderer)
    data class ChipCloudRenderer(val chips: List<Chip>)
    data class Chip(val chipCloudChipRenderer: ChipCloudChipRenderer)
    data class ChipCloudChipRenderer(val navigationEndpoint: NavigationEndpoint)
}

fun searchYoutubeMusic(query: String, params: String?, limit: Int = 10): Result<List<Pair<MediaItemLayout, String?>>> {

    val params_str: String = if (params != null) "\"$params\"" else "null"
    val request = Request.Builder()
        .url("https://music.youtube.com/youtubei/v1/search?key=${getString(R.string.yt_i_api_key)}")
        .headers(DataApi.getYTMHeaders())
        .post(DataApi.getYoutubeiRequestBody("""{ "query": "$query", "params": $params_str }"""))
        .build()

    val result = DataApi.request(request)
    if (result.isFailure) {
        return result.cast()
    }

    val stream = result.getOrThrow().body!!.charStream()
    val parsed: YoutubeiSearchResponse = DataApi.klaxon.parse(stream)!!
    stream.close()

    val ret: MutableList<Pair<MediaItemLayout, String?>> = mutableListOf()

    val tab = parsed.contents.tabbedSearchResultsRenderer.tabs.first().tabRenderer
    val chips = tab.content!!.sectionListRenderer.header!!.chipCloudRenderer.chips

    println(chips)

    for (category in tab.content.sectionListRenderer.contents!!.withIndex()) {

        println(category.value.musicShelfRenderer?.title?.first_text)
        val shelf = category.value.musicShelfRenderer ?: continue

        ret.add(Pair(
            MediaItemLayout(shelf.title!!.first_text, null, items = shelf.contents.mapNotNull { it.toMediaItem() }.toMutableList()),
            if (category.index == 0) null else chips[category.index - 1].chipCloudChipRenderer.navigationEndpoint.searchEndpoint!!.params
        ))
    }

    println(ret)

    return Result.success(ret)
}