package com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.endpoint

import com.toasterofbread.spmp.model.mediaitem.MediaItemData
import com.toasterofbread.spmp.model.mediaitem.layout.BrowseParamsData
import com.toasterofbread.spmp.platform.getDataLanguage
import com.toasterofbread.spmp.youtubeapi.endpoint.ArtistWithParamsEndpoint
import com.toasterofbread.spmp.youtubeapi.endpoint.ArtistWithParamsRow
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.YoutubeMusicApi
import com.toasterofbread.spmp.youtubeapi.model.YoutubeiBrowseResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

class YTMArtistWithParamsEndpoint(override val api: YoutubeMusicApi): ArtistWithParamsEndpoint() {
    override suspend fun loadArtistWithParams(
        browse_params: BrowseParamsData
    ): Result<List<ArtistWithParamsRow>> = withContext(Dispatchers.IO) {
        val hl = api.context.getDataLanguage()
        val request = Request.Builder()
            .endpointUrl("/youtubei/v1/browse")
            .addAuthApiHeaders()
            .postWithBody(
                mapOf(
                    "browseId" to browse_params.browse_id,
                    "params" to browse_params.browse_params
                )
            )
            .build()

        val result = api.performRequest(request)
        val parse_result: YoutubeiBrowseResponse = result.parseJsonResponse {
            return@withContext Result.failure(it)
        }

        val row_content = parse_result.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents
            ?: emptyList()

        val rows = api.database.transactionWithResult {
            row_content.map { row ->
                val items: List<MediaItemData> = row.getMediaItemsOrNull(hl).orEmpty()
                for (item in items) {
                    item.saveToDatabase(api.database, subitems_uncertain = true)
                }

                ArtistWithParamsRow(
                    title = row.title?.text,
                    items = items
                )
            }
        }

        return@withContext Result.success(rows)
    }
}
