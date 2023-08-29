package com.toasterofbread.spmp.youtubeapi

import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.YoutubeMusicApi
import okhttp3.Request

data class YoutubeVideoFormat(
    val itag: Int? = null,
    val mimeType: String,
    val bitrate: Int,
    val signatureCipher: String? = null,
    val url: String? = null,
) {
    val identifier: Int = itag ?: bitrate
    var stream_url: String? = url
    val audio_only: Boolean get() = mimeType.startsWith("audio")
}

internal fun YoutubeMusicApi.checkYoutubeVideoStreamUrl(url: String): Boolean {
    val request = Request.Builder()
        .url(url)
        .header("Cookie", "CONSENT=YES+1")
        .header("User-Agent", getUserAgent())
        .build()

    val response = performRequest(request, true).getOrNull() ?: return false
    val is_invalid = response.body!!.contentType().toString().startsWith("text/")
    response.close()

    return response.code == 200 && !is_invalid
}

internal data class YoutubeFormatsResponse(
    val playabilityStatus: PlayabilityStatus,
    val streamingData: StreamingData? = null,
) {
    val is_ok: Boolean get() = playabilityStatus.status == "OK"
    data class StreamingData(val formats: List<YoutubeVideoFormat>, val adaptiveFormats: List<YoutubeVideoFormat>)
    data class PlayabilityStatus(val status: String)
}

internal fun YoutubeApi.Endpoint.buildVideoFormatsRequest(id: String, alt: Boolean, api: YoutubeMusicApi): Request {
    return Request.Builder()
        .url("${api.api_url}/youtubei/v1/player?key=${getString("yt_i_api_key")}")
        .postWithBody(
            mapOf(
                "videoId" to id,
                "playlistId" to null
            ),
            if (alt) YoutubeApi.PostBodyContext.ALT else YoutubeApi.PostBodyContext.BASE
        )
        .build()
}
