package com.toasterofbread.spmp.model.mediaitem.loader

import com.toasterofbread.spmp.model.mediaitem.Artist
import com.toasterofbread.spmp.model.mediaitem.db.toSQLBoolean
import com.toasterofbread.spmp.platform.PlatformContext
import com.toasterofbread.spmp.youtubeapi.EndpointNotImplementedException

internal object ArtistSubscribedLoader: ItemStateLoader<String, Boolean>() {
    suspend fun loadArtistSubscribed(artist: Artist, context: PlatformContext): Result<Boolean>? {
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
