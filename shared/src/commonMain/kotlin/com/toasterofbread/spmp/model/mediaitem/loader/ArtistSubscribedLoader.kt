package com.toasterofbread.spmp.model.mediaitem.loader

import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.platform.AppContext

internal object ArtistSubscribedLoader: ItemStateLoader<String, Boolean>() {
    suspend fun loadArtistSubscribed(
        artist: Artist, 
        context: AppContext
    ): Result<Boolean>? =
        performLoad(artist.id) {
            val result = context.ytapi.user_auth_state!!.SubscribedToArtist.isSubscribedToArtist(artist.id)
            artist.Subscribed.set(result.getOrNull(), context.database)
            return@performLoad result
        }
}
