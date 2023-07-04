package com.toasterofbread.spmp.api

import SpMp
import com.toasterofbread.spmp.api.Api.Companion.addYtHeaders
import com.toasterofbread.spmp.api.Api.Companion.getStream
import com.toasterofbread.spmp.api.Api.Companion.ytUrl
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

class RelatedGroup(val title: String, val items: List<MediaItem>?, val description: String?)

private class BrowseResponse(val contents: YoutubeiBrowseResponse.Content)

private suspend fun loadBrowseEndpoint(browse_id: String): Result<List<RelatedGroup>> = withContext(Dispatchers.IO) {
    val hl = SpMp.data_language
    val request = Request.Builder()
        .ytUrl("/youtubei/v1/browse")
        .addYtHeaders()
        .post(Api.getYoutubeiRequestBody(mapOf("browseId" to browse_id)))
        .build()

    val result = Api.request(request)
    if (result.isFailure) {
        return@withContext result.cast()
    }

    val stream = result.getOrThrow().getStream()

    try {
        val parsed: BrowseResponse = Api.klaxon.parse(stream)!!
        return@withContext Result.success(parsed.contents.sectionListRenderer.contents!!.map { group ->
            RelatedGroup(
                group.title!!.text,
                group.getMediaItemsOrNull(hl),
                group.description
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

suspend fun getSongRelated(song: Song): Result<List<RelatedGroup>> {
    return song.getRelatedBrowseId().fold(
        { loadBrowseEndpoint(it) },
        { Result.failure(it) }
    )
}
