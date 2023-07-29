package com.toasterofbread.spmp.model.mediaitem.loader

import com.toasterofbread.Database
import com.toasterofbread.spmp.api.isSubscribedToArtist
import com.toasterofbread.spmp.model.mediaitem.Artist
import com.toasterofbread.spmp.model.mediaitem.toSQLBoolean

internal object ArtistSubscribedLoader: ItemStateLoader<String, Boolean>() {
    suspend fun loadArtistSubscribed(artist: Artist, db: Database): Result<Boolean> {
        return performResultLoad(artist.id) {
            isSubscribedToArtist(artist).onSuccess { subscribed ->
                db.artistQueries.updateSubscribedById(subscribed.toSQLBoolean(), artist.id)
            }
        }
    }
}
