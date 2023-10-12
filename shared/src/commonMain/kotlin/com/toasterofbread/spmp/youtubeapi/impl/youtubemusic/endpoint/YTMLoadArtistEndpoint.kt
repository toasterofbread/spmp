package com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.endpoint

import SpMp
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistData
import com.toasterofbread.spmp.platform.getDataLanguage
import com.toasterofbread.spmp.youtubeapi.YoutubeApi
import com.toasterofbread.spmp.youtubeapi.endpoint.LoadArtistEndpoint
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.DataParseException
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.YoutubeMusicApi
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.processDefaultResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

class YTMLoadArtistEndpoint(override val api: YoutubeMusicApi): LoadArtistEndpoint() {
    override suspend fun loadArtist(artist_data: ArtistData): Result<ArtistData> = withContext(Dispatchers.IO) {
        val hl = api.context.getDataLanguage()
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

        val response = api.performRequest(request).getOrElse {
            return@withContext Result.failure(DataParseException.ofYoutubeJsonRequest(request, api, cause = it))
        }

        response.use {
            processDefaultResponse(artist_data, response, hl, api).onFailure {
                return@withContext Result.failure(DataParseException.ofYoutubeJsonRequest(request, api, cause = it))
            }
        }

        artist_data.loaded = true
        artist_data.saveToDatabase(api.database)

        return@withContext Result.success(artist_data)
    }
}
