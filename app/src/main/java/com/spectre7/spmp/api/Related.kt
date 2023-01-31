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

fun getMediaItemRelated(item: MediaItem): Result<List<List<RelatedGroup<MediaItem>>>> {

    var error: Result<List<List<RelatedGroup<MediaItem>>>>? = null
    fun loadBrowseEndpoint(browse_endpoint: MediaItem.BrowseEndpoint): List<RelatedGroup<MediaItem>>? {
        val request = Request.Builder()
            .url("https://music.youtube.com/youtubei/v1/browse")
            .headers(getYTMHeaders())
            .post(getYoutubeiRequestBody(
                """
            {
                "browse": "${browse_endpoint.id}"
            }
        """
            ))
            .build()

        val response = client.newCall(request).execute()
        if (response.code != 200) {
            error = Result.failure(response)
            return null
        }

        val parsed = klaxon.parseArray<RelatedGroup<RelatedItem>>(response.body!!.charStream())!!

        return List(parsed.size) { i ->
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

                        val media_item: MediaItem
                        if (!item.browseId.startsWith("MPREb_")) {
                            media_item = Artist.fromId(item.browseId).apply { addBrowseEndpoint(item.browseId, MediaItem.BrowseEndpoint.Type.ARTIST) }
                        }
                        else {
                            media_item = Playlist.fromId(item.browseId).apply { addBrowseEndpoint(item.browseId, MediaItem.BrowseEndpoint.Type.ALBUM) }
                        }

                        media_item.loadData()
                    }
                    else {
                        throw NotImplementedError(item.toString())
                    }
                }
            )
        }
    }

    val ret = mutableListOf<List<RelatedGroup<MediaItem>>>()
    for (endpoint in item.related_endpoints) {
        ret.add(loadBrowseEndpoint(endpoint) ?: return error!!)
    }

    return Result.success(ret)
}