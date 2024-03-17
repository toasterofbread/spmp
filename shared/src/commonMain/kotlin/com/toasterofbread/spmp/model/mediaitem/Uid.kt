package com.toasterofbread.spmp.model.mediaitem

import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import dev.toastbits.ytmkt.model.external.mediaitem.YtmArtist
import dev.toastbits.ytmkt.model.external.mediaitem.YtmMediaItem
import dev.toastbits.ytmkt.model.external.mediaitem.YtmPlaylist
import dev.toastbits.ytmkt.model.external.mediaitem.YtmSong

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

fun YtmMediaItem.getType(): MediaItemType =
    when (this) {
        is MediaItem -> getType()
        is YtmSong -> MediaItemType.SONG
        is YtmPlaylist -> MediaItemType.PLAYLIST_REM
        is YtmArtist -> MediaItemType.ARTIST
        else -> throw NotImplementedError(this::class.toString())
    }

fun YtmMediaItem.getUid(): String =
    when (getType()) {
        MediaItemType.SONG -> "s$id"
        MediaItemType.ARTIST -> "a$id"
        MediaItemType.PLAYLIST_REM -> "p$id"
        MediaItemType.PLAYLIST_LOC -> "l$id"
    }
