package com.toasterofbread.spmp.model.mediaitem.artist

import SpMp
import com.toasterofbread.Database
import com.toasterofbread.spmp.api.subscribeOrUnsubscribeArtist
import com.toasterofbread.spmp.model.mediaitem.Artist
import com.toasterofbread.spmp.model.mediaitem.db.toSQLBoolean

suspend fun Artist.setSubscribed(subscribed: Boolean, db: Database = SpMp.context.database): Result<Unit> {
    return subscribeOrUnsubscribeArtist(this, subscribed, db).onSuccess {
        db.artistQueries.updateSubscribedById(subscribed.toSQLBoolean(), id)
    }
}
