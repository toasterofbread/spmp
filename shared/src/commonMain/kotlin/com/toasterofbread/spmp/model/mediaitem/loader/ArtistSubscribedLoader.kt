package com.toasterofbread.spmp.model.mediaitem.loader

import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.youtubeapi.EndpointNotImplementedException
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.isOwnChannel

internal object ArtistSubscribedLoader: ItemStateLoader<String, Boolean>() {
    suspend fun loadArtistSubscribed(artist: Artist, context: AppContext): Result<Boolean>? {
        val endpoint = context.ytapi.user_auth_state?.SubscribedToArtist
        if (endpoint?.isImplemented() != true) {
            throw EndpointNotImplementedException(endpoint)
        }

        return performLoad(artist.id) {
            val result = endpoint.isSubscribedToArtist(artist)
            artist.Subscribed.set(result.getOrNull(), context.database)
            return@performLoad result
        }
    }
}
