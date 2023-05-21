package com.spectre7.spmp.api

import com.spectre7.spmp.api.DataApi.Companion.addYtHeaders
import com.spectre7.spmp.api.DataApi.Companion.ytUrl
import com.spectre7.spmp.model.*
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

private fun loadBrowseEndpoint(browse_endpoint: MediaItemBrowseEndpoint): Result<List<RelatedGroup<MediaItem>>> {
    val request = Request.Builder()
        .ytUrl("/youtubei/v1/browse")
        .addYtHeaders()
        .post(DataApi.getYoutubeiRequestBody("""{"browse": "${browse_endpoint.id}"}"""))
        .build()

    val result = DataApi.request(request)
    if (result.isFailure) {
        return result.cast()
    }

    val parsed = DataApi.klaxon.parseArray<RelatedGroup<RelatedItem>>(result.getOrThrowHere().body!!.charStream())!!

    return Result.success(List(parsed.size) { i ->
        val group = parsed[i]

        RelatedGroup(
            group.title,
            List(group.contents.size) { j ->
                val item = group.contents[j]

                if (item.videoId != null) {
                    Song.fromId(item.videoId).loadData().getOrThrowHere()!!
                }
                else if (item.playlistId != null) {
                    Playlist.fromId(item.playlistId).loadData().getOrThrowHere()!!
                }
                else if (item.browseId != null) {

                    val media_item: MediaItem
                    if (!item.browseId.startsWith("MPREb_")) {
                        media_item = Artist.fromId(item.browseId).apply { addBrowseEndpoint(item.browseId, MediaItemBrowseEndpoint.Type.ARTIST) }
                    }
                    else {
                        media_item = Playlist.fromId(item.browseId).apply { addBrowseEndpoint(item.browseId, MediaItemBrowseEndpoint.Type.ALBUM) }
                    }

                    media_item.loadData().getOrThrowHere()!!
                }
                else {
                    throw NotImplementedError(item.toString())
                }
            }
        )
    })
}

fun getMediaItemRelated(item: MediaItem): Result<List<List<RelatedGroup<MediaItem>>>> {
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