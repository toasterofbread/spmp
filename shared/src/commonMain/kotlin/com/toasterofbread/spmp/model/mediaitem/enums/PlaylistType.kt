package com.toasterofbread.spmp.model.mediaitem.enums

import com.toasterofbread.spmp.resources.getString
import dev.toastbits.ytmkt.model.external.mediaitem.YtmPlaylist

fun YtmPlaylist.Type?.getReadable(plural: Boolean): String {
    return getString(when (this) {
        YtmPlaylist.Type.PLAYLIST, YtmPlaylist.Type.LOCAL, null -> if (plural) "playlists" else "playlist"
        YtmPlaylist.Type.ALBUM -> if (plural) "albums" else "album"
        YtmPlaylist.Type.AUDIOBOOK -> if (plural) "audiobooks" else "audiobook"
        YtmPlaylist.Type.PODCAST -> if (plural) "podcasts" else "podcast"
        YtmPlaylist.Type.RADIO -> if (plural) "radios" else "radio"
    })
}
