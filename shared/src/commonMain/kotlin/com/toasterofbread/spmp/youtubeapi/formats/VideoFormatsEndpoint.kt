package com.toasterofbread.spmp.youtubeapi.formats

import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.youtubeapi.YoutubeApi
import com.toasterofbread.spmp.youtubeapi.YoutubeVideoFormat

enum class VideoFormatsEndpointType {
    YOUTUBEI,
    PIPED,
    NEWPIPE;
  
    fun instantiate(api: YoutubeApi): VideoFormatsEndpoint =
        when(this) {
            YOUTUBEI -> YoutubeiVideoFormatsEndpoint(api)
            PIPED -> PipedVideoFormatsEndpoint(api)
            NEWPIPE -> NewPipeVideoFormatsEndpoint(api)
        }

    fun getReadable(): String =
        when(this) {
            YOUTUBEI -> getString("video_format_endpoint_youtubei")
            PIPED -> getString("video_format_endpoint_piped")
            NEWPIPE -> getString("video_format_endpoint_newpipe")
        }

    companion object {
        val DEFAULT: VideoFormatsEndpointType = YOUTUBEI
    }
}

abstract class VideoFormatsEndpoint: YoutubeApi.Endpoint() {
    abstract suspend fun getVideoFormats(id: String, filter: ((YoutubeVideoFormat) -> Boolean)? = null): Result<List<YoutubeVideoFormat>>

    class YoutubeMusicPremiumContentException(message: String?): RuntimeException(message)
}
