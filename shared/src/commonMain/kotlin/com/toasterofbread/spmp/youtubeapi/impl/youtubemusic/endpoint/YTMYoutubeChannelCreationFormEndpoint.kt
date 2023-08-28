package com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.endpoint

import com.toasterofbread.spmp.youtubeapi.YoutubeApi
import com.toasterofbread.spmp.youtubeapi.endpoint.YoutubeChannelCreationFormEndpoint
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.YoutubeMusicApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Headers
import okhttp3.Request

class YTMYoutubeChannelCreationFormEndpoint(override val api: YoutubeMusicApi): YoutubeChannelCreationFormEndpoint() {
    override suspend fun getForm(
        headers: Headers,
        channel_creation_token: String,
    ): Result<YoutubeAccountCreationForm.ChannelCreationForm> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .endpointUrl("/youtubei/v1/channel/get_channel_creation_form")
            .headers(headers)
            .postWithBody(
                mapOf(
                    "source" to "MY_CHANNEL_CHANNEL_CREATION_SOURCE",
                    "channelCreationToken" to channel_creation_token
                ),
                YoutubeApi.PostBodyContext.UI_LANGUAGE
            )
            .build()

        val result = api.performRequest(request)
        val data: YoutubeAccountCreationForm = result.parseJsonResponse(null) {
            return@withContext Result.failure(it)
        }

        return@withContext Result.success(data.channelCreation.channelCreationForm)
    }
}
