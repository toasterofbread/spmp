package com.spectre7.spmp.api

import okhttp3.Request
import com.spectre7.spmp.model.*

class RelatedGroup<T>(val title: String, val contents: List<T>)
private data class RelatedItem(
    val videoId: String? = null,
    val playlistId: String? = null,
    val browseId: String? = null,
    val artists: List<IdItem> = listOf(),
    val album: IdItem? = null
) {
    class IdItem(val id: String)
}

fun getMediaItemRelated(item: MediaItem): Result<List<RelatedGroup<MediaItem>>> {

    if (item.browse_endpoint == null) {
        return Result.success(null)
    }

    val url = "https://music.youtube.com/youtubei/v1/browse"
    val request = Request.Builder()
        .url(if (ctoken == null) url else "$url?ctoken=$ctoken&continuation=$ctoken&type=next")
        .headers(getYTMHeaders())
        .post(getYoutubeiRequestBody(
        """
            {
                "browse": "${item.browse_endpoint.id}"
            }
        """
        ))
        .build()
    
    val response = client.newCall(request).execute()
    if (response.code != 200) {
        return Result.failure(response)
    }

    val parsed = klaxon.parseArray<RelatedGroup<RelatedItem>>(response.body!!.charStream())!!

    val ret = List(parsed.size) { i ->
        val group = parsed[i]
        
        RelatedGroup(
            group.title,
            List(group.contents.size) { j ->
                val item = group.contents[j]

                if (item.videoId != null) {
                    Song.fromId(item.videoId).loadData()
                }
                else if (item.playlistId != null) {
                    Playlist.fromId(item.playlistId).loadData()
                }
                else if (item.browseId != null) {

                    val converted = convertBrowseId(item.browseId)
                    if (!converted.success) {
                        return Result.failure(converted.exception)
                    }

                    val media_item: MediaItem
                    if (converted.data == item.browseId) {
                        media_item = Artist.fromId(item.data).apply { setBrowseEndpoint(item.browseId, MediaItem.BrowseEndpoint.Type.ARTIST) }
                    }
                    else {
                        media_item = Playlist.fromId(item.data).apply { setBrowseEndpoint(item.browseId, MediaItem.BrowseEndpoint.Type.ALBUM) }
                    }

                    media_item.loadData()
                }
                else {
                    throw NotImplementedError(item)
                }
            }
        )
    }

    return Result.success(ret)
}