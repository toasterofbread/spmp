package com.spectre7.spmp.api

import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

private const DEFAULT_CLIENT = """{"clientName":"ANDROID","clientVersion":"16.50","visitorData":null,"hl":"en"}"""
private const NO_CONTENT_WARNING_CLIENT = """{"clientName":"TVHTML5_SIMPLY_EMBEDDED_PLAYER","clientVersion":"2.0","visitorData":null,"hl":"en"}"""

private class GetDownloadUrlResult(val playabilityStatus: PlayabilityStatus, val streamingData: StreamingData? = null) {
    class StreamingData(val adaptiveFormats: List<Format>)
    class Format(val itag: Int?, val url: String? = null)
    class PlayabilityStatus(val status: String, val reason: String? = null)

    fun isPlayable(): Boolean {
        return playabilityStatus.status == "OK"
    }

    fun getError(): String {
        return playabilityStatus.reason.toString()
    }
}

fun getVideoDownloadUrl(id: String): Result<String> {

    fun attemptGetDownloadUrl(client_str: String): String? {
        
        val request = Request.Builder()
            .url("https://www.youtube.com/youtubei/v1/player?key=AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8&prettyPrint=false")
            .post("""{"context":{"client":$client_str},"videoId":"$id","playlistId":null}""".toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()
        
        val response = client.newCall(request).execute()
        if (response.code != 200) {
            return null
        }

        val result = klaxon.parse<GetDownloadUrlResult>(response.body!!.charStream())!!
        if (!result.isPlayable()) {
            return null
        }

        for (format in result.streamingData!!.adaptiveFormats) {
            // TODO | Format config
            if (format.itag == 140) {
                return format.url
            }
        }

        return null
    }

    var url = attemptGetDownloadUrl(DEFAULT_CLIENT)
    if (url != null) {
        return Result(url)
    }

    url = attemptGetDownloadUrl(NO_CONTENT_WARNING_CLIENT)
    if (url != null) {
        return Result(url)
    }

    // Use yt-dlp if other methods fail (is it too slow to use by default?)
    val formats = ytd.callAttr("extract_info", id, false).callAttr("get", "formats").asList()
    for (format in formats) {
        if (format.callAttr("get", "format_id").toString() == "140") {
            return Result(format.callAttr("get", "url").toString())
        }
    }

    return Result(null, RuntimeError("Could not find download url for video ID $id"))
}