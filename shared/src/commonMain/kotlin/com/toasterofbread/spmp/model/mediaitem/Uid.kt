package com.toasterofbread.spmp.model.mediaitem

import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType

fun getMediaItemFromUid(uid: String): MediaItem {
    val id = uid.substring(1)
    return MediaItemType.fromUid(uid).referenceFromId(id)
}

fun MediaItemType.Companion.fromUid(uid: String): MediaItemType =
    when(uid.first()) {
        's' -> MediaItemType.SONG
        'a' -> MediaItemType.ARTIST
        'p' -> MediaItemType.PLAYLIST_REM
        'l' -> MediaItemType.PLAYLIST_LOC
        else -> throw NotImplementedError(uid)
    }

fun MediaItem.getUid(): String =
    when (getType()) {
        MediaItemType.SONG -> "s$id"
        MediaItemType.ARTIST -> "a$id"
        MediaItemType.PLAYLIST_REM -> "p$id"
        MediaItemType.PLAYLIST_LOC -> "l$id"
    }
