package com.toasterofbread.spmp.model.mediaitem.song

import com.toasterofbread.spmp.platform.AppContext
import dev.toastbits.ytmkt.formats.VideoFormatsEndpoint
import dev.toastbits.ytmkt.model.external.YoutubeVideoFormat

enum class SongAudioQuality {
    LOW, MEDIUM, HIGH
}

suspend fun AppContext.getSongTargetStreamQuality(): SongAudioQuality =
    settings.Streaming.STREAM_AUDIO_QUALITY.get()

suspend fun AppContext.getSongTargetDownloadQuality(): SongAudioQuality =
    settings.Streaming.DOWNLOAD_AUDIO_QUALITY.get()

suspend fun getSongAudioFormatByQuality(
    song_id: String,
    quality: SongAudioQuality,
    context: AppContext,
    endpoint: VideoFormatsEndpoint? = null
): Result<YoutubeVideoFormat> =
    getSongFormats(
        song_id,
        context,
        endpoint,
        filter = { format ->
            format.mimeType.startsWith("audio/mp4")
        }
    ).fold(
        { Result.success(it.getByQuality(quality)) },
        { Result.failure(it) }
    )

suspend fun getSongTargetAudioFormat(song_id: String, context: AppContext, endpoint: VideoFormatsEndpoint? = null): Result<YoutubeVideoFormat> =
    getSongAudioFormatByQuality(song_id, context.getSongTargetStreamQuality(), context, endpoint)

suspend fun getSongFormats(
    song_id: String,
    context: AppContext,
    endpoint: VideoFormatsEndpoint? = null,
    filter: (YoutubeVideoFormat) -> Boolean = { true }
): Result<List<YoutubeVideoFormat>> {
    val result: Result<List<YoutubeVideoFormat>> =
        (endpoint ?: context.ytapi.VideoFormats).getVideoFormats(song_id, filter = filter)

    val formats: List<YoutubeVideoFormat> =
        result.fold(
            { it },
            { return Result.failure(it) }
        ).filter { it.url != null }

    if (formats.isEmpty()) {
        return Result.failure(RuntimeException("No valid formats returned by getVideoFormats($song_id)"))
    }

    return Result.success(formats.sortedByDescending { it.bitrate })
}

// Expects formats to be sorted by bitrate (descending)
private fun List<YoutubeVideoFormat>.getByQuality(quality: SongAudioQuality): YoutubeVideoFormat {
    check(isNotEmpty())
    return when (quality) {
        SongAudioQuality.HIGH -> firstOrNull { it.isAudioOnly() } ?: first()
        SongAudioQuality.MEDIUM -> {
            val audio_formats = filter { it.isAudioOnly() }
            if (audio_formats.isNotEmpty()) {
                audio_formats[audio_formats.size / 2]
            }
            else {
                get(size / 2)
            }
        }
        SongAudioQuality.LOW -> lastOrNull { it.isAudioOnly() } ?: last()
    }
}
