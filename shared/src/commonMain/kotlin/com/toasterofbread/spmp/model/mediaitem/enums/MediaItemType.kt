package com.toasterofbread.spmp.model.mediaitem.enums

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistRef
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistData
import com.toasterofbread.spmp.model.mediaitem.playlist.LocalPlaylistRef
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylistRef
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylistData
import com.toasterofbread.spmp.model.mediaitem.playlist.LocalPlaylistData
import com.toasterofbread.spmp.model.mediaitem.song.SongRef
import com.toasterofbread.spmp.model.mediaitem.song.SongData
import com.toasterofbread.spmp.model.mediaitem.MediaItemData
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.artist
import spmp.shared.generated.resources.artists
import spmp.shared.generated.resources.playlist
import spmp.shared.generated.resources.playlists
import spmp.shared.generated.resources.song
import spmp.shared.generated.resources.songs

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

    fun getReadable(plural: Boolean = false): StringResource =
        when (this) {
            SONG -> if (plural) Res.string.songs else Res.string.song
            ARTIST -> if (plural) Res.string.artists else Res.string.artist
            PLAYLIST_REM, PLAYLIST_LOC -> if (plural) Res.string.playlists else Res.string.playlist
        }

    fun referenceFromId(id: String): MediaItem = when (this) {
        SONG -> SongRef(id)
        ARTIST -> ArtistRef(id)
        PLAYLIST_REM -> RemotePlaylistRef(id)
        PLAYLIST_LOC -> LocalPlaylistRef(id)
    }

    fun dataFromId(id: String): MediaItemData = when (this) {
        SONG -> SongData(id)
        ARTIST -> ArtistData(id)
        PLAYLIST_REM -> RemotePlaylistData(id)
        PLAYLIST_LOC -> LocalPlaylistData(id)
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
