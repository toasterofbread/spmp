package com.toasterofbread.spmp.api

import com.toasterofbread.spmp.api.Api.Companion.addYtHeaders
import com.toasterofbread.spmp.api.Api.Companion.ytUrl
import com.toasterofbread.spmp.api.model.YoutubeiBrowseResponse
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

data class ArtistWithParamsRow(val title: String?, val items: List<MediaItem>)

suspend fun loadArtistWithParams(artist_id: String, browse_params: String): Result<List<ArtistWithParamsRow>> = withContext(Dispatchers.IO) {
    val hl = SpMp.data_language
    val request = Request.Builder()
        .ytUrl("/youtubei/v1/browse")
        .addYtHeaders()
        .post(Api.getYoutubeiRequestBody(
            mapOf(
                "browseId" to artist_id,
                "params" to browse_params
            )
        ))
        .build()

    val result = Api.request(request)
    val parse_result: YoutubeiBrowseResponse = result.parseJsonResponse {
        return@withContext Result.failure(it)
    }

    val rows = parse_result.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents
        ?: emptyList()

    return@withContext Result.success(
        rows.map { row ->
            ArtistWithParamsRow(
                title = row.title?.text,
                items = row.getMediaItemsOrNull(hl).orEmpty()
            )
        }
    )
}
