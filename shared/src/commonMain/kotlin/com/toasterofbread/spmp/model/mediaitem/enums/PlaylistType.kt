package com.toasterofbread.spmp.model.mediaitem.enums

import com.toasterofbread.spmp.resources.getString
import dev.toastbits.ytmkt.model.external.mediaitem.YtmPlaylist

enum class PlaylistType {
    PLAYLIST, LOCAL, ALBUM, AUDIOBOOK, PODCAST, RADIO;

    companion object {
        fun fromYtmPlaylistType(type: YtmPlaylist.Type): PlaylistType =
            when (type) {
                YtmPlaylist.Type.PLAYLIST -> PLAYLIST
                YtmPlaylist.Type.ALBUM -> ALBUM
                YtmPlaylist.Type.AUDIOBOOK -> AUDIOBOOK
                YtmPlaylist.Type.PODCAST -> PODCAST
                YtmPlaylist.Type.RADIO  -> RADIO
            }
    }
}

fun PlaylistType?.getReadable(plural: Boolean): String =
    getString(when (this) {
        PlaylistType.PLAYLIST, PlaylistType.LOCAL, null -> if (plural) "playlists" else "playlist"
        PlaylistType.ALBUM -> if (plural) "albums" else "album"
        PlaylistType.AUDIOBOOK -> if (plural) "audiobooks" else "audiobook"
        PlaylistType.PODCAST -> if (plural) "podcasts" else "podcast"
        PlaylistType.RADIO -> if (plural) "radios" else "radio"
    })
