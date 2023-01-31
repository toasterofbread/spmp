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
import com.spectre7.spmp.api.VideoData
import com.spectre7.spmp.api.YoutubeVideoFormat
import com.spectre7.spmp.api.getVideoFormats
import com.spectre7.spmp.api.getSongLyrics
import com.spectre7.spmp.ui.component.SongPreviewLong
import com.spectre7.spmp.ui.component.SongPreviewSquare
import com.spectre7.utils.getString
import com.spectre7.utils.toHiragana
import okhttp3.internal.filterList
import java.io.FileNotFoundException
import java.net.URL
import java.time.Duration
import kotlin.concurrent.thread
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.jvm.isAccessible

class DataRegistry constructor(var songs: MutableMap<String, SongEntry> = mutableMapOf()) {
    init {
        for (song in songs) {
            song.value.id = song.key
            song.value.registry = this
        }
    }

    data class SongEntry(
        var _title: String? = null,
        var _theme_colour: Int? = null,
        var _lyrics_id: Int? = null,
        var _lyrics_source: Int? = null
    ) {
        @Json(ignored = true) lateinit var id: String
        @Json(ignored = true) lateinit var registry: DataRegistry

        fun isDefault(): Boolean {
            return this == SongEntry()
        }

        var title: String?
            get() = getMutableState<String?>("_title").value
            set(value) = set("_title", value)

        var theme_colour: Int?
            get() = getMutableState<Int?>("_theme_colour").value
            set(value) = set("_theme_colour", value)

        var lyrics_id: Int?
            get() = getMutableState<Int?>("_lyrics_id").value
            set(value) = set("_lyrics_id", value)

        var lyrics_source: Song.Lyrics.Source?
            get() {
                val value = getMutableState<Int?>("_lyrics_source").value ?: return null
                return Song.Lyrics.Source.values()[value]
            }
            set(value) = set("_lyrics_source", value?.ordinal)

        private val mutable_states = mutableMapOf<String, MutableState<*>>()

        private fun <T> getMutableState(name: String): MutableState<T> {
            return mutable_states.getOrPut(name, {
                val property = SongEntry::class.members.first { it.name == name } as KMutableProperty1<SongEntry, T>
                property.isAccessible = true
                mutableStateOf(property.get(this))
            }) as MutableState<T>
        }

        private fun <T> set(name: String, value: T) {
            val property = SongEntry::class.members.first { it.name == name } as KMutableProperty1<SongEntry, T>
            property.isAccessible = true
            if (property.get(this) == value) {
                return
            }

            val state = getMutableState<T>(name)
            state.value = value

            property.set(this, value)
            registry.save(Settings.prefs)
        }
    }

    @Synchronized
    fun getSongEntry(song_id: String): SongEntry {
        val ret = songs.getOrDefault(song_id, null)

        if (ret != null) {
            return ret
        }

        return SongEntry().also { entry ->
            entry.id = song_id
            entry.registry = this
            songs[song_id] = entry
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

    private lateinit var _title: String
    lateinit var artist: Artist
    lateinit var duration: Duration

    override fun subInitWithData(data: Any) {
        if (data !is VideoData) {
            throw ClassCastException(data.javaClass.name)
        }

        _title = data.videoDetails.title
        artist = Artist.fromId(data.videoDetails.channelId).loadData() as Artist
        duration = Duration.ofSeconds(data.videoDetails.lengthSeconds.toLong())
    }

    var theme_colour: Color?
        get() {
            val value = registry.theme_colour
            if (value != null) {
                return Color(value)
            }
            return null
        }
        set(value) { registry.theme_colour = value?.toArgb() }

    val original_title: String get() = _title
    var title: String
        get() {
            if (registry.title != null) {
                return registry.title!!
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
        set(value) { registry.title = value }

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
            song_registry = if (data == null) {
                DataRegistry()
            }
            else {
                Klaxon().parse<DataRegistry>(data)!!
            }
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
        when (quality) {
            ThumbnailQuality.HIGH -> {
                try {
                    return BitmapFactory.decodeStream(URL("https://img.youtube.com/vi/$id/maxresdefault.jpg").openConnection().getInputStream())!!
                }
                catch (e: FileNotFoundException) {
                    val image = BitmapFactory.decodeStream(URL(getThumbUrl(quality)).openConnection().getInputStream())!!

                    // Crop image to 16:9
                    val height = (image.width * (9f/16f)).toInt()
                    return Bitmap.createBitmap(image, 0, (image.height - height) / 2, image.width, height)
                }
            }
            ThumbnailQuality.LOW -> {
                return BitmapFactory.decodeStream(URL(getThumbUrl(quality)).openConnection().getInputStream())!!
            }
        }
    }

    @Composable
    override fun PreviewSquare(content_colour: Color, onClick: (() -> Unit)?, onLongClick: (() -> Unit)?, modifier: Modifier) {
        SongPreviewSquare(this, content_colour, modifier, onClick, onLongClick)
    }

    @Composable
    override fun PreviewLong(content_colour: Color, onClick: (() -> Unit)?, onLongClick: (() -> Unit)?, modifier: Modifier) {
        SongPreviewLong(this, content_colour, modifier, onClick, onLongClick)
    }

    override fun _getUrl(): String {
        return "https://music.youtube.com/watch?v=$id"
    }
}
