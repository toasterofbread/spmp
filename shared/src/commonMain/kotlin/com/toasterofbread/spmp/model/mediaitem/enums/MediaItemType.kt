package com.toasterofbread.spmp.model.mediaitem.enums

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.ui.graphics.vector.ImageVector
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistRef
import com.toasterofbread.spmp.model.mediaitem.playlist.LocalPlaylistRef
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylistRef
import com.toasterofbread.spmp.model.mediaitem.song.SongRef
import com.toasterofbread.spmp.resources.getString

enum class MediaItemType {
    SONG, ARTIST, PLAYLIST_REM, PLAYLIST_LOC;

    fun isPlaylist(): Boolean = when (this) {
        PLAYLIST_REM, PLAYLIST_LOC -> true
        else -> false
    }

    fun getIcon(): ImageVector {
        return when (this) {
            SONG     -> Icons.Filled.MusicNote
            ARTIST   -> Icons.Filled.Person
            PLAYLIST_REM, PLAYLIST_LOC -> Icons.Filled.PlaylistPlay
        }
    }

    fun getReadable(plural: Boolean = false): String {
        return getString(
            when (this) {
                SONG -> if (plural) "songs" else "song"
                ARTIST -> if (plural) "artists" else "artist"
                PLAYLIST_REM, PLAYLIST_LOC -> if (plural) "playlists" else "playlist"
            }
        )
    }

    fun referenceFromId(id: String): MediaItem = when (this) {
        SONG -> SongRef(id)
        ARTIST -> ArtistRef(id)
        PLAYLIST_REM -> RemotePlaylistRef(id)
        PLAYLIST_LOC -> LocalPlaylistRef(id)
    }

    override fun toString(): String {
        return name.lowercase().replaceFirstChar { it.uppercase() }
    }

    companion object {
        fun fromBrowseEndpointType(page_type: String): MediaItemType {
            // Remove "MUSIC_PAGE_TYPE_" prefix
            val type_name: String = page_type.substring(16)

            if (type_name.startsWith("ARTIST")) {
                return ARTIST
            }
            if (type_name.startsWith("PODCAST")) {
                return PLAYLIST_REM
            }

            return when (type_name) {
                "PLAYLIST",
                "ALBUM",
                "AUDIOBOOK",
                "RADIO" ->
                    PLAYLIST_REM
                "USER_CHANNEL", "LIBRARY_ARTIST" ->
                    ARTIST
                "NON_MUSIC_AUDIO_TRACK_PAGE", "UNKNOWN" ->
                    SONG
                else -> throw NotImplementedError(page_type)
            }
        }
    }
}
