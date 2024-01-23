package com.toasterofbread.spmp.youtubeapi.endpoint

import com.toasterofbread.spmp.model.mediaitem.artist.ArtistData
import com.toasterofbread.spmp.youtubeapi.YoutubeApi

abstract class LikedArtistsEndpoint: YoutubeApi.UserAuthState.UserAuthEndpoint() {
    abstract suspend fun getLikedArtists(): Result<List<ArtistData>>
}
