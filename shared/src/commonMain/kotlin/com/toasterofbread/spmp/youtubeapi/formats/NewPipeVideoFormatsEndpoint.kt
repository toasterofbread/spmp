package com.toasterofbread.spmp.youtubeapi.formats

import com.toasterofbread.spmp.youtubeapi.YoutubeApi
import com.toasterofbread.spmp.youtubeapi.YoutubeVideoFormat
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import org.schabi.newpipe.extractor.linkhandler.LinkHandler
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeStreamLinkHandlerFactory
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.StreamExtractor
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.VideoStream
import java.io.IOException
import java.util.concurrent.TimeUnit

class NewPipeVideoFormatsEndpoint(override val api: YoutubeApi): VideoFormatsEndpoint() {

    private fun VideoStream.toYoutubeVideoFormat(): YoutubeVideoFormat {
        return YoutubeVideoFormat(itag, format!!.mimeType, bitrate, url = content)
    }

    private fun AudioStream.toYoutubeVideoFormat(): YoutubeVideoFormat {
        return YoutubeVideoFormat(itag, format!!.mimeType, bitrate, url = content)
    }

    override suspend fun getVideoFormats(id: String, filter: ((YoutubeVideoFormat) -> Boolean)?): Result<List<YoutubeVideoFormat>> {
        val link_handler: LinkHandler = YoutubeStreamLinkHandlerFactory.getInstance().fromId(id)
        val youtube_stream_extractor: StreamExtractor = NewPipe.getService(ServiceList.YouTube.serviceId).getStreamExtractor(link_handler)

        val stream_info: StreamInfo
        try {
            stream_info = StreamInfo.getInfo(youtube_stream_extractor)
        }
        catch (e: ParsingException) {
            return Result.failure(e)
        }

        val audio_streams: List<YoutubeVideoFormat> = stream_info.audioStreams
            .map { it.toYoutubeVideoFormat() }
            .filter { filter?.invoke(it) ?: true }

        val video_streams: List<YoutubeVideoFormat> = stream_info.videoStreams
            .map { it.toYoutubeVideoFormat() }
            .filter { filter?.invoke(it) ?: true }

        return Result.success(audio_streams + video_streams)
    }
}


class CustomDownloader private constructor(builder: OkHttpClient.Builder) : Downloader() {
    private val client: OkHttpClient = builder.readTimeout(30, TimeUnit.SECONDS).build()

    @Throws(IOException::class, ReCaptchaException::class)
    override fun execute(request: Request): Response {
        val httpMethod = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val requestBody = request.dataToSend()?.toRequestBody(null, 0)

        val requestBuilder = okhttp3.Request.Builder()
            .url(url)
            .method(httpMethod, requestBody)
            .addHeader("User-Agent", USER_AGENT)

        for ((headerName, headerValueList) in headers) {
            if (headerValueList.size > 1) {
                requestBuilder.removeHeader(headerName)
                for (headerValue in headerValueList) {
                    requestBuilder.addHeader(headerName, headerValue!!)
                }
            } else if (headerValueList.size == 1) {
                requestBuilder.header(headerName, headerValueList[0])
            }
        }

        client.newCall(requestBuilder.build()).execute().use { response ->
            if (response.code == 429) {
                throw ReCaptchaException("reCaptcha Challenge requested", url)
            }

            val responseBodyToReturn = response.body?.string()
            val latestUrl = response.request.url.toString()

            return Response(
                response.code, response.message, response.headers.toMultimap(),
                responseBodyToReturn, latestUrl
            )
        }
    }


    companion object {
        private lateinit var _instance: CustomDownloader

        private fun init(builder: OkHttpClient.Builder = OkHttpClient.Builder()) {
            _instance = CustomDownloader(builder)
        }

        fun getInstance(): CustomDownloader {
            if (!::_instance.isInitialized) { init() }
            return _instance
        }

        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:68.0) Gecko/20100101 Firefox/68.0"
    }

}