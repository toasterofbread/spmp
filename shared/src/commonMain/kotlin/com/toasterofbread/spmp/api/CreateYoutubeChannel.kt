package com.toasterofbread.spmp.api

import com.toasterofbread.spmp.api.Api.Companion.getStream
import com.toasterofbread.spmp.api.Api.Companion.ytUrl
import com.toasterofbread.spmp.model.mediaitem.Artist
import com.toasterofbread.spmp.model.mediaitem.ArtistRef
import com.toasterofbread.spmp.ui.layout.youtubemusiclogin.CreateChannelResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.Headers.Companion.toHeaders
import okhttp3.Request

suspend fun createYoutubeChannel(
    cookie: String,
    headers: Map<String, String>,
    channel_creation_token: String,
    params: Map<String, String>
): Result<Artist> = withContext(Dispatchers.IO) {
    val request = Request.Builder()
        .ytUrl("/youtubei/v1/channel/create_channel")
        .headers(headers.toHeaders())
        .header("COOKIE", cookie)
        .post(Api.getYoutubeiRequestBody(
            mutableMapOf(
                "channelCreationToken" to channel_creation_token
            ).apply {
                for (param in params) {
                    if (param.value.isNotBlank()) {
                        put(param.key, param.value)
                    }
                }
            }
        ))
        .build()

    val result = Api.request(request)
    val stream = result.getOrNull()?.getStream(false) ?: return@withContext result.cast()

    try {
        val response: CreateChannelResponse = Api.klaxon.parse(stream)!!
        delay(1000) // Give YouTube time to update the account before we load it
        return@withContext Result.success(ArtistRef(response.navigationEndpoint.browseEndpoint.browseId))
    } catch (e: Throwable) {
        return@withContext Result.failure(e)
    } finally {
        stream.close()
    }
}
