package com.spectre7.spmp.api

import com.spectre7.spmp.api.Api.Companion.addYtHeaders
import com.spectre7.spmp.api.Api.Companion.getStream
import com.spectre7.spmp.api.Api.Companion.ytUrl
import com.spectre7.spmp.model.mediaitem.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

suspend fun getGenericFeedViewMorePage(browse_id: String): Result<List<MediaItem>> = withContext(Dispatchers.IO) {

    val hl = SpMp.data_language
    val request = Request.Builder()
        .ytUrl("/youtubei/v1/browse")
        .addYtHeaders()
        .post(Api.getYoutubeiRequestBody(
            mapOf("browseId" to browse_id)
        ))
        .build()

    val result = Api.request(request)
    val stream = result.getOrNull()?.getStream() ?: return@withContext result.cast()

    val parsed: YoutubeiBrowseResponse = Api.klaxon.parse(stream)!!
    stream.close()

    val items = parsed
        .contents!!
        .singleColumnBrowseResultsRenderer
        .tabs
        .first()
        .tabRenderer
        .content!!
        .sectionListRenderer
        .contents!!
        .first()
        .getMediaItems(hl)

    return@withContext Result.success(items)
}
