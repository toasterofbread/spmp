package com.toasterofbread.spmp.model.mediaitem.enums

import com.toasterofbread.spmp.resources.getString

enum class PlaylistType {
    LOCAL, PLAYLIST, ALBUM, AUDIOBOOK, PODCAST, RADIO;

    companion object {
        fun fromBrowseEndpointType(type: String): PlaylistType {
            return when (type) {
                "MUSIC_PAGE_TYPE_PLAYLIST" -> PLAYLIST
                "MUSIC_PAGE_TYPE_ALBUM" -> ALBUM
                "MUSIC_PAGE_TYPE_AUDIOBOOK" -> AUDIOBOOK
                "MUSIC_PAGE_TYPE_PODCAST" -> PODCAST
                "MUSIC_PAGE_TYPE_RADIO" -> RADIO
                else -> PLAYLIST
            }
        }
    }
}

fun PlaylistType?.getReadable(plural: Boolean): String {
    return getString(when (this) {
        PlaylistType.PLAYLIST, PlaylistType.LOCAL, null -> if (plural) "playlists" else "playlist"
        PlaylistType.ALBUM -> if (plural) "albums" else "album"
        PlaylistType.AUDIOBOOK -> if (plural) "audiobooks" else "audiobook"
        PlaylistType.PODCAST -> if (plural) "podcasts" else "podcast"
        PlaylistType.RADIO -> if (plural) "radios" else "radio"
    })
}
