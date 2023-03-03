package com.spectre7.spmp.model

import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.provider.MediaStore.Audio
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.edit
import com.beust.klaxon.Klaxon
import com.spectre7.spmp.R
import com.spectre7.spmp.api.*
import com.spectre7.spmp.ui.component.SongPreviewLong
import com.spectre7.spmp.ui.component.SongPreviewSquare
import com.spectre7.spmp.ui.layout.PlayerViewContext
import com.spectre7.utils.getString
import com.spectre7.utils.recomposeHighlighter
import com.spectre7.utils.toHiragana
import okhttp3.internal.filterList
import java.io.FileNotFoundException
import java.net.URL
import kotlin.concurrent.thread

class DataRegistry constructor(data: Map<String, Map<String, Any?>>? = null) {
    private var songs: MutableMap<String, SongEntry> = mutableMapOf()

    init {
        if (data != null) {
            for (song_data in data) {
                songs[song_data.key] = SongEntry(song_data.value)
            }
        }
    }

    inner class SongEntry(init_data: Map<String, Any?>? = null) {
        val data: Map<String, MutableState<Any?>> = getDefaultData()

        init {
            if (init_data != null) {
                for (item in init_data) {
                    data[item.key]!!.value = item.value
                }
            }
        }

        fun <T> set(key: String, value: T, save: Boolean = true) {
            data[key]!!.value = value

            if (save) {
                saveData()
            }
        }

        fun <T> get(key: String): T? {
            return data[key]!!.value as T?
        }

        fun <T> getState(key: String): MutableState<T?> {
            return data[key]!! as MutableState<T?>
        }

        fun save() {
            saveData()
        }

        internal fun getJsonData(): Map<String, Any?>? {
            val ret: MutableMap<String, Any?> = mutableMapOf()
            for (item in data) {
                if (item.value.value == null) {
                    continue
                }
                ret[item.key] = item.value.value
            }
            return ret.ifEmpty { null }
        }

        private fun getDefaultData(): Map<String, MutableState<Any?>> {
            return mapOf(
                "title" to mutableStateOf(null),
                "theme_colour" to mutableStateOf(null),
                "lyrics_id" to mutableStateOf(null),
                "lyrics_source" to mutableStateOf(null),
                "thumbnail_rounding" to mutableStateOf(null)
            )
        }
    }

    @Synchronized
    fun getSongEntry(song_id: String): SongEntry {
        val ret = songs.getOrDefault(song_id, null)

        if (ret != null) {
            return ret
        }

        return SongEntry().also { entry ->
            songs[song_id] = entry
        }
    }

    @Synchronized
    fun saveData(prefs: SharedPreferences = Settings.prefs) {
        val song_data = mutableMapOf<String, Map<String, Any?>>()
        for (song in songs) {
            val data = song.value.getJsonData()
            if (data != null) {
                song_data[song.key] = data
            }
        }

        if (song_data.isEmpty()) {
            return
        }

        prefs.edit {
            putString("data_registry", Klaxon().toJsonString(song_data))
        }
    }
}

class Song private constructor (
    id: String
): MediaItem(id) {

    enum class AudioQuality {
        LOW, MEDIUM, HIGH
    }

    val registry: DataRegistry.SongEntry

    private var audio_formats: List<YoutubeVideoFormat>? = null
    private var stream_format: YoutubeVideoFormat? = null
    private var download_format: YoutubeVideoFormat? = null

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
                    PETITLYRICS -> getString(R.string.lyrics_source_petitlyrics)
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
                    NONE -> getString(R.string.lyrics_sync_none)
                    LINE_SYNC -> getString(R.string.lyrics_sync_line)
                    WORD_SYNC -> getString(R.string.lyrics_sync_word)
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

        data class Term(val subterms: List<Text>, val start: Float? = null, val end: Float? = null) {
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

            val range: ClosedFloatingPointRange<Float>
                get() = start!! .. end!!

            companion object {
                val EMPTY = Term(listOf(Text("")), -1f, -1f)
            }
        }

        init {
            for (line in lines) {
                for (term in line) {
                    assert(sync_type == SyncType.NONE || (term.start != null && term.end != null))
                }
            }
        }
    }

    init {
        registry = song_registry.getSongEntry(id)
    }

    companion object {
        private val songs: MutableMap<String, Song> = mutableMapOf()
        lateinit var song_registry: DataRegistry

        fun init(prefs: SharedPreferences) {
            val data = prefs.getString("data_registry", null)
            song_registry = DataRegistry(if (data == null) null else Klaxon().parse(data))
        }

        @Synchronized
        fun fromId(id: String): Song {
            return songs.getOrPut(id) {
                val song = Song(id)
                song.loadFromCache()
                return@getOrPut song
            }.getOrReplacedWith() as Song
        }
    }

    fun getLyrics(callback: (Lyrics?) -> Unit) {
        thread {
            callback(getSongLyrics(this))
        }
    }

    var theme_colour: Color?
        get() {
            val value = registry.get<Int>("theme_colour")
            if (value != null) {
                return Color(value)
            }
            return null
        }
        set(value) { registry.set("theme_colour", value?.toArgb()) }
    
    fun setTitleOverride(value: String) {
        registry.set("title", value)
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

    fun getTargetStreamQuality(): AudioQuality {
        return Settings.getEnum(Settings.KEY_STREAM_AUDIO_QUALITY)
    }

    fun getTargetDownloadQuality(): AudioQuality {
        return Settings.getEnum(Settings.KEY_DOWNLOAD_AUDIO_QUALITY)
    }

    override fun canLoadThumbnail(): Boolean {
        return true
    }

    override fun downloadThumbnail(quality: ThumbnailQuality): Bitmap {
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
                val image = BitmapFactory.decodeStream(URL(url).openConnection().getInputStream())!!
                if (image.width == image.height) {
                    return image
                }

                // Crop image to 1:1
                val size = (image.width * (9f/16f)).toInt()
                return Bitmap.createBitmap(image, (image.width - size) / 2, (image.height - size) / 2, size, size)
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
    override fun PreviewSquare(content_colour: () -> Color, playerProvider: () -> PlayerViewContext, enable_long_press_menu: Boolean, modifier: Modifier) {
        SongPreviewSquare(this, content_colour, playerProvider, enable_long_press_menu, modifier)
    }

    @Composable
    fun PreviewSquare(content_colour: () -> Color, playerProvider: () -> PlayerViewContext, enable_long_press_menu: Boolean, modifier: Modifier = Modifier, queue_index: Int?) {
        SongPreviewSquare(this, content_colour, playerProvider, enable_long_press_menu, modifier, queue_index = queue_index)
    }

    @Composable
    override fun PreviewLong(content_colour: () -> Color, playerProvider: () -> PlayerViewContext, enable_long_press_menu: Boolean, modifier: Modifier) {
        SongPreviewLong(this, content_colour, playerProvider, enable_long_press_menu, modifier)
    }

    @Composable
    fun PreviewLong(content_colour: () -> Color, playerProvider: () -> PlayerViewContext, enable_long_press_menu: Boolean, modifier: Modifier = Modifier, queue_index: Int?) {
        SongPreviewLong(this, content_colour, playerProvider, enable_long_press_menu, modifier, queue_index = queue_index)
    }

    override val url: String get() = "https://music.youtube.com/watch?v=$id"
}
