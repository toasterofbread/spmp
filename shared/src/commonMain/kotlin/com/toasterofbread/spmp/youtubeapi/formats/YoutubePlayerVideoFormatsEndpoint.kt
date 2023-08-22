package com.toasterofbread.spmp.youtubeapi.formats

import com.toasterofbread.spmp.youtubeapi.YoutubeFormatsResponse
import com.toasterofbread.spmp.youtubeapi.YoutubeVideoFormat
import com.toasterofbread.spmp.youtubeapi.buildVideoFormatsRequest
import com.toasterofbread.spmp.youtubeapi.checkYoutubeVideoStreamUrl
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.YoutubeMusicApi
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.cast
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.getOrThrowHere
import okhttp3.Request

private const val MAX_RETRIES = 5

class YoutubePlayerVideoFormatsEndpoint(override val api: YoutubeMusicApi): VideoFormatsEndpoint() {
    override fun getVideoFormats(id: String, filter: ((YoutubeVideoFormat) -> Boolean)?): Result<List<YoutubeVideoFormat>> {
        val RESPONSE_DATA_START = "ytInitialPlayerResponse = "
        val request = Request.Builder()
            .url("https://www.youtube.com/watch?v=$id")
            .header("Cookie", "CONSENT=YES+1")
            .header("User-Agent", api.getUserAgent())
            .build()

        fun getFormats(): Result<Pair<SignatureCipherDecrypter, List<YoutubeVideoFormat>>> {
            var result = api.performRequest(request)
            if (result.isFailure) {
                return result.cast()
            }

            val html = result.getOrThrowHere().body!!.string()

            val decrypter_result = SignatureCipherDecrypter.fromPlayerPage(html)
            if (!decrypter_result.isSuccess) {
                return decrypter_result.cast()
            }

            val start = html.indexOf(RESPONSE_DATA_START) + RESPONSE_DATA_START.length
            val end = html.indexOf("};", start) + 1

            var streaming_data: YoutubeFormatsResponse = api.klaxon.parse(html.substring(start, end).reader())!!
            if (!streaming_data.is_ok) {
                result = api.performRequest(buildVideoFormatsRequest(id, true))

                if (!result.isSuccess) {
                    return result.cast()
                }

                val stream = result.getOrThrow().body!!.charStream()
                streaming_data = api.klaxon.parse(stream)!!
                stream.close()

                if (!streaming_data.is_ok) {
                    return Result.failure(Exception(streaming_data.playabilityStatus.status))
                }
            }

            return Result.success(Pair(
                decrypter_result.getOrThrowHere(),
                streaming_data.streamingData!!.adaptiveFormats + streaming_data.streamingData!!.formats
            ))
        }

        val ret: MutableList<YoutubeVideoFormat> = mutableListOf()

        // For some reason the URL is occasionally invalid (GET either fails with 403 or yields the URL itself)
        // I can't tell why this occurs, but just getting the URL again always seems to produce a valid one
        for (i in 0 until MAX_RETRIES) {
            val result = getFormats()
            if (!result.isSuccess) {
                return result.cast()
            }

            val (decrypter, formats) = result.getOrThrowHere()
            var valid: Boolean = true

            for (format in formats) {
                if ((filter != null && !filter(format)) || ret.any { it.identifier == format.identifier }) {
                    continue
                }

                format.stream_url = format.url ?: decrypter.decryptSignatureCipher(format.signatureCipher!!)
                if (format.url == null && !api.checkYoutubeVideoStreamUrl(format.stream_url!!)) {
                    valid = false
                    break
                }

                ret.add(format)
            }

            if (valid) {
                return Result.success(ret)
            }
        }

        return Result.failure(RuntimeException("Could not load formats for video $id after $MAX_RETRIES attempts"))
    }
}
