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

    fun toMediaItem(): MediaItem {
        if (videoId != null) {
            return Song.fromId(videoId)
        }
        else if (playlistId != null) {
            return AccountPlaylist.fromId(playlistId)
        }
        else if (browseId != null) {
            if (!browseId.startsWith("MPREb_")) {
                return Artist.fromId(browseId).apply { addBrowseEndpoint(browseId, MediaItemBrowseEndpoint.Type.ARTIST) }
            }
            else {
                return AccountPlaylist.fromId(browseId).apply { addBrowseEndpoint(browseId, MediaItemBrowseEndpoint.Type.ALBUM) }
            }
        }

        throw NotImplementedError(toString())
    }
}

private suspend fun loadBrowseEndpoint(browse_endpoint: MediaItemBrowseEndpoint): Result<List<RelatedGroup<MediaItem>>> = withContext(Dispatchers.IO) {
    val request = Request.Builder()
        .ytUrl("/youtubei/v1/browse")
        .addYtHeaders()
        .post(Api.getYoutubeiRequestBody(mapOf("browse" to browse_endpoint.id))
        .build()

    val result = Api.request(request)
    if (result.isFailure) {
        return@withContext result.cast()
    }

    val stream = result.getOrThrow().getStream()
    
    try {
        val parsed = Api.klaxon.parseArray<RelatedGroup<RelatedItem>>(stream)!!
        return@withContext Result.success(List(parsed.size) { i ->
            val group = parsed[i]

            RelatedGroup(
                group.title,
                group.contents.map { it.toMediaItem() }
            )
        })
    }
    catch (e: Throwable) {
        return@withContext Result.failure(e)
    }
    finally {
        stream.close()
    }
}

suspend fun getMediaItemRelated(item: MediaItem): Result<List<RelatedGroup<MediaItem>>> {
    val ret: MutableList<RelatedGroup<MediaItem>> = mutableListOf()
    for (endpoint in item.related_endpoints) {
        val load_result = loadBrowseEndpoint(endpoint)
        if (load_result.isFailure) {
            return load_result.cast()
        }

        ret.addAll(load_result.getOrThrow())
    }

    return Result.success(ret)
}
