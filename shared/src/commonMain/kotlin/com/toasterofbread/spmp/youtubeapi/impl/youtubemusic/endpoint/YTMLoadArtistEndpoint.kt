package com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.endpoint

import SpMp
import com.toasterofbread.spmp.model.mediaitem.ArtistData
import com.toasterofbread.spmp.youtubeapi.YoutubeApi
import com.toasterofbread.spmp.youtubeapi.endpoint.LoadArtistEndpoint
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.YoutubeMusicApi
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.processDefaultResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

class YTMLoadArtistEndpoint(override val api: YoutubeMusicApi): LoadArtistEndpoint() {
    override suspend fun loadArtist(artist_data: ArtistData): Result<Unit> = withContext(Dispatchers.IO) {
        val hl = SpMp.data_language
        val request: Request = Request.Builder()
            .endpointUrl("/youtubei/v1/browse")
            .addAuthApiHeaders()
            .postWithBody(
                mapOf(
                    "browseId" to artist_data.id
                ),
                YoutubeApi.PostBodyContext.MOBILE
            )
            .build()

        val result = api.performRequest(request).fold(
            { response ->
                processDefaultResponse(artist_data, response, hl, api)
            },
            { error ->
                Result.failure(error)
            }
        )

        return@withContext result.onSuccess {
            artist_data.loaded = true
            artist_data.saveToDatabase(api.db)
        }
    }
}
