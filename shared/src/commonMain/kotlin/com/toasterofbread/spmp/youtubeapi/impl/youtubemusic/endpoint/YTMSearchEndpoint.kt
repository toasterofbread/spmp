package com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.endpoint

import com.toasterofbread.spmp.model.mediaitem.MediaItemData
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistData
import com.toasterofbread.spmp.model.mediaitem.enums.PlaylistType
import com.toasterofbread.spmp.model.mediaitem.enums.SongType
import com.toasterofbread.spmp.model.mediaitem.layout.MediaItemLayout
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylistData
import com.toasterofbread.spmp.model.mediaitem.song.SongData
import com.toasterofbread.spmp.platform.getDataLanguage
import com.toasterofbread.spmp.resources.uilocalisation.YoutubeLocalisedString
import com.toasterofbread.spmp.youtubeapi.endpoint.SearchEndpoint
import com.toasterofbread.spmp.youtubeapi.endpoint.SearchFilter
import com.toasterofbread.spmp.youtubeapi.endpoint.SearchResults
import com.toasterofbread.spmp.youtubeapi.endpoint.SearchType
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.YoutubeMusicApi
import com.toasterofbread.spmp.youtubeapi.model.MusicCardShelfRenderer
import com.toasterofbread.spmp.youtubeapi.model.NavigationEndpoint
import com.toasterofbread.spmp.youtubeapi.model.TextRuns
import com.toasterofbread.spmp.youtubeapi.model.YoutubeiShelf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

class YTMSearchEndpoint(override val api: YoutubeMusicApi): SearchEndpoint() {
    override suspend fun searchMusic(query: String, params: String?): Result<SearchResults> = withContext(Dispatchers.IO) {
        val hl = api.context.getDataLanguage()
        val request = Request.Builder()
            .endpointUrl("/youtubei/v1/search")
            .addAuthApiHeaders()
            .postWithBody(mapOf("query" to query, "params" to params))
            .build()

        val result = api.performRequest(request)
        val parsed: YoutubeiSearchResponse = result.parseJsonResponse {
            return@withContext Result.failure(it)
        }

        val tab = parsed.contents.tabbedSearchResultsRenderer.tabs.first().tabRenderer

        var correction_suggestion: String? = null
        val categories: List<YoutubeiShelf> = tab.content?.sectionListRenderer?.contents?.filter { shelf ->
            if (shelf.itemSectionRenderer != null) {
                shelf.itemSectionRenderer.contents.firstOrNull()?.didYouMeanRenderer?.correctedQuery?.first_text?.also {
                    correction_suggestion = it
                }
                false
            }
            else {
                true
            }
        } ?: emptyList()

        val category_layouts: MutableList<Pair<MediaItemLayout, SearchFilter?>> = mutableListOf()
        val chips = tab.content?.sectionListRenderer?.header?.chipCloudRenderer?.chips

        for (category in categories.withIndex()) {
            val card: MusicCardShelfRenderer? = category.value.musicCardShelfRenderer
            val key: String? = card?.header?.musicCardShelfHeaderBasicRenderer?.title?.firstTextOrNull()
            if (key != null) {
                category_layouts.add(Pair(
                    MediaItemLayout(
                        mutableListOf(card.getMediaItem()),
                        YoutubeLocalisedString.Type.SEARCH_PAGE.createFromKey(key, api.context),
                        null,
                        type = MediaItemLayout.Type.CARD
                    ),
                    null
                ))
                continue
            }

            val shelf: YTMGetHomeFeedEndpoint.MusicShelfRenderer = category.value.musicShelfRenderer ?: continue
            val items = shelf.contents?.mapNotNull { it.toMediaItemData(hl)?.first }?.toMutableList() ?: continue
            val search_params = if (category.index == 0) null else chips?.get(category.index - 1)?.chipCloudChipRenderer?.navigationEndpoint?.searchEndpoint?.params

            val title: String? = shelf.title?.firstTextOrNull()
            if (title != null) {
                category_layouts.add(Pair(
                    MediaItemLayout(items, YoutubeLocalisedString.Type.SEARCH_PAGE.createFromKey(title, api.context), null),
                    search_params?.let {
                        val item = items.firstOrNull() ?: return@let null
                        SearchFilter(when (item) {
                            is SongData -> if (item.song_type == SongType.VIDEO) SearchType.VIDEO else SearchType.SONG
                            is ArtistData -> SearchType.ARTIST
                            is RemotePlaylistData -> when (item.playlist_type) {
                                PlaylistType.ALBUM -> SearchType.ALBUM
                                else -> SearchType.PLAYLIST
                            }
                            else -> throw NotImplementedError(item.getType().toString())
                        }, it)
                    }
                ))
            }
        }

        api.database.transaction {
            for (category in category_layouts) {
                for (item in category.first.items) {
                    (item as MediaItemData).saveToDatabase(api.database)
                }
            }
        }

        if (correction_suggestion == null && query.trim().lowercase() == "recursion") {
            correction_suggestion = query
        }

        return@withContext Result.success(SearchResults(category_layouts, correction_suggestion))
    }
}

private data class YoutubeiSearchResponse(
    val contents: Contents
) {
    data class Contents(val tabbedSearchResultsRenderer: TabbedSearchResultsRenderer)
    data class TabbedSearchResultsRenderer(val tabs: List<Tab>)
    data class Tab(val tabRenderer: TabRenderer)
    data class TabRenderer(val content: Content?)
    data class Content(val sectionListRenderer: SectionListRenderer)
    data class SectionListRenderer(
        val contents: List<YoutubeiShelf>?,
        val header: ChipCloudRendererHeader?
    )
}

data class ChipCloudRenderer(val chips: List<Chip>)
data class Chip(val chipCloudChipRenderer: ChipCloudChipRenderer)
data class ChipCloudChipRenderer(val navigationEndpoint: NavigationEndpoint, val text: TextRuns?)

data class ChipCloudRendererHeader(val chipCloudRenderer: ChipCloudRenderer?)
