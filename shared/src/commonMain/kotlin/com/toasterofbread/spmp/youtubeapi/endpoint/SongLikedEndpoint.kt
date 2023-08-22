package com.toasterofbread.spmp.youtubeapi.endpoint

import com.toasterofbread.spmp.model.mediaitem.Song
import com.toasterofbread.spmp.model.mediaitem.song.SongLikedStatus
import com.toasterofbread.spmp.youtubeapi.YoutubeApi

abstract class SongLikedEndpoint: YoutubeApi.UserAuthState.UserAuthEndpoint() {
    abstract suspend fun getSongLiked(song: Song): Result<SongLikedStatus>
}
