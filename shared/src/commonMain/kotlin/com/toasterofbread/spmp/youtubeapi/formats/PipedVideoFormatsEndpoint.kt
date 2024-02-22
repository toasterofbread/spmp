package com.toasterofbread.spmp.youtubeapi.formats

import com.toasterofbread.spmp.youtubeapi.YoutubeApi
import com.toasterofbread.spmp.youtubeapi.YoutubeVideoFormat
import com.toasterofbread.spmp.youtubeapi.fromJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.Response
import java.io.Reader

class PipedVideoFormatsEndpoint(override val api: YoutubeApi): VideoFormatsEndpoint() {
    override suspend fun getVideoFormats(id: String, filter: ((YoutubeVideoFormat) -> Boolean)?): Result<List<YoutubeVideoFormat>> = withContext(Dispatchers.IO) {
        val request: Request = Request.Builder()
            .url("https://pipedapi.kavin.rocks/streams/$id")
            .header("User-Agent", "Mozilla/5.0 (X11; Linux x86_64; rv:109.0) Gecko/20100101 Firefox/114.0")
            .build()
        val result: Result<Response> = api.performRequest(request, allow_fail_response = true)

        val response: Response = result.fold(
            { it },
            { return@withContext Result.failure(it) }
        )

        val body: String = response.body?.string() ?: return@withContext Result.failure(NullPointerException())
        val parsed: PipedStreamsResponse
        try {
            parsed = api.gson.fromJson(body)
        }
        catch (e: Throwable) {
            throw RuntimeException("getVideoFormats for $id using Piped API failed $body", e)
        }

        if (!response.isSuccessful) {
            if (parsed.error?.contains("YoutubeMusicPremiumContentException") == true) {
                return@withContext Result.failure(YoutubeMusicPremiumContentException(parsed.message))
            }

            return@withContext Result.failure(RuntimeException(parsed.message))
        }

        return@withContext Result.success(
            parsed.audioStreams?.let { streams ->
                if (filter != null) streams.filter(filter) else streams
            } ?: emptyList()
        )
    }
}

private data class PipedStreamsResponse(
    val error: String?,
    val message: String?,
    val audioStreams: List<YoutubeVideoFormat>?,
    val relatedStreams: List<RelatedStream>?
) {
    data class RelatedStream(val url: String, val type: String)
}
