package com.toasterofbread.spmp.model.mediaitem

import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType

fun getMediaItemFromUid(uid: String): MediaItem {
    val id = uid.substring(1)
    return when(uid.first()) {
        's' -> SongRef(id)
        'a' -> ArtistRef(id)
        'p' -> AccountPlaylistRef(id)
        'l' -> LocalPlaylistRef(id)
        else -> throw NotImplementedError(uid)
    }
}

fun MediaItem.getUid(): String =
    when (getType()) {
        MediaItemType.SONG -> "s$id"
        MediaItemType.ARTIST -> "a$id"
        MediaItemType.PLAYLIST_ACC -> "p$id"
        MediaItemType.PLAYLIST_LOC -> "l$id"
        MediaItemType.PLAYLIST_BROWSEPARAMS -> throw IllegalStateException(id)
    }
