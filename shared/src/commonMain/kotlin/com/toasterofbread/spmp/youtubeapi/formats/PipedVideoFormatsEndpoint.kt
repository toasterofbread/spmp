package com.toasterofbread.spmp.youtubeapi.formats

import com.toasterofbread.spmp.youtubeapi.YoutubeVideoFormat
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.YoutubeMusicApi
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.cast
import okhttp3.Request

class PipedVideoFormatsEndpoint(override val api: YoutubeMusicApi): VideoFormatsEndpoint() {
    override fun getVideoFormats(id: String, filter: ((YoutubeVideoFormat) -> Boolean)?): Result<List<YoutubeVideoFormat>> {
        val request = Request.Builder().url("https://pipedapi.syncpundit.io/streams/$id").build()
        val result = api.performRequest(request)

        if (result.isFailure) {
            return result.cast()
        }

        val stream = result.getOrThrow().body!!.charStream()
        val response: PipedStreamsResponse = api.klaxon.parse(stream)!!
        stream.close()

        return Result.success(response.audioStreams.let { if (filter != null) it.filter(filter) else it })
    }
}

private data class PipedStreamsResponse(
    val audioStreams: List<YoutubeVideoFormat>,
    val relatedStreams: List<RelatedStream>,
) {
    data class RelatedStream(val url: String, val type: String)
}
