package com.spectre7.spmp.model.mediaitem

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toArgb
import com.spectre7.spmp.api.DEFAULT_CONNECT_TIMEOUT
import com.spectre7.spmp.api.YoutubeVideoFormat
import com.spectre7.spmp.api.cast
import com.spectre7.spmp.api.getVideoFormats
import com.spectre7.spmp.model.Settings
import com.spectre7.spmp.model.mediaitem.data.SongItemData
import com.spectre7.spmp.model.mediaitem.enums.SongAudioQuality
import com.spectre7.spmp.model.mediaitem.enums.SongType
import com.spectre7.spmp.platform.crop
import com.spectre7.spmp.platform.toImageBitmap
import com.spectre7.spmp.ui.component.SongPreviewLong
import com.spectre7.spmp.ui.component.SongPreviewSquare
import okhttp3.internal.filterList
import java.net.URL

class Song private constructor (id: String): MediaItem(id) {
    override val url: String get() = "https://music.youtube.com/watch?v=$id"

    override val data = SongItemData(this)
    val song_reg_entry: SongDataRegistryEntry = registry_entry as SongDataRegistryEntry

    val like_status = SongLikeStatus(id)
    val lyrics = SongLyricsHolder(this)

    val song_type: SongType? get() = data.song_type
    val duration: Long? get() = data.duration
    val album: Playlist? get() = data.album

    var theme_colour: Color?
        get() = song_reg_entry.theme_colour?.let { Color(it) }
        set(value) {
            editRegistry {
                (it as SongDataRegistryEntry).theme_colour = value?.toArgb()
            }
        }

    override fun canGetThemeColour(): Boolean = theme_colour != null || super.canGetThemeColour()
    override fun getThemeColour(): Color? = theme_colour ?: super.getThemeColour()

    suspend fun getRelatedBrowseId(): Result<String> =
        getGeneralValue { data.related_browse_id }

    fun <T> editSongData(action: SongItemData.() -> T): T {
        val ret = editData {
            action(this as SongItemData)
        }
        return ret
    }

    suspend fun <T> editSongDataSuspend(action: suspend SongItemData.() -> T): T {
        val ret = editDataSuspend {
            action(this as SongItemData)
        }
        return ret
    }

    fun editSongDataManual(action: SongItemData.() -> Unit): SongItemData {
        action(data)
        return data
    }

    fun getFormatByQuality(quality: SongAudioQuality): Result<YoutubeVideoFormat> {
        val formats = getAudioFormats()
        if (formats.isFailure) {
            return formats.cast()
        }

        return Result.success(formats.getOrThrow().getByQuality(quality))
    }

    fun getStreamFormat(): Result<YoutubeVideoFormat> {
        val quality: SongAudioQuality = getTargetStreamQuality()
        if (stream_format?.matched_quality != quality) {
            val formats = getAudioFormats()
            if (formats.isFailure) {
                return formats.cast()
            }

            stream_format = formats.getOrThrow().getByQuality(quality)
        }

        return Result.success(stream_format!!)
    }

    override fun canLoadThumbnail(): Boolean = true

    @Composable
    override fun PreviewSquare(params: MediaItemPreviewParams) {
        SongPreviewSquare(this, params)
    }

    @Composable
    fun PreviewSquare(params: MediaItemPreviewParams, queue_index: Int?) {
        SongPreviewSquare(this, params, queue_index = queue_index)
    }

    @Composable
    override fun PreviewLong(params: MediaItemPreviewParams) {
        SongPreviewLong(this, params)
    }

    @Composable
    fun PreviewLong(params: MediaItemPreviewParams, queue_index: Int?) {
        SongPreviewLong(this, params, queue_index = queue_index)
    }

    override fun getDefaultRegistryEntry(): MediaItemDataRegistry.Entry = SongDataRegistryEntry()

    private var audio_formats: List<YoutubeVideoFormat>? = null
    private var stream_format: YoutubeVideoFormat? = null

    override fun downloadThumbnail(quality: MediaItemThumbnailProvider.Quality): Result<ImageBitmap> {
        // Iterate through getThumbUrl URL and ThumbnailQuality URLs for passed quality and each lower quality
        for (i in 0 .. quality.ordinal + 1) {

            // Some static thumbnails are cropped for some reason
            if (i == 0 && thumbnail_provider !is MediaItemThumbnailProvider.DynamicProvider) {
                continue
            }

            val url = if (i == 0) getThumbUrl(quality) ?: continue else {
                when (MediaItemThumbnailProvider.Quality.values()[quality.ordinal - i + 1]) {
                    MediaItemThumbnailProvider.Quality.LOW -> "https://img.youtube.com/vi/$id/0.jpg"
                    MediaItemThumbnailProvider.Quality.HIGH -> "https://img.youtube.com/vi/$id/maxresdefault.jpg"
                }
            }

            try {
                val connection = URL(url).openConnection()
                connection.connectTimeout = DEFAULT_CONNECT_TIMEOUT

                val stream = connection.getInputStream()
                val bytes = stream.readBytes()
                stream.close()

                val image = bytes.toImageBitmap()
                if (image.width == image.height) {
                    return Result.success(image)
                }

                // Crop image to 1:1
                val size = (image.width * (9f/16f)).toInt()
                return Result.success(image.crop((image.width - size) / 2, (image.height - size) / 2, size, size))
            }
            catch (e: Throwable) {
                if (i == quality.ordinal + 1) {
                    return Result.failure(e)
                }
            }
        }

        return Result.failure(IllegalStateException())
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

    @Synchronized
    private fun getAudioFormats(): Result<List<YoutubeVideoFormat>> {
        if (audio_formats == null) {
            val result = getVideoFormats(id) { it.audio_only }
            if (result.isFailure) {
                return result.cast()
            }

            if (result.getOrThrow().isEmpty()) {
                return Result.failure(Exception("No formats returned by getVideoFormats($id)"))
            }

            audio_formats = result.getOrThrow().sortedByDescending { it.bitrate }
        }
        return Result.success(audio_formats!!)
    }

    companion object {
        private val songs: MutableMap<String, Song> = mutableMapOf()

        @Synchronized
        fun fromId(id: String): Song {
            return songs.getOrPut(id) {
                val song = Song(id)
                song.loadFromCache()
                return@getOrPut song
            }
        }

        fun getTargetStreamQuality(): SongAudioQuality {
            return Settings.getEnum(Settings.KEY_STREAM_AUDIO_QUALITY)
        }

        fun getTargetDownloadQuality(): SongAudioQuality {
            return Settings.getEnum(Settings.KEY_DOWNLOAD_AUDIO_QUALITY)
        }
    }
}
