package com.spectre7.spmp.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.beust.klaxon.Klaxon
import com.spectre7.spmp.ui.component.PlaylistPreviewLong
import com.spectre7.spmp.ui.component.PlaylistPreviewSquare
import com.spectre7.utils.getStringTODO

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

    override fun getSerialisedData(klaxon: Klaxon): List<String> {
        return super.getSerialisedData(klaxon) + listOf(klaxon.toJsonString(playlist_type?.ordinal))
    }

    override fun supplyFromSerialisedData(data: MutableList<Any?>, klaxon: Klaxon): MediaItem {
        require(data.size >= 1)
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
    return getStringTODO(when (this) {
        Playlist.PlaylistType.PLAYLIST, null -> if (plural) "playlists" else "playlist"
        Playlist.PlaylistType.ALBUM -> if (plural) "albums" else "album"
        Playlist.PlaylistType.AUDIOBOOK -> if (plural) "audiobooks" else "audiobook"
        Playlist.PlaylistType.RADIO -> if (plural) "radios" else "radio"
    })
}
