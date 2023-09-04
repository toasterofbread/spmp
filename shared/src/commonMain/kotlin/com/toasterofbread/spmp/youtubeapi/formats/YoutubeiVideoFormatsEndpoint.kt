package com.toasterofbread.spmp.youtubeapi.formats

import com.toasterofbread.spmp.youtubeapi.YoutubeFormatsResponse
import com.toasterofbread.spmp.youtubeapi.YoutubeVideoFormat
import com.toasterofbread.spmp.youtubeapi.buildVideoFormatsRequest
import com.toasterofbread.spmp.youtubeapi.checkYoutubeVideoStreamUrl
import com.toasterofbread.spmp.youtubeapi.fromJson
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.YoutubeMusicApi
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.cast
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.getOrThrowHere
import java.io.IOException

private const val MAX_RETRIES = 5

class YoutubeiVideoFormatsEndpoint(override val api: YoutubeMusicApi): VideoFormatsEndpoint() {
    override fun getVideoFormats(id: String, filter: ((YoutubeVideoFormat) -> Boolean)?): Result<List<YoutubeVideoFormat>> {
        var result = api.performRequest(buildVideoFormatsRequest(id, false, api))
        var formats: YoutubeFormatsResponse? = null

        if (result.isSuccess) {
            val stream = result.getOrThrow().body!!.charStream()
            formats = api.gson.fromJson(stream)!!
            stream.close()
        }

        if (formats?.streamingData == null) {
            result = api.performRequest(buildVideoFormatsRequest(id, true, api))
            if (result.isFailure) {
                return result.cast()
            }

            val stream = result.getOrThrow().body!!.charStream()
            formats = api.gson.fromJson(stream)!!
            stream.close()
        }

        if (formats.streamingData == null) {
            return Result.failure(IOException(formats.playabilityStatus.status))
        }

        val streaming_data = formats.streamingData!!
        val ret: MutableList<YoutubeVideoFormat> = mutableListOf()
        var decrypter: SignatureCipherDecrypter? = null

        for (i in 0 until streaming_data.formats.size + streaming_data.adaptiveFormats.size) {
            val format = if (i < streaming_data.formats.size) streaming_data.formats[i] else streaming_data.adaptiveFormats[i - streaming_data.formats.size]
            if (filter != null && !filter(format)) {
                continue
            }

            if (format.url == null) {
                if (decrypter == null) {
                    decrypter = SignatureCipherDecrypter.fromNothing("${api.api_url}/watch?v=$id").getOrThrowHere()
                }
            }

            val error = format.loadStreamUrl(id)
            if (error != null) {
                return Result.failure(error)
            }
//        format.stream_url = format.url ?: decrypter!!.decryptSignatureCipher(format.signatureCipher!!)
//        println("${format.itag} | ${format.url != null} | ${format.stream_url}")

            ret.add(format)
        }

        return Result.success(ret)
    }

    private fun YoutubeVideoFormat.loadStreamUrl(video_id: String): Throwable? {
        if (stream_url != null) {
            return null
        }
        if (url != null) {
            stream_url = url
            return null
        }

        for (i in 0 until MAX_RETRIES) {
            val decrypter = SignatureCipherDecrypter.fromNothing("${api.api_url}/watch?v=$video_id", i == 0).getOrThrowHere()
            stream_url = decrypter.decryptSignatureCipher(signatureCipher!!)
            if (api.checkYoutubeVideoStreamUrl(stream_url!!)) {
                break
            }

            if (i + 1 == MAX_RETRIES) {
                stream_url = null
                return RuntimeException("Could not load formats for video $video_id after $MAX_RETRIES attempts")
            }
        }

        return null
    }
}
