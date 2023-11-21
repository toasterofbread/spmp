package com.toasterofbread.spmp.model.mediaitem.song

import com.toasterofbread.spmp.model.settings.Settings
import com.toasterofbread.spmp.model.settings.category.StreamingSettings
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.youtubeapi.YoutubeVideoFormat
import okhttp3.internal.filterList

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
    val result = context.ytapi.VideoFormats.getVideoFormats(song_id) { it.audio_only }

    val formats = result.fold(
        { it },
        { return Result.failure(it) }
    )

    if (formats.isEmpty()) {
        return Result.failure(RuntimeException("No formats returned by getVideoFormats($song_id)"))
    }

    return Result.success(formats.sortedByDescending { it.bitrate })
}

// Expects formats to be sorted by bitrate (descending)
private fun List<YoutubeVideoFormat>.getByQuality(quality: SongAudioQuality): YoutubeVideoFormat {
    check(isNotEmpty())
    return when (quality) {
        SongAudioQuality.HIGH -> firstOrNull { it.audio_only } ?: first()
        SongAudioQuality.MEDIUM -> {
            val audio_formats = filterList { audio_only }
            if (audio_formats.isNotEmpty()) {
                audio_formats[audio_formats.size / 2]
            }
            else {
                get(size / 2)
            }
        }
        SongAudioQuality.LOW -> lastOrNull { it.audio_only } ?: last()
    }
}
