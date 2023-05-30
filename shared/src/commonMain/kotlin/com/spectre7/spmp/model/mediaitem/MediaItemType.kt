package com.spectre7.spmp.model.mediaitem

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.ui.graphics.vector.ImageVector
import com.beust.klaxon.JsonObject
import com.spectre7.spmp.api.DataApi
import com.spectre7.spmp.resources.getString
import com.spectre7.spmp.platform.PlatformContext

enum class MediaItemType {
    SONG, ARTIST, PLAYLIST_ACC, PLAYLIST_LOC;

    fun getIcon(): ImageVector {
        return when (this) {
            SONG     -> Icons.Filled.MusicNote
            ARTIST   -> Icons.Filled.Person
            PLAYLIST_ACC, PLAYLIST_LOC -> Icons.Filled.PlaylistPlay
        }
    }

    fun getReadable(plural: Boolean = false): String {
        return getString(
            when (this) {
                SONG -> if (plural) "songs" else "song"
                ARTIST -> if (plural) "artists" else "artist"
                PLAYLIST_ACC, PLAYLIST_LOC -> if (plural) "playlists" else "playlist"
            }
        )
    }

    fun parseRegistryEntry(obj: JsonObject): MediaItemDataRegistry.Entry {
        return when (this) {
            SONG -> DataApi.klaxon.parseFromJsonObject<Song.SongDataRegistryEntry>(obj)!!
            PLAYLIST_ACC, PLAYLIST_LOC -> DataApi.klaxon.parseFromJsonObject<PlaylistDataRegistryEntry>(obj)!!
            else -> DataApi.klaxon.parseFromJsonObject(obj)!!
        }
    }
    
    suspend fun fromId(id: String, context: PlatformContext = SpMp.context): MediaItem = when (this) {
        SONG -> Song.fromId(id)
        ARTIST -> Artist.fromId(id)
        PLAYLIST_ACC -> AccountPlaylist.fromId(id)
        PLAYLIST_LOC -> LocalPlaylist.fromId(id, context)
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
