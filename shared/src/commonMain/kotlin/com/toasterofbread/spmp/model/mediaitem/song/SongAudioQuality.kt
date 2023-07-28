package com.toasterofbread.spmp.model.mediaitem.song

import com.toasterofbread.spmp.api.YoutubeVideoFormat
import com.toasterofbread.spmp.api.getVideoFormats
import com.toasterofbread.spmp.model.Settings
import okhttp3.internal.filterList

enum class SongAudioQuality {
    LOW, MEDIUM, HIGH
}

fun getSongTargetStreamQuality(): SongAudioQuality =
    Settings.getEnum(Settings.KEY_STREAM_AUDIO_QUALITY)

fun getSongTargetDownloadQuality(): SongAudioQuality =
    Settings.getEnum(Settings.KEY_DOWNLOAD_AUDIO_QUALITY)

fun getSongFormatByQuality(song_id: String, quality: SongAudioQuality): Result<YoutubeVideoFormat> =
    getAudioFormats(song_id).fold(
        { Result.success(it.getByQuality(quality)) },
        { Result.failure(it) }
    )

fun getSongStreamFormat(song_id: String): Result<YoutubeVideoFormat> =
    getSongFormatByQuality(song_id, getSongTargetStreamQuality())

private fun getAudioFormats(song_id: String): Result<List<YoutubeVideoFormat>> {
    val result = getVideoFormats(song_id) { it.audio_only }

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
    }.also { it.matched_quality = quality }
}
