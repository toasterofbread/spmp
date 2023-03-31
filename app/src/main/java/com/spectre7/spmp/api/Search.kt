package com.spectre7.spmp.api

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.graphics.vector.ImageVector
import com.spectre7.spmp.R
import com.spectre7.spmp.api.DataApi.Companion.addYtHeaders
import com.spectre7.spmp.api.DataApi.Companion.ytUrl
import com.spectre7.spmp.model.Artist
import com.spectre7.spmp.model.MediaItem
import com.spectre7.spmp.model.Playlist
import com.spectre7.spmp.model.Song
import com.spectre7.spmp.ui.component.MediaItemLayout
import com.spectre7.utils.getString
import com.spectre7.utils.log
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

enum class SearchType {
    SONG, VIDEO, PLAYLIST, ALBUM, ARTIST;

    fun getIcon(): ImageVector {
        return when (this) {
            SONG -> MediaItem.Type.SONG.getIcon()
            PLAYLIST -> MediaItem.Type.ARTIST.getIcon()
            ARTIST -> MediaItem.Type.PLAYLIST.getIcon()
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

fun searchYoutubeMusic(query: String, params: String?): Result<List<Pair<MediaItemLayout, SearchFilter?>>> {
    val params_str: String = if (params != null) "\"$params\"" else "null"
    val request = Request.Builder()
        .ytUrl("/youtubei/v1/search")
        .addYtHeaders()
        .post(DataApi.getYoutubeiRequestBody("""{ "query": "$query", "params": $params_str }"""))
        .build()

    val result = DataApi.request(request)
    if (result.isFailure) {
        return result.cast()
    }

    val stream = result.getOrThrow().body!!.charStream()
    val parsed: YoutubeiSearchResponse = DataApi.klaxon.parse(stream)!!
    stream.close()

    val ret: MutableList<Pair<MediaItemLayout, SearchFilter?>> = mutableListOf()
    val tab = parsed.contents.tabbedSearchResultsRenderer.tabs.first().tabRenderer

    val chips = tab.content!!.sectionListRenderer.header!!.chipCloudRenderer.chips

    for (category in tab.content.sectionListRenderer.contents!!.withIndex()) {

        val card = category.value.musicCardShelfRenderer
        if (card != null) {
            ret.add(Pair(
                MediaItemLayout(card.header.musicCardShelfHeaderBasicRenderer!!.title.first_text, null, items = mutableListOf(card.getMediaItem()), type = MediaItemLayout.Type.CARD),
                null
            ))
            continue
        }

        val shelf = category.value.musicShelfRenderer ?: continue
        val search_params = if (category.index == 0) null else chips[category.index - 1].chipCloudChipRenderer.navigationEndpoint.searchEndpoint!!.params
        val items = shelf.contents.mapNotNull { it.toMediaItem() }.toMutableList()

        ret.add(Pair(
            MediaItemLayout(shelf.title!!.first_text, null, items = items),
            search_params?.let {
                val item = items.firstOrNull() ?: return@let null
                SearchFilter(when (item) {
                    is Song -> if (item.song_type == Song.SongType.VIDEO) SearchType.VIDEO else SearchType.SONG
                    is Artist -> SearchType.ARTIST
                    is Playlist -> when (item.playlist_type) {
                        Playlist.PlaylistType.ALBUM -> SearchType.ALBUM
                        else -> SearchType.PLAYLIST
                    }
                    else -> throw NotImplementedError(item.type.toString())
                }, it)
            }
        ))
    }

    return Result.success(ret)
}