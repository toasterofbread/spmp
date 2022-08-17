package com.spectre7.spmp.model

import androidx.compose.runtime.Composable
import com.spectre7.spmp.api.DataApi
import com.spectre7.spmp.ui.components.SongPreview
import java.time.Duration
import java.util.Date

data class SongData (
    val locale: String?,
    val title: String,
    val desc: String
)

class Song (
    private val id: String,
    val nativeData: SongData? = null,
    val artist: Artist,
    val uploadDate: Date? = null,

    val duration: Duration? = null,
    val listenCount: Int = 0
): Previewable() {
    companion object {
        fun fromId(video_id: String): Song {
            return DataApi.getSong(video_id)!!
        }
    }

    fun getDefaultLanguage(): String? {
        return nativeData?.locale
    }

    fun getThumbUrl(): String {
        return "https://img.youtube.com/vi/$id/mqdefault.jpg"
    }

    override fun getId(): String {
        return id
    }

    @Composable
    override fun getPreview() {
        return SongPreview(this)
    }
}
