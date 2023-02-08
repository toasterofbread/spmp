package com.spectre7.spmp.model

import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.edit
import com.beust.klaxon.Klaxon
import com.spectre7.spmp.R
import com.spectre7.spmp.api.VideoDetails
import com.spectre7.spmp.api.YoutubeVideoFormat
import com.spectre7.spmp.api.getVideoFormats
import com.spectre7.spmp.api.getSongLyrics
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

        fun <T> set(key: String, value: T) {
            data[key]!!.value = value
            save()
        }

        fun <T> get(key: String): T? {
            return data[key]!!.value as T?
        }

        fun getJsonData(): Map<String, Any?> {
            return data.mapValues { it.value.value }
        }

        fun isDefault(): Boolean {
            for (item in getDefaultData()) {
                if (data[item.key]!!.value != item.value.value) {
                    return false
                }
            }
            return true
        }

        private fun getDefaultData(): Map<String, MutableState<Any?>> {
            return mapOf(
                "title" to mutableStateOf(null),
                "theme_colour" to mutableStateOf(null),
                "lyrics_id" to mutableStateOf(null),
                "lyrics_source" to mutableStateOf(null),
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
    fun save(prefs: SharedPreferences = Settings.prefs) {
        val song_data = mutableMapOf<String, Map<String, Any?>>()
        for (song in songs) {
            if (song.value.isDefault()) {
                continue
            }
            song_data[song.key] = song.value.getJsonData()
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

    private var stream_url: String? = null
    private var stream_url_loading: Boolean = false
    private val stream_url_load_lock = Object()

    var theme_colour: Color?
        get() {
            val value = registry.get<Int>("theme_colour")
            if (value != null) {
                return Color(value)
            }
            return null
        }
        set(value) { registry.set("theme_colour", value?.toArgb()) }

    
    fun getDisplayTitle(): String? {
        val registry_title = registry.get<String>("title")
        if (registry_title != null) {
            return registry_title
        }

        if (title == null) {
            return null
        }

        var ret = title

        for (pair in listOf("[]", "{}")) {
            while (true) {
                val a = ret.indexOf(pair[0])
                if (a < 0) {
                    break
                }

                val b = ret.indexOf(pair[1])
                if (b < 0) {
                    break
                }

                val temp = ret
                ret = temp.slice(0 until a - 1) + temp.slice(b + 1 until temp.length)
            }
        }

        for ((key, value) in mapOf("-" to "", "  " to "", artist.title to "", "MV" to "")) {
            if (key.isEmpty()) {
                continue
            }
            while (ret.contains(key)) {
                ret = ret.replace(key, value)
            }
        }

        return (ret as CharSequence).trim().trim('ㅤ').toString()
    }
    
    fun setTitleOverride(value: String) {
        registry.set("title", value)
    }

    override fun subInitWithData(data: Serialisable) {
        if (data !is SerialisableSong) {
            throw ClassCastException(data.javaClass.name)
        }

        title = data.title
        if (data.artist_id != null) {
            artist = Artist.fromId(data.artist_id).loadData() as Artist
        }
        description = data.description
    }

    data class SerialisableSong(
        id: String,
        title: String? = null,
        artist_id: String? = null,
        description: String? = null
    ): Serialisable(Type.SONG, id, title, artist_id, description) {
        override fun getMediaItem(): MediaItem {
            return Song.fromId(id)
        }
    }

    override fun toSerialisable(): Serialisable {
        return SerialisableSong(id, title, artist?.id, description)
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
                    PETITLYRICS -> getString(R.string.lyrics_source_petitlyrics)
                }

            val colour: Color
                get() = when (this) {
                    PETITLYRICS -> Color(0xFFBD0A0F)
                }

            val string_code: String
                get() = when (this) {
                    PETITLYRICS -> "ptl"
                }

            companion object {
                fun getFromString(string_code: String): Source {
                    for (source in values()) {
                        if (source.string_code == string_code) {
                            return source
                        }
                    }
                    throw NotImplementedError(string_code)
                }
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
            return songs.getOrElse(id) {
                val song = Song(id)
                songs[id] = song
                return song
            }.getOrReplacedWith() as Song
        }

        fun serialisable(id: String): Serialisable {
            return Serialisable(Type.SONG.ordinal, id)
        }
    }

    fun getLyrics(callback: (Lyrics?) -> Unit) {
        thread {
            callback(getSongLyrics(this))
        }
    }

    private fun getWantedVideoFormat(formats: List<YoutubeVideoFormat>): YoutubeVideoFormat {
        val _formats = formats.sortedByDescending { it.bitrate }
        return when (Settings.getEnum<AudioQuality>(Settings.KEY_STREAM_AUDIO_QUALITY)) {
            AudioQuality.HIGH -> _formats.firstOrNull { it.audio_only } ?: _formats.first()
            AudioQuality.MEDIUM -> {
                val audio_formats = _formats.filterList { audio_only }
                if (audio_formats.isNotEmpty()) {
                    audio_formats[audio_formats.size / 2]
                }
                else {
                    _formats[_formats.size / 2]
                }
            }
            AudioQuality.LOW -> _formats.lastOrNull { it.audio_only } ?: _formats.last()
        }
    }

    fun loadStreamUrl(): String {
        synchronized(stream_url_load_lock) {
            if (stream_url != null) {
                return stream_url!!
            }

            if (stream_url_loading) {
                stream_url_load_lock.wait()
                return stream_url!!
            }

            stream_url_loading = true
        }

//        if (streaming_formats != null) {
//            format = getWantedVideoFormat(streaming_formats!!)
//            format.loadStreamUrl(id)
//        }

        val format: YoutubeVideoFormat = getVideoFormats(id) { getWantedVideoFormat(it) }.getDataOrThrow()
        stream_url = format.stream_url!!

        synchronized(stream_url_load_lock) {
            stream_url_loading = false
            stream_url_load_lock.notifyAll()
        }

        return stream_url!!
    }

    override fun downloadThumbnail(quality: ThumbnailQuality): Bitmap {
        lateinit var image: Bitmap
        try {
            val filename = when (quality) {
                ThumbnailQuality.LOW -> "0"
                ThumbnailQuality.HIGH -> "maxresdefault"
            }
            image = BitmapFactory.decodeStream(URL(getThumbUrl(quality) ?: "https://img.youtube.com/vi/$id/$filename.jpg").openConnection().getInputStream())!!
        }
        catch (e: FileNotFoundException) {
            image = BitmapFactory.decodeStream(URL("https://img.youtube.com/vi/$id/0.jpg").openConnection().getInputStream())!!
        }

        // Crop image to 16:9
        val height = (image.width * (9f/16f)).toInt()
        return Bitmap.createBitmap(image, 0, (image.height - height) / 2, image.width, height)
    }

    @Composable
    override fun PreviewSquare(content_colour: () -> Color, playerProvider: () -> PlayerViewContext, enable_long_press_menu: Boolean, modifier: Modifier) {
        SongPreviewSquare(this, content_colour, playerProvider, enable_long_press_menu, modifier.recomposeHighlighter())
    }

    @Composable
    override fun PreviewLong(content_colour: () -> Color, playerProvider: () -> PlayerViewContext, enable_long_press_menu: Boolean, modifier: Modifier) {
        SongPreviewLong(this, content_colour, playerProvider, enable_long_press_menu, modifier.recomposeHighlighter())
    }

    override fun getAssociatedArtist(): Artist? {
        return artist
    }

    override fun _getUrl(): String {
        return "https://music.youtube.com/watch?v=$id"
    }
}
