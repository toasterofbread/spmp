package com.toasterofbread.spmp.youtubeapi.impl.youtubemusic

import SpMp
import com.toasterofbread.spmp.model.mediaitem.MediaItemData
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistData
import com.toasterofbread.spmp.model.mediaitem.enums.PlaylistType
import com.toasterofbread.spmp.model.mediaitem.enums.SongType
import com.toasterofbread.spmp.model.mediaitem.layout.MediaItemLayout
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylistData
import com.toasterofbread.spmp.model.mediaitem.song.SongData
import com.toasterofbread.spmp.resources.uilocalisation.LocalisedYoutubeString
import com.toasterofbread.spmp.youtubeapi.endpoint.SearchEndpoint
import com.toasterofbread.spmp.youtubeapi.endpoint.SearchFilter
import com.toasterofbread.spmp.youtubeapi.endpoint.SearchResults
import com.toasterofbread.spmp.youtubeapi.endpoint.SearchType
import com.toasterofbread.spmp.youtubeapi.fromJson
import com.toasterofbread.spmp.youtubeapi.getReader
import com.toasterofbread.spmp.youtubeapi.model.NavigationEndpoint
import com.toasterofbread.spmp.youtubeapi.model.TextRuns
import com.toasterofbread.spmp.youtubeapi.model.YoutubeiShelf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.Reader

class SearchEndpointImpl(override val api: YoutubeMusicApi): SearchEndpoint() {
    override suspend fun searchMusic(query: String, params: String?): Result<SearchResults> = withContext(Dispatchers.IO) {
        val hl = SpMp.data_language
        val request = Request.Builder()
            .endpointUrl("/youtubei/v1/search")
            .addAuthApiHeaders()
            .postWithBody(mapOf("query" to query, "params" to params))
            .build()

        val result = api.performRequest(request)
        val response = result.getOrNull() ?: return@withContext result.cast()

        val reader: Reader = response.getReader(api)
        val parsed: YoutubeiSearchResponse = try {
            api.gson.fromJson(reader)
        }
        catch (e: Throwable) {
            return@withContext Result.failure(e)
        }
        finally {
            reader.close()
        }

        val tab = parsed.contents.tabbedSearchResultsRenderer.tabs.first().tabRenderer

        var correction_suggestion: String? = null
        val categories = tab.content!!.sectionListRenderer.contents!!.filter { shelf ->
            if (shelf.itemSectionRenderer != null) {
                shelf.itemSectionRenderer.contents.firstOrNull()?.didYouMeanRenderer?.correctedQuery?.first_text?.also {
                    correction_suggestion = it
                }
                false
            }
            else {
                true
            }
        }

        val category_layouts: MutableList<Pair<MediaItemLayout, SearchFilter?>> = mutableListOf()
        val chips = tab.content.sectionListRenderer.header!!.chipCloudRenderer!!.chips

        for (category in categories.withIndex()) {
            val card = category.value.musicCardShelfRenderer
            if (card != null) {
                category_layouts.add(Pair(
                    MediaItemLayout(
                        mutableListOf(card.getMediaItem()),
                        LocalisedYoutubeString.Type.SEARCH_PAGE.create(card.header.musicCardShelfHeaderBasicRenderer!!.title!!.first_text),
                        null,
                        type = MediaItemLayout.Type.CARD
                    ),
                    null
                ))
                continue
            }

            val shelf = category.value.musicShelfRenderer ?: continue
            val items = shelf.contents?.mapNotNull { it.toMediaItemData(hl)?.first }?.toMutableList() ?: continue
            val search_params = if (category.index == 0) null else chips[category.index - 1].chipCloudChipRenderer.navigationEndpoint.searchEndpoint!!.params

            category_layouts.add(Pair(
                MediaItemLayout(items, LocalisedYoutubeString.Type.SEARCH_PAGE.create(shelf.title!!.first_text), null),
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

        api.database.transaction {
            for (category in category_layouts) {
                for (item in category.first.items) {
                    (item as MediaItemData).saveToDatabase(api.database)
                }
            }
        }

        return@withContext Result.success(SearchResults(correction_suggestion, category_layouts))
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
