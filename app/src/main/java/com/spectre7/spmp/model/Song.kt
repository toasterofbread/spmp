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
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.api.DataApi
import com.spectre7.spmp.ui.component.SongPreview
import java.io.FileNotFoundException
import java.net.URL
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.jvm.isAccessible

class DataRegistry(var songs: MutableMap<String, SongEntry> = mutableMapOf()) {
    init {
        for (song in songs) {
            song.value.id = song.key
        }
    }

    data class SongEntry(val overrides: SongOverrides = SongOverrides()) {
        @Json(ignored = true) lateinit var id: String

        fun isDefault(): Boolean {
            return overrides.isDefault()
        }

        data class SongOverrides(
            var _title: String? = null,
            var _lyrics_search_title: String? = null,
            var _lyrics_search_artist: String? = null,
            var _lyrics_search_use_main_overrides: Boolean = true,
            var _theme_colour: Int? = null
        ) {

            var title: String?
                get() = getMutableState<String?>("_title").value
                set(value) = set("_title", value)

            var lyrics_search_title: String?
                get() = getMutableState<String?>("_lyrics_search_title").value
                set(value) = set("_lyrics_search_title", value)

            var lyrics_search_artist: String?
                get() = getMutableState<String?>("_lyrics_search_artist").value
                set(value) = set("_lyrics_search_artist", value)

            var lyrics_search_use_main_overrides: Boolean
                get() = getMutableState<Boolean>("_lyrics_search_use_main_overrides").value
                set(value) = set("_lyrics_search_use_main_overrides", value)

            var theme_colour: Int?
                get() = getMutableState<Int?>("_theme_colour").value
                set(value) = set("_theme_colour", value)

            private val mutable_states = mutableMapOf<String, MutableState<*>>()

            private fun <T> getMutableState(name: String): MutableState<T> {
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
                Song.song_registry!!.save(MainActivity.prefs)
            }

            fun isDefault(): Boolean {
                return this == SongOverrides()
            }
        }
    }

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
        fun load(prefs: SharedPreferences): DataRegistry {
            val data = prefs.getString("data_registry", null)
            if (data == null || data == "{}") {
                return DataRegistry()
            }
            else {
                val ret = Klaxon().parse<DataRegistry>(data)!!
                println("$ret | ${ret.songs}")
                return ret
            }
        }
    }
}

class Song private constructor (
    private val id: String
): YtItem() {

    val registry: DataRegistry.SongEntry

    // Data
    private lateinit var _title: String
    lateinit var description: String
    lateinit var artist: Artist
    lateinit var upload_date: Date
    lateinit var duration: Duration
    var stream_url: String? = null

    init {
        if (song_registry == null) {
            song_registry = DataRegistry.load(MainActivity.prefs)
        }
        registry = song_registry!!.getSongEntry(getId())
    }

    override fun initWithData(data: ServerInfoResponse, onFinished: () -> Unit) {
        _title = data.snippet!!.title
        description = data.snippet.description!!
        upload_date = Date.from(Instant.parse(data.snippet.publishedAt))
        duration = Duration.parse(data.contentDetails!!.duration)
        stream_url = data.stream_url
        artist = Artist.fromId(data.snippet.channelId!!).loadData(true) {
            if (it == null) {
                throw RuntimeException("Song artist is null (song: $id, artist: ${data.snippet.channelId})")
            }
            loaded = true
            super.initWithData(data, onFinished)
        } as Artist
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

    data class Lyrics(val source: String, val is_timed: Boolean?, val lyrics: List<List<Term>>) {
        data class Term(val subterms: List<Subterm>, val start: Float? = null, val end: Float? = null)
        data class Subterm(val text: String, val furi: String? = null) {
            var index: Int = -1
        }

        init {
            var index = 0
            for (line in lyrics) {
                for (term in line) {
                    assert(!isTimed() || (term.start != null && term.end != null))
                    for (subterm in term.subterms) {
                        subterm.index = index++
                    }
                }
            }
        }

        fun isTimed(): Boolean {
            return is_timed!!
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
        DataApi.getSongLyrics(this, callback)
    }

    fun getStreamUrl(callback: (url: String) -> Unit) {
        if (stream_url != null) {
            callback(stream_url!!)
        }
        else {
            DataApi.getStreamUrl(getId()) {
                if (it == null) {
                    throw RuntimeException(getId())
                }
                callback(it)
            }
        }
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

    override fun getId(): String {
        return id
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
