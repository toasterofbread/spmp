package com.toasterofbread.spmp.youtubeapi.endpoint

import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylist
import com.toasterofbread.spmp.youtubeapi.YoutubeApi

abstract class AccountPlaylistAddSongsEndpoint: YoutubeApi.UserAuthState.UserAuthEndpoint() {
    abstract suspend fun addSongs(playlist: RemotePlaylist, song_ids: Collection<String>): Result<Unit>
}
