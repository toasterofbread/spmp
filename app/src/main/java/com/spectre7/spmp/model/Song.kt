package com.spectre7.spmp.model

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.spectre7.spmp.api.DataApi
import com.spectre7.spmp.ui.components.SongPreview
import com.spectre7.spmp.PlayerHost
import com.spectre7.spmp.MainActivity
import java.io.FileNotFoundException
import java.lang.RuntimeException
import java.net.URL
import java.time.Duration
import java.util.Date
import com.spectre7.ptl.Ptl
import com.spectre7.spmp.R
import com.spectre7.utils.getString

data class SongData (
    val locale: String?,
    val title: String,
    val desc: String
)

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

    interface Lyrics {
        fun getLyricsString(): String
        fun getSource(): String
    }
    class YTLyrics(private val lyrics: String, private val source: String? = null): Lyrics {
        override fun getLyricsString(): String {
            return lyrics
        }
        override fun getSource(): String {
            if (source != null) {
                return source.removePrefix("Source: ") + getString(R.string.lyrics_source_via_youtubemusic)
            }
            return source ?: getString(R.string.lyrics_source_youtube)
        }
    }
    class PTLyrics(val lyrics: Ptl.Lyrics): Lyrics {
        private val lyrics_string: String

        init {
            if (lyrics is Ptl.StaticLyrics) {
                lyrics_string = lyrics.text
            }
            else {
                val ret = StringBuilder()
                val lines = (lyrics as Ptl.TimedLyrics).lines
                for (i in 0 until lines.size) {
                    for (word in lines[i].words) {
                        ret.append(word.text)
                    }

                    if (i < lines.size - 1) {
                        ret.append("\n")
                    }
                }
                lyrics_string = ret.toString()
            }
        }

        override fun getLyricsString(): String = lyrics_string

        override fun getSource(): String {
            return getString(R.string.lyrics_source_petitlyrics)
        }

        fun getStatic(): Ptl.StaticLyrics? {
            return lyrics as Ptl.StaticLyrics?
        }

        fun getTimed(): Ptl.TimedLyrics? {
            return lyrics as Ptl.TimedLyrics?
        }
    }

    companion object {
        fun fromId(song_id: String, callback: (Song) -> Unit) {
            DataApi.getSong(song_id) {
                callback(it!!)
            }
        }

        fun batchFromId(song_ids: List<String>, callback: (Int, Song?) -> Unit) {
            DataApi.batchGetSongs(song_ids, callback)
        }
    }

    // TODO | Add config
    fun getTitle(): String {
        var ret = nativeData!!.title

//        if ("title_replacements" in api._config && ret in api._config.title_replacements) {
//            return api._config.title_replacements[ret].trim();
//        }

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

    fun getCounterpartId(): String? {
        if (counterpart_id == null) {
            counterpart_id = DataApi.getSongCounterpartId(this)
        }
        return counterpart_id
    }

    fun getYTLyricsId(): String? {
        if (yt_lyrics_id == null) {
            yt_lyrics_id = DataApi.getSongYTLyricsId(this)
        }
        return yt_lyrics_id
    }

    fun getPTLyricsId(): Int? {
        if (pt_lyrics_id == null) {
            pt_lyrics_id = DataApi.getSongPTLyricsId(this)
        }
        return pt_lyrics_id
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
