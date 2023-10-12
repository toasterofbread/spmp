package com.toasterofbread.spmp.youtubeapi.formats

import com.toasterofbread.spmp.youtubeapi.YoutubeApi
import com.toasterofbread.spmp.youtubeapi.YoutubeVideoFormat
import com.toasterofbread.spmp.youtubeapi.fromJson
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.cast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

class PipedVideoFormatsEndpoint(override val api: YoutubeApi): VideoFormatsEndpoint() {
    override suspend fun getVideoFormats(id: String, filter: ((YoutubeVideoFormat) -> Boolean)?): Result<List<YoutubeVideoFormat>> = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("https://pipedapi.syncpundit.io/streams/$id").build()
        val result = api.performRequest(request)

        if (result.isFailure) {
            return@withContext result.cast()
        }

        val stream = result.getOrThrow().body!!.charStream()
        val response: PipedStreamsResponse = api.gson.fromJson(stream)!!
        stream.close()

        return@withContext Result.success(response.audioStreams.let { if (filter != null) it.filter(filter) else it })
    }
}

private data class PipedStreamsResponse(
    val audioStreams: List<YoutubeVideoFormat>,
    val relatedStreams: List<RelatedStream>,
) {
    data class RelatedStream(val url: String, val type: String)
}
