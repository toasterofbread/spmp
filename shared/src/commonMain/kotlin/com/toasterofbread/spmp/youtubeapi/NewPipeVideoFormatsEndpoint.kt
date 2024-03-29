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

class NewPipeVideoFormatsEndpoint(override val api: YtmApi): VideoFormatsEndpoint() {
    override suspend fun getVideoFormats(
        id: String,
        filter: ((YoutubeVideoFormat) -> Boolean)?
    ): Result<List<YoutubeVideoFormat>> = runCatching {
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

private fun VideoStream.toYoutubeVideoFormat(): YoutubeVideoFormat {
    return YoutubeVideoFormat(itag, format!!.mimeType, bitrate, url = content)
}

private fun AudioStream.toYoutubeVideoFormat(): YoutubeVideoFormat {
    return YoutubeVideoFormat(itag, format!!.mimeType, bitrate, url = content)
}
