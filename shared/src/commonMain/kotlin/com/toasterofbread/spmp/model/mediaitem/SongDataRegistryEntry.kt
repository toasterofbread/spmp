package com.toasterofbread.spmp.model.mediaitem

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.beust.klaxon.Json
import com.toasterofbread.spmp.api.lyrics.LyricsReference
import com.toasterofbread.utils.ValueListeners

const val SONG_STATIC_LYRICS_SYNC_OFFSET = 500

class SongDataRegistryEntry: MediaItemDataRegistry.Entry() {
    var theme_colour: Int? by mutableStateOf(null)
    var thumbnail_rounding: Int? by mutableStateOf(null)
    var np_gradient_depth: Float? by mutableStateOf(null)
    var notif_image_offset_x: Int? by mutableStateOf(null)
    var notif_image_offset_y: Int? by mutableStateOf(null)

    var lyrics_id: String? by mutableStateOf(null)
    var lyrics_source_idx: Int? by mutableStateOf(null)
    var lyrics_sync_offset: Int? by mutableStateOf(null)

    @Json(ignored = true)
    val lyrics_listeners = ValueListeners<LyricsReference?>()
    fun updateLyrics(id: String?, source_idx: Int?) {
        if (id == lyrics_id && source_idx == lyrics_source_idx) {
            return
        }

        lyrics_id = id
        lyrics_source_idx = source_idx

        lyrics_listeners.call(getLyricsReference())
    }
    fun getLyricsReference(): LyricsReference? =
        if (lyrics_id != null) LyricsReference(lyrics_source_idx!!, lyrics_id!!)
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
        lyrics_source_idx = null
        lyrics_sync_offset = null
    }
}
