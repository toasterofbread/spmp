package com.toasterofbread.spmp.youtubeapi.endpoint

import com.toasterofbread.spmp.model.mediaitem.Playlist
import com.toasterofbread.spmp.youtubeapi.YoutubeApi

abstract class AccountPlaylistAddSongsEndpoint: YoutubeApi.UserAuthState.UserAuthEndpoint() {
    abstract suspend fun addSongs(playlist: Playlist, song_ids: Collection<String>): Result<Unit>
}
