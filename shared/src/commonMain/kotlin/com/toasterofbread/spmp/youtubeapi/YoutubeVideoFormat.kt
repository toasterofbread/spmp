package com.toasterofbread.spmp.youtubeapi

import okhttp3.Request

data class YoutubeVideoFormat(
    val itag: Int?,
    val mimeType: String,
    val bitrate: Int,
    val url: String?
) {
    val audio_only: Boolean get() = mimeType.startsWith("audio")
}

internal data class YoutubeFormatsResponse(
    val playabilityStatus: PlayabilityStatus,
    val streamingData: StreamingData?,
) {
    data class StreamingData(val formats: List<YoutubeVideoFormat>, val adaptiveFormats: List<YoutubeVideoFormat>)
    data class PlayabilityStatus(val status: String)
}

internal fun YoutubeApi.Endpoint.buildVideoFormatsRequest(id: String): Request {
    return Request.Builder()
        .endpointUrl("/youtubei/v1/player")
        .postWithBody(
            mapOf(
                "videoId" to id,
                "playlistId" to null
            ),
            YoutubeApi.PostBodyContext.ANDROID_MUSIC
        )
        .build()
}
