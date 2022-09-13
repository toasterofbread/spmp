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

    private var counterpartId: String? = null
    private var lyricsId: String? = null

    data class Lyrics(val lyrics: String, val source: String? = null)

    companion object {
        fun fromId(video_id: String): Song {
            return DataApi.getSong(video_id)!!
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
                val a = ret.indexOf(pair[0]);
                if (a < 0) {
                    break;
                }

                val b = ret.indexOf(pair[1]);
                if (b < 0) {
                    break;
                }

                val temp = ret;
                ret = temp.slice(0 until a - 1) + temp.slice(b + 1 until temp.length);
            }
        }

        for ((key, value) in mapOf("-" to "", "  " to "", artist.nativeData.name to "", "MV" to "")) {
            if (key.isEmpty()) {
                continue
            }
            while (ret.contains(key)) {
                ret = ret.replace(key, value);
            }
        }

        return ret.trim()
    }

    fun getCounterpartId(): String? {
        if (counterpartId == null) {
            counterpartId = MainActivity.youtube.getSongCounterpartId(this)
        }
        return counterpartId
    }

    fun getLyricsId(): String? {
        if (lyricsId == null) {
            lyricsId = MainActivity.youtube.getSongLyricsId(this)
        }
        return lyricsId
    }

    fun getLyrics(callback: (Lyrics?) -> Unit) {
        MainActivity.youtube.getSongLyrics(this, callback)
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
