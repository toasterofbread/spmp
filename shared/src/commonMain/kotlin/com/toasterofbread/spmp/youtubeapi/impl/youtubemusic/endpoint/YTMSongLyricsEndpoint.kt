package com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.endpoint

import com.toasterofbread.spmp.youtubeapi.endpoint.SongLyricsEndpoint
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.YoutubeMusicApi
import com.toasterofbread.spmp.youtubeapi.model.YoutubeiBrowseResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

class YTMSongLyricsEndpoint(override val api: YoutubeMusicApi): SongLyricsEndpoint() {
    private class LyricsBrowseResponse(val contents: YoutubeiBrowseResponse.Content)

    override suspend fun getSongLyrics(lyrics_id: String): Result<String> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .endpointUrl("/youtubei/v1/browse")
            .addAuthApiHeaders()
            .postWithBody(
                mapOf(
                    "browseId" to lyrics_id
                )
            )
            .build()

        val result = api.performRequest(request)
        val data: LyricsBrowseResponse = result.parseJsonResponse {
            return@withContext Result.failure(it)
        }

        val lyrics_text: String? =
            data.contents.sectionListRenderer?.contents?.firstOrNull()?.musicDescriptionShelfRenderer?.description?.firstTextOrNull()

        if (lyrics_text == null) {
            return@withContext Result.failure(RuntimeException("Browse response for ID $lyrics_id contains no lyrics"))
        }

        return@withContext Result.success(lyrics_text)
    }
}
