package com.toasterofbread.spmp.youtubeapi.endpoint

import com.toasterofbread.spmp.model.mediaitem.Artist
import com.toasterofbread.spmp.youtubeapi.YoutubeApi
import okhttp3.Headers

abstract class CreateYoutubeChannelEndpoint: YoutubeApi.Endpoint() {
    abstract suspend fun createYoutubeChannel(
        headers: Headers,
        channel_creation_token: String,
        params: Map<String, String>
    ): Result<Artist>
}
