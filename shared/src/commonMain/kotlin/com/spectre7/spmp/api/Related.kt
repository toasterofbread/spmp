package com.spectre7.spmp.api

import com.spectre7.spmp.api.Api.Companion.addYtHeaders
import com.spectre7.spmp.api.Api.Companion.ytUrl
import com.spectre7.spmp.model.mediaitem.*
import okhttp3.Request

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

private suspend fun loadBrowseEndpoint(browse_endpoint: MediaItemBrowseEndpoint): Result<List<RelatedGroup<MediaItem>>> {
    val request = Request.Builder()
        .ytUrl("/youtubei/v1/browse")
        .addYtHeaders()
        .post(Api.getYoutubeiRequestBody("""{"browse": "${browse_endpoint.id}"}"""))
        .build()

    val result = Api.request(request)
    if (result.isFailure) {
        return result.cast()
    }

    val parsed = Api.klaxon.parseArray<RelatedGroup<RelatedItem>>(result.getOrThrowHere().body!!.charStream())!!

    return Result.success(List(parsed.size) { i ->
        val group = parsed[i]

        RelatedGroup(
            group.title,
            List(group.contents.size) { j ->
                val item = group.contents[j]

                if (item.videoId != null) {
                    Song.fromId(item.videoId)
                }
                else if (item.playlistId != null) {
                    AccountPlaylist.fromId(item.playlistId)
                }
                else if (item.browseId != null) {

                    val media_item: MediaItem
                    if (!item.browseId.startsWith("MPREb_")) {
                        media_item = Artist.fromId(item.browseId).apply { addBrowseEndpoint(item.browseId, MediaItemBrowseEndpoint.Type.ARTIST) }
                    }
                    else {
                        media_item = AccountPlaylist.fromId(item.browseId).apply { addBrowseEndpoint(item.browseId, MediaItemBrowseEndpoint.Type.ALBUM) }
                    }

                    media_item
                }
                else {
                    throw NotImplementedError(item.toString())
                }
            }
        )
    })
}

suspend fun getMediaItemRelated(item: MediaItem): Result<List<List<RelatedGroup<MediaItem>>>> {
    val ret = mutableListOf<List<RelatedGroup<MediaItem>>>()
    for (endpoint in item.related_endpoints) {
        val load_result = loadBrowseEndpoint(endpoint)
        if (load_result.isFailure) {
            return load_result.cast()
        }

        ret.add(load_result.getOrThrow())
    }

    return Result.success(ret)
}