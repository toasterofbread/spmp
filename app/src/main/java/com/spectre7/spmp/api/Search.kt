package com.spectre7.spmp.api

import com.spectre7.spmp.R
import com.spectre7.spmp.model.MediaItem
import com.spectre7.spmp.ui.component.MediaItemLayout
import com.spectre7.utils.getString
import com.spectre7.utils.printJson
import okhttp3.Request

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

    for (category in tab.content.sectionListRenderer.contents!!.withIndex()) {

        val card = category.value.musicCardShelfRenderer
        if (card != null) {
            TODO()
            continue
        }

        val shelf = category.value.musicShelfRenderer ?: continue
        ret.add(Pair(
            MediaItemLayout(shelf.title!!.first_text, null, items = shelf.contents.mapNotNull { it.toMediaItem() }.toMutableList()),
            if (category.index == 0) null else chips[category.index - 1].chipCloudChipRenderer.navigationEndpoint.searchEndpoint!!.params
        ))
    }

    return Result.success(ret)
}