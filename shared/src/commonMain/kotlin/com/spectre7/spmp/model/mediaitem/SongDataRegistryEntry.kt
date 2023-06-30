package com.spectre7.spmp.model.mediaitem

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.beust.klaxon.Json
import com.spectre7.spmp.model.SongLyrics
import com.spectre7.utils.ValueListeners

const val SONG_STATIC_LYRICS_SYNC_OFFSET = 500

class SongDataRegistryEntry: MediaItemDataRegistry.Entry() {
    var theme_colour: Int? by mutableStateOf(null)
    var thumbnail_rounding: Int? by mutableStateOf(null)
    var np_gradient_depth: Float? by mutableStateOf(null)
    var notif_image_offset_x: Int? by mutableStateOf(null)
    var notif_image_offset_y: Int? by mutableStateOf(null)

    var lyrics_id: Int? by mutableStateOf(null)
    var lyrics_source: SongLyrics.Source? by mutableStateOf(null)
    var lyrics_sync_offset: Int? by mutableStateOf(null)

    @Json(ignored = true)
    val lyrics_listeners = ValueListeners<Pair<Int, SongLyrics.Source>?>()
    fun updateLyrics(id: Int?, source: SongLyrics.Source?) {
        if (id == lyrics_id && source == lyrics_source) {
            return
        }

        lyrics_id = id
        lyrics_source = source

        lyrics_listeners.call(getLyricsData())
    }
    fun getLyricsData(): Pair<Int, SongLyrics.Source>? =
        if (lyrics_id != null) Pair(lyrics_id!!, lyrics_source!!)
        else null

    fun getLyricsSyncOffset(): Int = (lyrics_sync_offset ?: 0) + SONG_STATIC_LYRICS_SYNC_OFFSET

    override fun clear() {
        super.clear()
        theme_colour = null
        thumbnail_rounding = null
        np_gradient_depth = null
        notif_image_offset_x = null
        notif_image_offset_y = null
        lyrics_id = null
        lyrics_source = null
        lyrics_sync_offset = null
    }
}
