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
            .headers(DataApi.getYTMHeaders())
            .post(DataApi.getYoutubeiRequestBody(
                """
            {
                "browse": "${browse_endpoint.id}"
            }
        """
            ))
            .build()

        val result = DataApi.request(request)
        if (result.isFailure) {
            error = result.cast()
            return null
        }

        val parsed = DataApi.klaxon.parseArray<RelatedGroup<RelatedItem>>(result.getOrThrowHere().body!!.charStream())!!

        return List(parsed.size) { i ->
            val group = parsed[i]

            RelatedGroup(
                group.title,
                List(group.contents.size) { j ->
                    val item = group.contents[j]

                    if (item.videoId != null) {
                        Song.fromId(item.videoId).loadData().getOrThrowHere()
                    }
                    else if (item.playlistId != null) {
                        Playlist.fromId(item.playlistId).loadData().getOrThrowHere()
                    }
                    else if (item.browseId != null) {

                        val media_item: MediaItem
                        if (!item.browseId.startsWith("MPREb_")) {
                            media_item = Artist.fromId(item.browseId).apply { addBrowseEndpoint(item.browseId, MediaItem.BrowseEndpoint.Type.ARTIST) }
                        }
                        else {
                            media_item = Playlist.fromId(item.browseId).apply { addBrowseEndpoint(item.browseId, MediaItem.BrowseEndpoint.Type.ALBUM) }
                        }

                        media_item.loadData().getOrThrowHere()
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