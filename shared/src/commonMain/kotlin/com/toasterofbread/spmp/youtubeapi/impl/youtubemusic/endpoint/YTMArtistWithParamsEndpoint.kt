package com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.endpoint

import SpMp
import com.toasterofbread.spmp.youtubeapi.endpoint.ArtistWithParamsEndpoint
import com.toasterofbread.spmp.youtubeapi.endpoint.ArtistWithParamsRow
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.YoutubeMusicApi
import com.toasterofbread.spmp.youtubeapi.model.YoutubeiBrowseResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

class YTMArtistWithParamsEndpoint(override val api: YoutubeMusicApi): ArtistWithParamsEndpoint() {
    override suspend fun loadArtistWithParams(
        artist_id: String,
        browse_params: String
    ): Result<List<ArtistWithParamsRow>> = withContext(Dispatchers.IO) {
        val hl = SpMp.data_language
        val request = Request.Builder()
            .endpointUrl("/youtubei/v1/browse")
            .addAuthApiHeaders()
            .postWithBody(
                mapOf(
                    "browseId" to artist_id,
                    "params" to browse_params
                )
            )
            .build()

        val result = api.performRequest(request)
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
}
