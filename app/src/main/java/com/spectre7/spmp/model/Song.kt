package com.spectre7.spmp.model

import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
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
import java.util.*

data class SongData (
    val locale: String?,
    val title: String,
    val desc: String
)

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
    }

    data class SongOverrides(
        var title: String? = null,
        var theme_colour: Int? = null
    ) {
        fun isDefault(): Boolean {
            return title == null && theme_colour == null
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

data class Song (
    private val id: String,
    private val nativeData: SongData? = null,
    val artist: Artist,
    val uploadDate: Date? = null,

    val duration: Duration? = null,
    val listenCount: Int = 0
): Previewable() {

    private var counterpart_id: String? = null
    private var yt_lyrics_id: String? = null
    private var pt_lyrics_id: Int? = null
    private val registry_entry: DataRegistry.SongEntry

    init {
        if (registry == null) {
            registry = DataRegistry.load(MainActivity.prefs)
        }

        registry_entry = registry!!.getSongEntry(getId())
    }

    var theme_colour: Color?
        get() = if (registry_entry.overrides.theme_colour == null) null else Color(registry_entry.overrides.theme_colour!!)
        set(value) { registry_entry.overrides.theme_colour = value?.toArgb(); println("SET | ${value?.toArgb()}"); registry!!.save(MainActivity.prefs) }

    var title: String
        get() {
            if (registry_entry.overrides.title != null) {
                return registry_entry.overrides.title!!
            }

            var ret = nativeData!!.title

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

            for ((key, value) in mapOf("-" to "", "  " to "", artist.nativeData.name to "", "MV" to "")) {
                if (key.isEmpty()) {
                    continue
                }
                while (ret.contains(key)) {
                    ret = ret.replace(key, value)
                }
            }

            return ret.trim()
        }
        set(value) { registry_entry.overrides.title = value; registry!!.save(MainActivity.prefs) }

    data class Lyrics(val source: String, val timed: Boolean, val lyrics: List<List<Term>>) {
        data class Term(val subterms: List<Subterm>, val start: Float, val end: Float)
        data class Subterm(val text: String, val furi: String?) {
            var index: Int = -1
        }

        init {
            var index = 0
            for (line in lyrics) {
                for (term in line) {
                    for (subterm in term.subterms) {
                        subterm.index = index++
                    }
                }
            }
        }
    }

    companion object {
        private var registry: DataRegistry? = null

        fun fromId(song_id: String, callback: (Song) -> Unit) {
            DataApi.getSong(song_id) {
                if (it == null) {
                    throw RuntimeException(song_id)
                }
                callback(it)
            }
        }

        fun batchFromId(song_ids: List<String>, callback: (Int, Song?) -> Unit) {
            DataApi.batchGetSongs(song_ids, callback)
        }
    }

    fun getCounterpartId(): String? {
        if (counterpart_id == null) {
            counterpart_id = DataApi.getSongCounterpartId(this)
        }
        return counterpart_id
    }

    fun getLyrics(callback: (Lyrics?) -> Unit) {
        DataApi.getSongLyrics(this, callback)
    }

    fun getDownloadUrl(callback: (url: String) -> Unit) {
        DataApi.getDownloadUrl(getId()) {
            if (it == null) {
                throw RuntimeException(getId())
            }
            callback(it)
        }
    }

    override fun getThumbUrl(hq: Boolean): String {
        return "https://img.youtube.com/vi/$id/${if (hq) "hqdefault" else "mqdefault"}.jpg"
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
