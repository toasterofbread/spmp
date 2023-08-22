package com.toasterofbread.spmp.model.mediaitem.loader

import com.toasterofbread.spmp.model.mediaitem.Artist
import com.toasterofbread.spmp.model.mediaitem.db.toSQLBoolean
import com.toasterofbread.spmp.platform.PlatformContext

internal object ArtistSubscribedLoader: ItemStateLoader<String, Boolean>() {
    suspend fun loadArtistSubscribed(artist: Artist, context: PlatformContext): Result<Boolean>? {
        val endpoint = context.ytapi.user_auth_state?.SubscribedToArtist
        if (endpoint?.isImplemented() != true) {
            return null
        }

        return performLoad(artist.id) {
            endpoint.isSubscribedToArtist(artist).onSuccess { subscribed ->
                context.database.artistQueries.updateSubscribedById(subscribed.toSQLBoolean(), artist.id)
            }
        }
    }
}
