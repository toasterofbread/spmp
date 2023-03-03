package com.spectre7.spmp.api

import com.spectre7.spmp.R
import com.spectre7.spmp.model.MediaItem
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

data class YTMusicSearchResults(
    val songs: List<Song>,
    val videos: List<Song>,
    val artists: List<Artist>,
    val playlists: List<Playlist>,
    val albums: List<Playlist>
)

fun searchYoutubeMusic(query: String, type: MediaItem.Type, limit: Int = 10): Result<YTMusicSearchResults> {
    TODO()
}