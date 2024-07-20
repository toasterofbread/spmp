package com.toasterofbread.spmp.model.mediaitem.enums

import androidx.compose.runtime.Composable
import dev.toastbits.ytmkt.model.external.mediaitem.YtmPlaylist
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.album
import spmp.shared.generated.resources.albums
import spmp.shared.generated.resources.audiobook
import spmp.shared.generated.resources.audiobooks
import spmp.shared.generated.resources.playlist
import spmp.shared.generated.resources.playlists
import spmp.shared.generated.resources.podcast
import spmp.shared.generated.resources.podcasts
import spmp.shared.generated.resources.radio
import spmp.shared.generated.resources.radios

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

@Composable
fun PlaylistType?.getReadable(plural: Boolean): String =
    stringResource(
        when (this) {
            PlaylistType.PLAYLIST, PlaylistType.LOCAL, null -> if (plural) Res.string.playlists else Res.string.playlist
            PlaylistType.ALBUM -> if (plural) Res.string.albums else Res.string.album
            PlaylistType.AUDIOBOOK -> if (plural) Res.string.audiobooks else Res.string.audiobook
            PlaylistType.PODCAST -> if (plural) Res.string.podcasts else Res.string.podcast
            PlaylistType.RADIO -> if (plural) Res.string.radios else Res.string.radio
        }
    )
