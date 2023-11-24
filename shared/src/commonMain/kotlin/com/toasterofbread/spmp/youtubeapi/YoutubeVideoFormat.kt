package com.toasterofbread.spmp.youtubeapi

import okhttp3.Request

data class YoutubeVideoFormat(
    val itag: Int?,
    val mimeType: String,
    val bitrate: Int,
    val url: String?,
    val loudness_db: Float? = null
) {
    val audio_only: Boolean get() = mimeType.startsWith("audio")

    override fun toString(): String {
        return "YoutubeVideoFormat(itag=$itag, mimeType=$mimeType, bitrate=$bitrate, loudness_db=$loudness_db)"
    }
}

internal data class YoutubeFormatsResponse(
    val playabilityStatus: PlayabilityStatus,
    val streamingData: StreamingData?,
    val playerConfig: PlayerConfig?
) {
    data class StreamingData(val formats: List<YoutubeVideoFormat>, val adaptiveFormats: List<YoutubeVideoFormat>)
    data class PlayabilityStatus(val status: String)

    data class PlayerConfig(val audioConfig: AudioConfig?)
    data class AudioConfig(val loudnessDb: Float?)
}

internal suspend fun YoutubeApi.Endpoint.buildVideoFormatsRequest(id: String): Request {
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
