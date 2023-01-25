package com.spectre7.spmp.model

import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.edit
import com.beust.klaxon.Json
import com.beust.klaxon.Klaxon
import com.spectre7.spmp.R
import com.spectre7.spmp.api.DataApi
import com.spectre7.spmp.api.getVideoFormats
import com.spectre7.spmp.api.VideoFormat
import com.spectre7.spmp.api.getSongLyrics
import com.spectre7.spmp.ui.component.SongPreview
import com.spectre7.utils.getString
import okhttp3.internal.filterList
import java.io.FileNotFoundException
import java.net.URL
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.jvm.isAccessible

class DataRegistry private constructor(var songs: MutableMap<String, SongEntry> = mutableMapOf()) {
    init {
        for (song in songs) {
            song.value.id = song.key
        }
    }

    data class SongEntry(
        val overrides: SongOverrides = SongOverrides()
    ) {
        @Json(ignored = true) lateinit var id: String

        fun isDefault(): Boolean {
            return overrides.isDefault()
        }

        data class SongOverrides(
            var _title: String? = null,
            var _theme_colour: Int? = null,
            var _lyrics_id: String? = null
        ) {

            var title: String?
                get() = getMutableState<String?>("_title").value
                set(value) = set("_title", value)

            var theme_colour: Int?
                get() = getMutableState<Int?>("_theme_colour").value
                set(value) = set("_theme_colour", value)

            var lyrics_id: String?
                get() = getMutableState<String?>("_lyrics_id").value
                set(value) = set("_lyrics_id", value)

            private val mutable_states = mutableMapOf<String, MutableState<*>>()

            private fun <T> getMutableState(name: String): MutableState<T> {
                if (name == "_theme_colour") {
                    {}
                }
                return mutable_states.getOrPut(name, {
                    val property = SongOverrides::class.members.first { it.name == name } as KMutableProperty1<SongOverrides, T>
                    property.isAccessible = true
                    mutableStateOf(property.get(this))
                }) as MutableState<T>
            }

            private fun <T> set(name: String, value: T) {
                val property = SongOverrides::class.members.first { it.name == name } as KMutableProperty1<SongOverrides, T>
                property.isAccessible = true
                if (property.get(this) == value) {
                    return
                }

                val state = getMutableState<T>(name)
                state.value = value

                property.set(this, value)
                Song.song_registry!!.save(Settings.prefs)
            }

            fun isDefault(): Boolean {
                return this == SongOverrides()
            }
        }
    }

    @Synchronized
    fun getSongEntry(song_id: String): SongEntry {
        val ret = songs.getOrDefault(song_id, null)

        if (ret != null) {
            return ret
        }

        return SongEntry().apply {
            id = song_id
            songs[id] = this
        }
    }

    @Synchronized
    fun save(prefs: SharedPreferences) {
        prefs.edit {
            val temp = songs
            songs = mutableMapOf()

            for (song in temp) {
                if (!song.value.isDefault()) {
                    songs[song.key] = song.value
                }
            }

            putString("data_registry", Klaxon().toJsonString(this@DataRegistry))
            songs = temp
        }
    }

    companion object {
        private var singleton: DataRegistry? = null

        @Synchronized
        fun getSingleton(prefs: SharedPreferences): DataRegistry {
            if (singleton == null) {
                val data = prefs.getString("data_registry", null)
                singleton = if (data == null || data == "{}") {
                    DataRegistry()
                }
                else {
                    Klaxon().parse<DataRegistry>(data)!!
                }
            }
            return singleton!!
        }
    }
}

class Song private constructor (
    private val _id: String
): MediaItem() {

    enum class AudioQuality {
        LOW, MEDIUM, HIGH
    }

    val registry: DataRegistry.SongEntry

    private var stream_url: String? = null
    private var stream_url_loading: Boolean = false
    private val stream_url_load_lock = Object()

    // Data
    private lateinit var _title: String
    lateinit var description: String
    lateinit var artist: Artist
    lateinit var upload_date: Date
    lateinit var duration: Duration

    init {
        song_registry = DataRegistry.getSingleton(Settings.prefs)
        registry = song_registry!!.getSongEntry(id)
    }

    override fun subInitWithData(data: YTApiDataResponse) {
        _title = data.snippet!!.title
        description = data.snippet.description!!
        upload_date = Date.from(Instant.parse(data.snippet.publishedAt))
        duration = Duration.parse(data.contentDetails!!.duration)
        artist = Artist.fromId(data.snippet.channelId!!).loadData() as Artist
    }

    var theme_colour: Color?
        get() {
            val value = registry.overrides.theme_colour
            if (value != null) {
                return Color(value)
            }
            return null
        }
        set(value) { registry.overrides.theme_colour = value?.toArgb() }

    val original_title: String get() = _title
    var title: String
        get() {
            if (registry.overrides.title != null) {
                return registry.overrides.title!!
            }

            var ret = _title

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

            for ((key, value) in mapOf("-" to "", "  " to "", artist.name to "", "MV" to "")) {
                if (key.isEmpty()) {
                    continue
                }
                while (ret.contains(key)) {
                    ret = ret.replace(key, value)
                }
            }

            return (ret as CharSequence).trim().trim('ㅤ').toString()
        }
        set(value) { registry.overrides.title = value }

    data class Lyrics(
        val id: String,
        val sync: Int,
        val lyrics: List<List<Term>>
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
                    return values().toList()//.reversed()
                }
            }
        }

        data class Term(val subterms: List<Subterm>, val start: Float? = null, val end: Float? = null)
        data class Subterm(val text: String, val furi: String? = null) {
            var index: Int = -1
        }

        val sync_type: SyncType
            get() = SyncType.values()[sync]

        init {
            var index = 0
            for (line in lyrics) {
                for (term in line) {
                    assert(sync_type == SyncType.NONE || (term.start != null && term.end != null))
                    for (subterm in term.subterms) {
                        subterm.index = index++
                    }
                }
            }
        }
    }

    companion object {
        internal var song_registry: DataRegistry? = null
        private val songs: MutableMap<String, Song> = mutableMapOf()

        fun fromId(id: String): Song {
            return songs.getOrElse(id) {
                val song = Song(id)
                songs[id] = song
                return song
            }
        }
    }

    fun getLyrics(callback: (Lyrics?) -> Unit) {
        thread {
            callback(getSongLyrics(this))
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

        stream_url = getVideoFormats(id) { _formats ->
            val formats = _formats.sortedByDescending { it.averageBitrate }
            return@getVideoFormats when (Settings.getEnum<AudioQuality>(Settings.KEY_STREAM_AUDIO_QUALITY)) {
                AudioQuality.HIGH -> formats.firstOrNull { it.audio_only } ?: formats.first()
                AudioQuality.MEDIUM -> {
                    val audio_formats = formats.filterList { audio_only }
                    if (audio_formats.isNotEmpty()) {
                        audio_formats[audio_formats.size / 2]
                    }
                    formats[formats.size / 2]
                }
                AudioQuality.LOW -> formats.lastOrNull { it.audio_only } ?: formats.last()
            }
        }.getDataOrThrow().stream_url

        synchronized(stream_url_load_lock) {
            stream_url_loading = false
            stream_url_load_lock.notifyAll()
        }

        return stream_url!!
    }

    override fun loadThumbnail(hq: Boolean): Bitmap {
        if (!thumbnailLoaded(hq)) {
            var thumb: Bitmap

            if (hq) {
                try {
                    thumb = BitmapFactory.decodeStream(URL("https://img.youtube.com/vi/$id/maxresdefault.jpg").openConnection().getInputStream())!!
                }
                catch (e: FileNotFoundException) {
                    thumb = BitmapFactory.decodeStream(URL(getThumbUrl(hq)).openConnection().getInputStream())!!

                    // Crop thumbnail to 16:9
                    val height = (thumb.width * (9f/16f)).toInt()
                    thumb = Bitmap.createBitmap(thumb, 0, (thumb.height - height) / 2, thumb.width, height)
                }
            }
            else {
                thumb = BitmapFactory.decodeStream(URL(getThumbUrl(hq)).openConnection().getInputStream())!!
            }

            if (hq) {
                thumbnail_hq = thumb
            }
            else {
                thumbnail = thumb
            }
        }
        return (if (hq) thumbnail_hq else thumbnail)!!
    }

    override fun _getId(): String {
        return _id
    }

    override fun _getUrl(): String {
        return "https://music.youtube.com/watch?v=$id"
    }

    @Composable
    override fun Preview(large: Boolean, modifier: Modifier, colour: Color) {
        return SongPreview(this, large, colour, modifier)
    }

    @Composable
    fun PreviewBasic(large: Boolean, modifier: Modifier, colour: Color) {
        return SongPreview(this, large, colour, modifier, true)
    }
}
