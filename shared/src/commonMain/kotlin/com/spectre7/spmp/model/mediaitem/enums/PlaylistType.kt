package com.toasterofbread.spmp.model.mediaitem.enums

import com.toasterofbread.spmp.resources.getString

enum class PlaylistType {
    PLAYLIST, ALBUM, AUDIOBOOK, RADIO;

    companion object {
        fun fromTypeString(type: String): PlaylistType {
            return when (type) {
                "MUSIC_PAGE_TYPE_PLAYLIST" -> PLAYLIST
                "MUSIC_PAGE_TYPE_ALBUM" -> ALBUM
                "MUSIC_PAGE_TYPE_AUDIOBOOK" -> AUDIOBOOK
                else -> throw NotImplementedError(type)
            }
        }
    }
}

fun PlaylistType?.getReadable(plural: Boolean): String {
    return getString(when (this) {
        PlaylistType.PLAYLIST, null -> if (plural) "playlists" else "playlist"
        PlaylistType.ALBUM -> if (plural) "albums" else "album"
        PlaylistType.AUDIOBOOK -> if (plural) "audiobooks" else "audiobook"
        PlaylistType.RADIO -> if (plural) "radios" else "radio"
    })
}
