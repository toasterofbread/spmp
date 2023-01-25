package com.spectre7.spmp.api

import com.spectre7.spmp.model.*
import com.spectre7.spmp.ui.layout.ResourceType

class DataApi {
    companion object {
        data class SearchResults(val items: List<Result>) {
            data class Result(val id: ResultId, val snippet: MediaItem.YTApiDataResponse.Snippet)
            data class ResultId(val kind: String, val videoId: String = "", val channelId: String = "", val playlistId: String = "")
        }

        fun search(request: String, type: ResourceType, max_results: Int = 10, channel_id: String? = null): List<SearchResults.Result> {
            throw NotImplementedError()
//            val parameters = mutableMapOf(
//                "part" to "snippet",
//                "type" to when (type) {
//                    ResourceType.SONG -> "video"
//                    ResourceType.ARTIST -> "channel"
//                    ResourceType.PLAYLIST -> "playlist"
//                },
//                "q" to request,
//                "maxResults" to max_results.toString(),
//                "safeSearch" to "none"
//            )
//            if (channel_id != null) {
//                parameters.put("channelId", channel_id)
//            }
//            val result = requestServer("/yt/search", parameters)
//            result.throwStatus()
//            return klaxon.parse<SearchResults>(result.body)!!.items
        }

        fun getSongRelated(song: Song) {
            // TODO : https://ytmusicapi.readthedocs.io/en/latest/reference.html#ytmusicapi.YTMusic.get_song_related
        }
    }
}

