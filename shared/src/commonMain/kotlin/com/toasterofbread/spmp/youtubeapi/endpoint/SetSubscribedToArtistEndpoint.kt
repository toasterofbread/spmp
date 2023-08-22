package com.toasterofbread.spmp.youtubeapi.endpoint

import com.toasterofbread.spmp.model.mediaitem.Artist
import com.toasterofbread.spmp.youtubeapi.YoutubeApi

abstract class SetSubscribedToArtistEndpoint: YoutubeApi.UserAuthState.UserAuthEndpoint() {
    abstract suspend fun setSubscribedToArtist(artist: Artist, subscribed: Boolean): Result<Unit>
}
