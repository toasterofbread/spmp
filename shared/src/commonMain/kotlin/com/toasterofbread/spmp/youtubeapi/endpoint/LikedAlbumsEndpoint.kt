package com.toasterofbread.spmp.youtubeapi.endpoint

import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylistData
import com.toasterofbread.spmp.youtubeapi.YoutubeApi

abstract class LikedAlbumsEndpoint: YoutubeApi.UserAuthState.UserAuthEndpoint() {
    abstract suspend fun getLikedAlbums(): Result<List<RemotePlaylistData>>
}
