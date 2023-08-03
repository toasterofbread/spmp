package com.toasterofbread.spmp.api

import SpMp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.graphics.vector.ImageVector
import com.toasterofbread.Database
import com.toasterofbread.spmp.api.Api.Companion.addYtHeaders
import com.toasterofbread.spmp.api.Api.Companion.getStream
import com.toasterofbread.spmp.api.Api.Companion.ytUrl
import com.toasterofbread.spmp.api.model.NavigationEndpoint
import com.toasterofbread.spmp.api.model.TextRuns
import com.toasterofbread.spmp.api.model.YoutubeiShelf
import com.toasterofbread.spmp.model.mediaitem.ArtistData
import com.toasterofbread.spmp.model.mediaitem.MediaItemData
import com.toasterofbread.spmp.model.mediaitem.PlaylistData
import com.toasterofbread.spmp.model.mediaitem.SongData
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.model.mediaitem.enums.PlaylistType
import com.toasterofbread.spmp.model.mediaitem.enums.SongType
import com.toasterofbread.spmp.resources.uilocalisation.LocalisedYoutubeString
import com.toasterofbread.spmp.ui.component.mediaitemlayout.MediaItemLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

data class YoutubeiSearchResponse(
    val contents: Contents
) {
    data class Contents(val tabbedSearchResultsRenderer: TabbedSearchResultsRenderer)
    data class TabbedSearchResultsRenderer(val tabs: List<Tab>)
    data class Tab(val tabRenderer: TabRenderer)
    data class TabRenderer(val content: Content? = null)
    data class Content(val sectionListRenderer: SectionListRenderer)
    data class SectionListRenderer(
        val contents: List<YoutubeiShelf>? = null,
        val header: ChipCloudRendererHeader? = null
    )
    data class ChipCloudRenderer(val chips: List<Chip>)
    data class Chip(val chipCloudChipRenderer: ChipCloudChipRenderer)
    data class ChipCloudChipRenderer(val navigationEndpoint: NavigationEndpoint, val text: TextRuns? = null)
}

data class ChipCloudRendererHeader(val chipCloudRenderer: YoutubeiSearchResponse.ChipCloudRenderer? = null)

enum class SearchType {
    SONG, VIDEO, PLAYLIST, ALBUM, ARTIST;

    fun getIcon(): ImageVector {
        return when (this) {
            SONG -> MediaItemType.SONG.getIcon()
            PLAYLIST -> MediaItemType.ARTIST.getIcon()
            ARTIST -> MediaItemType.PLAYLIST_ACC.getIcon()
            VIDEO -> Icons.Filled.PlayArrow
            ALBUM -> Icons.Filled.Album
        }
    }

    fun getDefaultParams(): String {
        return when (this) {
            SONG -> "EgWKAQIIAUICCAFqDBAOEAoQAxAEEAkQBQ%3D%3D"
            PLAYLIST -> "EgWKAQIoAUICCAFqDBAOEAoQAxAEEAkQBQ%3D%3D"
            ARTIST -> "EgWKAQIgAUICCAFqDBAOEAoQAxAEEAkQBQ%3D%3D"
            VIDEO -> "EgWKAQIQAUICCAFqDBAOEAoQAxAEEAkQBQ%3D%3D"
            ALBUM -> "EgWKAQIYAUICCAFqDBAOEAoQAxAEEAkQBQ%3D%3D"
        }
    }
}

data class SearchFilter(val type: SearchType, val params: String)
data class SearchResults(val suggested_correction: String?, val categories: List<Pair<MediaItemLayout, SearchFilter?>>)

suspend fun searchYoutubeMusic(query: String, params: String?, db: Database): Result<SearchResults> = withContext(Dispatchers.IO) {
    val hl = SpMp.data_language
    val request = Request.Builder()
        .ytUrl("/youtubei/v1/search")
        .addYtHeaders()
        .post(Api.getYoutubeiRequestBody(mapOf("query" to query, "params" to params)))
        .build()

    val result = Api.request(request)
    val response = result.getOrNull() ?: return@withContext result.cast()

    val stream = response.getStream()
    val parsed: YoutubeiSearchResponse = try {
        Api.klaxon.parse(stream)!!
    }
    catch (e: Throwable) {
        return@withContext Result.failure(e)
    }
    finally {
        stream.close()
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
                    is PlaylistData -> when (item.playlist_type) {
                        PlaylistType.ALBUM -> SearchType.ALBUM
                        else -> SearchType.PLAYLIST
                    }
                    else -> throw NotImplementedError(item.getType().toString())
                }, it)
            }
        ))
    }

    db.transaction {
        for (category in category_layouts) {
            for (item in category.first.items) {
                (item as MediaItemData).saveToDatabase(db)
            }
        }
    }

    return@withContext Result.success(SearchResults(correction_suggestion, category_layouts))
}
