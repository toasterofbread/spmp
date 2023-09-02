package com.toasterofbread.spmp.youtubeapi.endpoint

import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.youtubeapi.YoutubeApi

abstract class SubscribedToArtistEndpoint: YoutubeApi.UserAuthState.UserAuthEndpoint() {
    abstract suspend fun isSubscribedToArtist(artist: Artist): Result<Boolean>
}
