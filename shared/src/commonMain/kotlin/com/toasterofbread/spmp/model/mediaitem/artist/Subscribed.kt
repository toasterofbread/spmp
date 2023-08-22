package com.toasterofbread.spmp.model.mediaitem.artist

import SpMp
import com.toasterofbread.spmp.model.mediaitem.Artist
import com.toasterofbread.spmp.model.mediaitem.db.toSQLBoolean
import com.toasterofbread.spmp.platform.PlatformContext
import com.toasterofbread.spmp.youtubeapi.EndpointNotImplementedException
import com.toasterofbread.spmp.youtubeapi.endpoint.SetSubscribedToArtistEndpoint

suspend fun Artist.setSubscribed(subscribed: Boolean, endpoint: SetSubscribedToArtistEndpoint, context: PlatformContext = SpMp.context): Result<Unit> {
    if (!endpoint.isImplemented()) {
        return Result.failure(EndpointNotImplementedException(endpoint))
    }

    return endpoint.setSubscribedToArtist(this, subscribed).onSuccess {
        context.database.artistQueries.updateSubscribedById(subscribed.toSQLBoolean(), id)
    }
}
