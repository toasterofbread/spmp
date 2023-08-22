package com.toasterofbread.spmp.youtubeapi.endpoint

import com.toasterofbread.spmp.model.mediaitem.Song
import com.toasterofbread.spmp.youtubeapi.YoutubeApi

abstract class MarkSongAsWatchedEndpoint: YoutubeApi.UserAuthState.UserAuthEndpoint() {
    abstract suspend fun markSongAsWatched(song: Song): Result<Unit>
}
