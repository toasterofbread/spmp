package com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.endpoint

import com.toasterofbread.spmp.model.mediaitem.Artist
import com.toasterofbread.spmp.model.mediaitem.ArtistRef
import com.toasterofbread.spmp.ui.layout.youtubemusiclogin.CreateChannelResponse
import com.toasterofbread.spmp.youtubeapi.endpoint.CreateYoutubeChannelEndpoint
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.YoutubeMusicApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.Headers
import okhttp3.Request

class YTMCreateYoutubeChannelEndpoint(override val api: YoutubeMusicApi): CreateYoutubeChannelEndpoint() {
    override suspend fun createYoutubeChannel(
        headers: Headers,
        channel_creation_token: String,
        params: Map<String, String>
    ): Result<Artist> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .endpointUrl("/youtubei/v1/channel/create_channel")
            .headers(headers)
            .postWithBody(
                mutableMapOf(
                    "channelCreationToken" to channel_creation_token
                ).apply {
                    for (param in params) {
                        if (param.value.isNotBlank()) {
                            put(param.key, param.value)
                        }
                    }
                }
            )
            .build()

        val result = api.performRequest(request)
        val data: CreateChannelResponse = result.parseJsonResponse {
            return@withContext Result.failure(it)
        }

        // Give YouTube time to update the account before we load it
        delay(1000)

        return@withContext Result.success(ArtistRef(data.navigationEndpoint.browseEndpoint.browseId))
    }
}
