package com.toasterofbread.spmp.youtubeapi.formats

import com.toasterofbread.spmp.youtubeapi.YoutubeApi
import com.toasterofbread.spmp.youtubeapi.YoutubeVideoFormat
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeStreamLinkHandlerFactory
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.VideoStream

class NewPipeVideoFormatsEndpoint(override val api: YoutubeApi): VideoFormatsEndpoint() {
    override fun getVideoFormats(id: String, filter: ((YoutubeVideoFormat) -> Boolean)?): Result<List<YoutubeVideoFormat>> {
        val stream_info: StreamInfo

        try {
            stream_info = StreamInfo.getInfo(
                NewPipe.getService(0).getStreamExtractor(
                    YoutubeStreamLinkHandlerFactory.getInstance().fromId(id)
                )
            )
        }
        catch (e: ParsingException) {
            return Result.failure(e)
        }

        return Result.success(
            stream_info.audioStreams.mapNotNull { stream ->
                val format = stream.toYoutubeVideoFormat()
                if (filter?.invoke(format) == false) {
                    return@mapNotNull null
                }
                return@mapNotNull format
            }
                    + stream_info.videoStreams.mapNotNull { stream ->
                val format = stream.toYoutubeVideoFormat()
                if (filter?.invoke(format) == false) {
                    return@mapNotNull null
                }
                return@mapNotNull format
            }
        )
    }
}

private fun VideoStream.toYoutubeVideoFormat(): YoutubeVideoFormat {
    return YoutubeVideoFormat(itag, format!!.mimeType, bitrate, url = content)
}

private fun AudioStream.toYoutubeVideoFormat(): YoutubeVideoFormat {
    return YoutubeVideoFormat(itag, format!!.mimeType, bitrate, url = content)
}
