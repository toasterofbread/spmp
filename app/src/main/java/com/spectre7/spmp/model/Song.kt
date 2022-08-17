package com.spectre7.spmp.model

import kotlin.concurrent.thread
import com.spectre7.spmp.api.DataApi
import java.time.Duration
import java.util.Date

data class SongData (
    val locale: String?,
    val title: String,
    val desc: String
)

class Song (
    val id: String,
    val nativeData: SongData,
    val artist: Artist,
    val uploadDate: Date,

    val duration: Duration? = null,
    val listenCount: Int = 0
) {
    companion object {
        fun fromId(video_id: String): Song {
            return DataApi.getSong(video_id)!!
        }
    }

    fun getDefaultLanguage(): String? {
        return nativeData.locale
    }

    fun getThumbUrl(): String {
        return "https://img.youtube.com/vi/$id/mqdefault.jpg"
    }
}
