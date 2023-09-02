package com.toasterofbread.spmp.youtubeapi.endpoint

import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.song.SongLikedStatus
import com.toasterofbread.spmp.youtubeapi.YoutubeApi

abstract class SetSongLikedEndpoint: YoutubeApi.UserAuthState.UserAuthEndpoint() {
    abstract suspend fun setSongLiked(song: Song, liked: SongLikedStatus): Result<Unit>
}
