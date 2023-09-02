package com.toasterofbread.spmp.model.mediaitem

import com.toasterofbread.spmp.model.mediaitem.artist.ArtistRef
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.model.mediaitem.playlist.LocalPlaylistRef
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylistRef
import com.toasterofbread.spmp.model.mediaitem.song.SongRef

fun getMediaItemFromUid(uid: String): MediaItem {
    val id = uid.substring(1)
    return when(uid.first()) {
        's' -> SongRef(id)
        'a' -> ArtistRef(id)
        'p' -> RemotePlaylistRef(id)
        'l' -> LocalPlaylistRef(id)
        else -> throw NotImplementedError(uid)
    }
}

fun MediaItem.getUid(): String =
    when (getType()) {
        MediaItemType.SONG -> "s$id"
        MediaItemType.ARTIST -> "a$id"
        MediaItemType.PLAYLIST_REM -> "p$id"
        MediaItemType.PLAYLIST_LOC -> "l$id"
    }
