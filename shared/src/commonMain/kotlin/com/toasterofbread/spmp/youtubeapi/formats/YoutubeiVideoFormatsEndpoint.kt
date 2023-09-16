package com.toasterofbread.spmp.youtubeapi.formats

import com.toasterofbread.spmp.youtubeapi.YoutubeApi
import com.toasterofbread.spmp.youtubeapi.YoutubeFormatsResponse
import com.toasterofbread.spmp.youtubeapi.YoutubeVideoFormat
import com.toasterofbread.spmp.youtubeapi.buildVideoFormatsRequest
import com.toasterofbread.spmp.youtubeapi.fromJson
import java.io.IOException

class YoutubeiVideoFormatsEndpoint(override val api: YoutubeApi): VideoFormatsEndpoint() {
    override fun getVideoFormats(id: String, filter: ((YoutubeVideoFormat) -> Boolean)?): Result<List<YoutubeVideoFormat>> {
        val formats: YoutubeFormatsResponse =
            api.performRequest(buildVideoFormatsRequest(id))
                .fold(
                    { response ->
                        response.use {
                            api.gson.fromJson(it.body!!.charStream())
                        }
                    },
                    { error ->
                        return Result.failure(error)
                    }
                )

        if (formats.streamingData == null) {
            return PipedVideoFormatsEndpoint(api).getVideoFormats(id, filter)
        }

        val streaming_data: YoutubeFormatsResponse.StreamingData = formats.streamingData
        return Result.success(
            streaming_data.adaptiveFormats.filter { format ->
                filter?.invoke(format) ?: true
            }
        )
    }
}
