package com.spectre7.spmp.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.beust.klaxon.Klaxon
import com.spectre7.spmp.ui.component.PlaylistPreviewLong
import com.spectre7.spmp.ui.component.PlaylistPreviewSquare
import com.spectre7.utils.getString

class Playlist private constructor (
    id: String
): MediaItemWithLayouts(id) {

    enum class PlaylistType {
        PLAYLIST, ALBUM, AUDIOBOOK, RADIO;

        companion object {
            fun fromTypeString(type: String): PlaylistType {
                return when (type) {
                    "MUSIC_PAGE_TYPE_PLAYLIST" -> PLAYLIST
                    "MUSIC_PAGE_TYPE_ALBUM" -> ALBUM
                    "MUSIC_PAGE_TYPE_AUDIOBOOK" -> AUDIOBOOK
                    else -> throw NotImplementedError(type)
                }
            }
        }
    }
    var playlist_type: PlaylistType? by mutableStateOf(null)
        private set

    fun supplyPlaylistType(value: PlaylistType?, certain: Boolean): Playlist {
        if (value != null && (playlist_type == null || certain)) {
            playlist_type = value
        }
        return this
    }

    var total_duration: Long? by mutableStateOf(null)
        private set

    fun supplyTotalDuration(value: Long?, certain: Boolean = false): Playlist {
        if (value != null && (total_duration == null || certain)) {
            total_duration = value
        }
        return this
    }

    var item_count: Int? by mutableStateOf(null)
        private set

    fun supplyItemCount(value: Int?, certain: Boolean = false): Playlist {
        if (value != null && (item_count == null || certain)) {
            item_count = value
        }
        return this
    }

    var year: Int? by mutableStateOf(null)
        private set

    fun supplyYear(value: Int?, certain: Boolean = false): Playlist {
        if (value != null && (year == null || certain)) {
            year = value
        }
        return this
    }

    override fun getSerialisedData(klaxon: Klaxon): List<String> {
        return super.getSerialisedData(klaxon) + listOf(klaxon.toJsonString(playlist_type?.ordinal), klaxon.toJsonString(total_duration), klaxon.toJsonString(item_count), klaxon.toJsonString(year))
    }

    override fun supplyFromSerialisedData(data: MutableList<Any?>, klaxon: Klaxon): MediaItem {
        require(data.size >= 4)
        data.removeLast()?.also { year = it as Int }
        data.removeLast()?.also { item_count = it as Int }
        data.removeLast()?.also { total_duration = (it as Int).toLong() }
        data.removeLast()?.also { playlist_type = PlaylistType.values()[it as Int] }
        return super.supplyFromSerialisedData(data, klaxon)
    }

    companion object {
        private val playlists: MutableMap<String, Playlist> = mutableMapOf()

        @Synchronized
        fun fromId(id: String): Playlist {
            return playlists.getOrPut(id) {
                val playlist = Playlist(id)
                playlist.loadFromCache()
                return@getOrPut playlist
            }.getOrReplacedWith() as Playlist
        }

        fun clearStoredItems(): Int {
            val amount = playlists.size
            playlists.clear()
            return amount
        }
    }

    @Composable
    override fun PreviewSquare(params: PreviewParams) {
        PlaylistPreviewSquare(this, params)
    }

    @Composable
    override fun PreviewLong(params: PreviewParams) {
        PlaylistPreviewLong(this, params)
    }

    override val url: String get() = "https://music.youtube.com/playlist?list=$id"
}

fun Playlist.PlaylistType?.getReadable(plural: Boolean): String {
    return getString(when (this) {
        Playlist.PlaylistType.PLAYLIST, null -> if (plural) "playlists" else "playlist"
        Playlist.PlaylistType.ALBUM -> if (plural) "albums" else "album"
        Playlist.PlaylistType.AUDIOBOOK -> if (plural) "audiobooks" else "audiobook"
        Playlist.PlaylistType.RADIO -> if (plural) "radios" else "radio"
    })
}
