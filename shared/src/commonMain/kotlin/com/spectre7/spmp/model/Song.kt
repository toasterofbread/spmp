package com.spectre7.spmp.model

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toArgb
import com.spectre7.spmp.api.*
import com.spectre7.spmp.platform.crop
import com.spectre7.spmp.platform.toImageBitmap
import com.spectre7.spmp.ui.component.SongPreviewLong
import com.spectre7.spmp.ui.component.SongPreviewSquare
import com.spectre7.utils.getString
import com.spectre7.utils.lazyAssert
import com.spectre7.utils.toHiragana
import okhttp3.internal.filterList
import java.io.FileNotFoundException
import java.net.URL
import kotlin.concurrent.thread

class Song protected constructor (
    id: String
): MediaItem(id) {

    enum class AudioQuality {
        LOW, MEDIUM, HIGH
    }

    class SongDataRegistryEntry: DataRegistry.Entry() {
        var theme_colour: Int? by mutableStateOf(null)
        var lyrics_id: Int? by mutableStateOf(null)
        var lyrics_source: Lyrics.Source? by mutableStateOf(null)
        var thumbnail_rounding: Int? by mutableStateOf(null)
    }
    
    val song_reg_entry: SongDataRegistryEntry = registry_entry as SongDataRegistryEntry
    override fun getDefaultRegistryEntry(): DataRegistry.Entry = SongDataRegistryEntry()

    private var audio_formats: List<YoutubeVideoFormat>? = null
    private var stream_format: YoutubeVideoFormat? = null
    private var download_format: YoutubeVideoFormat? = null

    enum class SongType { SONG, VIDEO }
    var song_type: SongType? by mutableStateOf(null)
        private set

    fun supplySongType(value: SongType?, certain: Boolean = false): Song {
        if (value != null && (song_type == null || certain)) {
            song_type = value
        }
        return this
    }

    // TODO
    var duration: Long? by mutableStateOf(null)
        private set

    fun supplyDuration(value: Long?, certain: Boolean = false): Song {
        if (value != null && (duration == null || certain)) {
            duration = value
        }
        return this
    }

    data class Lyrics(
        val id: Int,
        val source: Source,
        val sync_type: SyncType,
        val lines: List<List<Term>>
    ) {

        enum class Source {
            PETITLYRICS;

            val readable: String
                get() = when (this) {
                    PETITLYRICS -> getString("lyrics_source_petitlyrics")
                }

            val colour: Color
                get() = when (this) {
                    PETITLYRICS -> Color(0xFFBD0A0F)
                }
        }

        enum class SyncType {
            NONE,
            LINE_SYNC,
            WORD_SYNC;

            val readable: String
                get() = when (this) {
                    NONE -> getString("lyrics_sync_none")
                    LINE_SYNC -> getString("lyrics_sync_line")
                    WORD_SYNC -> getString("lyrics_sync_word")
                }

            companion object {
                fun fromKey(key: String): SyncType {
                    return when (key) {
                        "text" -> NONE
                        "line_sync" -> LINE_SYNC
                        "text_sync" -> WORD_SYNC
                        else -> throw NotImplementedError(key)
                    }
                }

                fun byPriority(): List<SyncType> {
                    return values().toList().reversed()
                }
            }
        }

        data class Term(val subterms: List<Text>, val start: Long? = null, val end: Long? = null) {
            var line_range: LongRange? = null
            var data: Any? = null

            data class Text(val text: String, var furi: String? = null) {
                init {
                    if (furi != null) {
                        if (furi == "*") {
                            this.furi = null
                        }
                        else {
                            furi = furi!!.toHiragana()
                            if (furi == text.toHiragana()) {
                                furi = null
                            }
                        }
                    }
                }
            }

            val range: LongRange
                get() = start!! .. end!!

            companion object {
                val EMPTY = Term(listOf(Text("")), null, null)
            }
        }

        init {
            lazyAssert {
                for (line in lines) {
                    for (term in line) {
                        if (sync_type != SyncType.NONE && (term.start == null || term.end == null)) {
                            return@lazyAssert false
                        }
                    }
                }
                return@lazyAssert true
            }
        }

        fun getLine(time_ms: Long): Int? {
            check(sync_type != SyncType.NONE)

            for (line in lines.withIndex()) {
                for (term in line.value) {
                    if (term.line_range!!.contains(time_ms)) {
                        return line.index
                    }
                }
            }

            return null
        }
    }

    companion object {
        private val songs: MutableMap<String, Song> = mutableMapOf()

        @Synchronized
        fun fromId(id: String): Song {
            if (id.contains(',')) {
                TODO()
            }
            return songs.getOrPut(id) {
                val song = Song(id)
                song.loadFromCache()
                return@getOrPut song
            }.getOrReplacedWith() as Song
        }

        fun getTargetStreamQuality(): AudioQuality {
            return Settings.getEnum(Settings.KEY_STREAM_AUDIO_QUALITY)
        }

        fun getTargetDownloadQuality(): AudioQuality {
            return Settings.getEnum(Settings.KEY_DOWNLOAD_AUDIO_QUALITY)
        }
    }

    var lyrics: Lyrics? by mutableStateOf(null)
        private set
    var lyrics_loaded: Boolean by mutableStateOf(false)
        private set

    @Synchronized
    fun loadLyrics(): Lyrics? {
        if (lyrics_loaded) {
            return lyrics
        }

        lyrics = getSongLyrics(this)
        lyrics_loaded = true
        return lyrics
    }

    var theme_colour: Color?
        get() = song_reg_entry.theme_colour?.let { Color(it) }
        set(value) {
            editRegistry {
                (it as SongDataRegistryEntry).theme_colour = value?.toArgb()
            }
        }

    // Expects formats to be sorted by bitrate (descending)
    private fun List<YoutubeVideoFormat>.getByQuality(quality: AudioQuality): YoutubeVideoFormat {
        check(isNotEmpty())
        return when (quality) {
            AudioQuality.HIGH -> firstOrNull { it.audio_only } ?: first()
            AudioQuality.MEDIUM -> {
                val audio_formats = filterList { audio_only }
                if (audio_formats.isNotEmpty()) {
                    audio_formats[audio_formats.size / 2]
                }
                else {
                    get(size / 2)
                }
            }
            AudioQuality.LOW -> lastOrNull { it.audio_only } ?: last()
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

    fun getFormatByQuality(quality: AudioQuality): Result<YoutubeVideoFormat> {
        val formats = getAudioFormats()
        if (formats.isFailure) {
            return formats.cast()
        }

        return Result.success(formats.getOrThrow().getByQuality(quality))
    }

    fun getStreamFormat(): Result<YoutubeVideoFormat> {
        val quality: AudioQuality = getTargetStreamQuality()
        if (stream_format?.matched_quality != quality) {
            val formats = getAudioFormats()
            if (formats.isFailure) {
                return formats.cast()
            }

            stream_format = formats.getOrThrow().getByQuality(quality)
        }

        return Result.success(stream_format!!)
    }

    fun getDownloadFormat(): Result<YoutubeVideoFormat> {
        val quality: AudioQuality = getTargetDownloadQuality()
        if (download_format?.matched_quality != quality) {
            val formats = getAudioFormats()
            if (formats.isFailure) {
                return formats.cast()
            }

            download_format = formats.getOrThrow().getByQuality(quality)
        }

        return Result.success(download_format!!)
    }

    override fun canLoadThumbnail(): Boolean {
        return true
    }

    override fun downloadThumbnail(quality: ThumbnailQuality): ImageBitmap? {
        // Iterate through getThumbUrl URL and ThumbnailQuality URLs for passed quality and each lower quality
        for (i in 0 .. quality.ordinal + 1) {

            // Some static thumbnails are cropped for some reason
            if (i == 0 && thumbnail_provider !is ThumbnailProvider.DynamicProvider) {
                continue
            }

            val url = if (i == 0) getThumbUrl(quality) ?: continue else {
                when (ThumbnailQuality.values()[quality.ordinal - i + 1]) {
                    ThumbnailQuality.LOW -> "https://img.youtube.com/vi/$id/0.jpg"
                    ThumbnailQuality.HIGH -> "https://img.youtube.com/vi/$id/maxresdefault.jpg"
                }
            }

            try {
                val stream = URL(url).openConnection().getInputStream()
                val bytes = stream.readBytes()
                stream.close()

                val image = bytes.toImageBitmap()
                if (image.width == image.height) {
                    return image
                }

                // Crop image to 1:1
                val size = (image.width * (9f/16f)).toInt()
                return image.crop((image.width - size) / 2, (image.height - size) / 2, size, size)
            }
            catch (e: FileNotFoundException) {
                if (i == quality.ordinal + 1) {
                    throw e
                }
            }
        }

        throw IllegalStateException()
    }

    @Composable
    override fun PreviewSquare(params: PreviewParams) {
        SongPreviewSquare(this, params)
    }

    @Composable
    fun PreviewSquare(params: PreviewParams, queue_index: Int?) {
        SongPreviewSquare(this, params, queue_index = queue_index)
    }

    @Composable
    override fun PreviewLong(params: PreviewParams) {
        SongPreviewLong(this, params)
    }

    @Composable
    fun PreviewLong(params: PreviewParams, queue_index: Int?) {
        SongPreviewLong(this, params, queue_index = queue_index)
    }

    override val url: String get() = "https://music.youtube.com/watch?v=$id"
}
