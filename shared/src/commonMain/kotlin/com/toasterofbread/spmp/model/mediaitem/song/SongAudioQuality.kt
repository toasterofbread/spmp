package com.toasterofbread.spmp.model.mediaitem.song

import com.toasterofbread.spmp.model.settings.Settings
import com.toasterofbread.spmp.model.settings.category.StreamingSettings
import com.toasterofbread.spmp.platform.AppContext
import dev.toastbits.ytmkt.model.external.YoutubeVideoFormat

enum class SongAudioQuality {
    LOW, MEDIUM, HIGH
}

fun getSongTargetStreamQuality(): SongAudioQuality =
    Settings.getEnum(StreamingSettings.Key.STREAM_AUDIO_QUALITY)

fun getSongTargetDownloadQuality(): SongAudioQuality =
    Settings.getEnum(StreamingSettings.Key.DOWNLOAD_AUDIO_QUALITY)

suspend fun getSongFormatByQuality(song_id: String, quality: SongAudioQuality, context: AppContext): Result<YoutubeVideoFormat> =
    getAudioFormats(song_id, context).fold(
        { Result.success(it.getByQuality(quality)) },
        { Result.failure(it) }
    )

suspend fun getSongStreamFormat(song_id: String, context: AppContext): Result<YoutubeVideoFormat> =
    getSongFormatByQuality(song_id, getSongTargetStreamQuality(), context)

private suspend fun getAudioFormats(song_id: String, context: AppContext): Result<List<YoutubeVideoFormat>> {
    val result: Result<List<YoutubeVideoFormat>> =
        context.ytapi.VideoFormats.getVideoFormats(song_id) {
            it.mimeType.startsWith("audio/mp4")
        }

    val formats: List<YoutubeVideoFormat> = result.fold(
        { it },
        { return Result.failure(it) }
    )

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
