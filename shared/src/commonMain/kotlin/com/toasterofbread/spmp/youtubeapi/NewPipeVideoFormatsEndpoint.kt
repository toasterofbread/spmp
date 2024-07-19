package com.toasterofbread.spmp.youtubeapi

import dev.toastbits.ytmkt.model.YtmApi
import dev.toastbits.ytmkt.formats.VideoFormatsEndpoint
import dev.toastbits.ytmkt.model.external.YoutubeVideoFormat
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.linkhandler.LinkHandler
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeStreamLinkHandlerFactory
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.StreamExtractor
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.VideoStream
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.request.headers
import io.ktor.http.HttpMethod
import io.ktor.http.takeFrom
import io.ktor.http.HttpStatusCode
import io.ktor.util.toMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import PlatformIO

class NewPipeVideoFormatsEndpoint(override val api: YtmApi): VideoFormatsEndpoint() {
    override suspend fun getVideoFormats(
        id: String,
        include_non_default: Boolean,
        filter: ((YoutubeVideoFormat) -> Boolean)?
    ): Result<List<YoutubeVideoFormat>> = withContext(Dispatchers.PlatformIO) {
        runCatching {
            init(api)

            val link_handler: LinkHandler = YoutubeStreamLinkHandlerFactory.getInstance().fromId(id)
            val youtube_stream_extractor: StreamExtractor = NewPipe.getService(ServiceList.YouTube.serviceId).getStreamExtractor(link_handler)

            val stream_info: StreamInfo = StreamInfo.getInfo(youtube_stream_extractor)

            val audio_streams: List<YoutubeVideoFormat> = stream_info.audioStreams
                .map { it.toYoutubeVideoFormat() }
                .filter { filter?.invoke(it) ?: true }

            val video_streams: List<YoutubeVideoFormat> = stream_info.videoStreams
                .map { it.toYoutubeVideoFormat() }
                .filter { filter?.invoke(it) ?: true }

            return@runCatching audio_streams + video_streams
        }
    }

    companion object {
        private var initialised: Boolean = false

        private fun init(api: YtmApi) {
            if (initialised) {
                return
            }

            NewPipe.init(object : Downloader() {
                override fun execute(request: org.schabi.newpipe.extractor.downloader.Request): org.schabi.newpipe.extractor.downloader.Response =
                    runBlocking {
                        val response: HttpResponse =
                            api.client.request {
                                url.takeFrom(request.url())

                                method = HttpMethod.parse(request.httpMethod())

                                val body: ByteArray? = request.dataToSend()
                                if (body != null) {
                                    setBody(body)
                                }

                                for ((name, values) in request.headers()) {
                                    headers.appendAll(name, values)
                                }
                                headers.append("User-Agent", getUserAgent())
                            }

                        if (response.status == HttpStatusCode.TooManyRequests) {
                            throw ReCaptchaException("reCaptcha Challenge requested", request.url())
                        }

                        return@runBlocking org.schabi.newpipe.extractor.downloader.Response(
                            response.status.value,
                            response.status.description,
                            response.headers.toMap(),
                            response.bodyAsText(),
                            request.url()
                        )
                    }
            })

            initialised = true
        }
    }
}

private fun getUserAgent(): String =
    "Mozilla/5.0 (X11; Linux x86_64; rv:105.0) Gecko/20100101 Firefox/105.0"

private fun VideoStream.toYoutubeVideoFormat(): YoutubeVideoFormat {
    return YoutubeVideoFormat(itag, format!!.mimeType, bitrate, url = content)
}

private fun AudioStream.toYoutubeVideoFormat(): YoutubeVideoFormat {
    return YoutubeVideoFormat(itag, format!!.mimeType, bitrate, url = content)
}
