package com.spectre7.spmp.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.ui.graphics.vector.ImageVector
import com.beust.klaxon.JsonObject
import com.spectre7.spmp.api.DataApi
import com.spectre7.spmp.resources.getString

enum class MediaItemType {
    SONG, ARTIST, PLAYLIST;

    fun getIcon(): ImageVector {
        return when (this) {
            SONG     -> Icons.Filled.MusicNote
            ARTIST   -> Icons.Filled.Person
            PLAYLIST -> Icons.Filled.PlaylistPlay
        }
    }

    fun getReadable(plural: Boolean = false): String {
        return getString(
            when (this) {
                SONG -> if (plural) "songs" else "song"
                ARTIST -> if (plural) "artists" else "artist"
                PLAYLIST -> if (plural) "playlists" else "playlist"
            }
        )
    }

    fun parseRegistryEntry(obj: JsonObject): MediaItemDataRegistry.Entry {
        return when (this) {
            SONG -> DataApi.klaxon.parseFromJsonObject<Song.SongDataRegistryEntry>(obj)!!
            PLAYLIST -> DataApi.klaxon.parseFromJsonObject<PlaylistDataRegistryEntry>(obj)!!
            else -> DataApi.klaxon.parseFromJsonObject(obj)!!
        }
    }

    override fun toString(): String {
        return name.lowercase().replaceFirstChar { it.uppercase() }
    }

    companion object {
        fun fromBrowseEndpointType(page_type: String): MediaItemType? {
            return when (page_type) {
                "MUSIC_PAGE_TYPE_PLAYLIST", "MUSIC_PAGE_TYPE_ALBUM", "MUSIC_PAGE_TYPE_AUDIOBOOK" -> PLAYLIST
                "MUSIC_PAGE_TYPE_ARTIST", "MUSIC_PAGE_TYPE_USER_CHANNEL" -> ARTIST
                else -> null
            }
        }
    }
}
