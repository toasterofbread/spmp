package com.spectre7.spmp.model.mediaitem.enums

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import com.beust.klaxon.JsonObject
import com.spectre7.spmp.api.Api
import com.spectre7.spmp.model.mediaitem.*
import com.spectre7.spmp.resources.getString
import com.spectre7.spmp.platform.PlatformContext
import com.spectre7.spmp.ui.component.*

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

    suspend fun fromId(id: String, context: PlatformContext = SpMp.context): MediaItem = when (this) {
        SONG -> Song.fromId(id)
        ARTIST -> Artist.fromId(id)
        PLAYLIST_ACC -> AccountPlaylist.fromId(id)
        PLAYLIST_LOC -> LocalPlaylist.fromId(id, context)
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
