package com.toasterofbread.spmp.model.mediaitem.enums

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.ui.graphics.vector.ImageVector
import com.beust.klaxon.JsonObject
import com.toasterofbread.spmp.api.Api
import com.toasterofbread.spmp.model.mediaitem.ArtistData
import com.toasterofbread.spmp.model.mediaitem.MediaItemData
import com.toasterofbread.spmp.model.mediaitem.MediaItemDataRegistry
import com.toasterofbread.spmp.model.mediaitem.PlaylistData
import com.toasterofbread.spmp.model.mediaitem.PlaylistDataRegistryEntry
import com.toasterofbread.spmp.model.mediaitem.SongData
import com.toasterofbread.spmp.model.mediaitem.SongDataRegistryEntry
import com.toasterofbread.spmp.resources.getString

enum class MediaItemType {
    SONG, ARTIST, PLAYLIST_ACC, PLAYLIST_LOC, PLAYLIST_BROWSEPARAMS;

    fun isPlaylist(): Boolean = when (this) {
        PLAYLIST_ACC, PLAYLIST_LOC, PLAYLIST_BROWSEPARAMS -> true
        else -> false
    }

    fun getIcon(): ImageVector {
        return when (this) {
            SONG     -> Icons.Filled.MusicNote
            ARTIST   -> Icons.Filled.Person
            PLAYLIST_ACC, PLAYLIST_LOC, PLAYLIST_BROWSEPARAMS -> Icons.Filled.PlaylistPlay
        }
    }

    fun getReadable(plural: Boolean = false): String {
        return getString(
            when (this) {
                SONG -> if (plural) "songs" else "song"
                ARTIST -> if (plural) "artists" else "artist"
                PLAYLIST_ACC, PLAYLIST_LOC, PLAYLIST_BROWSEPARAMS -> if (plural) "playlists" else "playlist"
            }
        )
    }

    fun parseRegistryEntry(obj: JsonObject): MediaItemDataRegistry.Entry {
        if (isPlaylist()) {
            return Api.klaxon.parseFromJsonObject<PlaylistDataRegistryEntry>(obj)!!
        }
        else if (this == SONG) {
            return Api.klaxon.parseFromJsonObject<SongDataRegistryEntry>(obj)!!
        }
        else {
            return Api.klaxon.parseFromJsonObject(obj)!!
        }
    }

    fun emptyDataFromId(id: String): MediaItemData = when (this) {
        SONG -> SongData(id)
        ARTIST -> ArtistData(id)
        PLAYLIST_ACC -> PlaylistData(id)
        PLAYLIST_LOC -> PlaylistData(id, playlist_type = PlaylistType.LOCAL)
        PLAYLIST_BROWSEPARAMS -> throw NotImplementedError(id)
    }

    override fun toString(): String {
        return name.lowercase().replaceFirstChar { it.uppercase() }
    }

    companion object {
        fun fromBrowseEndpointType(page_type: String): MediaItemType? {
            return when (page_type) {
                "MUSIC_PAGE_TYPE_PLAYLIST", "MUSIC_PAGE_TYPE_ALBUM", "MUSIC_PAGE_TYPE_AUDIOBOOK" -> PLAYLIST_ACC
                "MUSIC_PAGE_TYPE_ARTIST", "MUSIC_PAGE_TYPE_USER_CHANNEL" -> ARTIST
                else -> null
            }
        }
    }
}
