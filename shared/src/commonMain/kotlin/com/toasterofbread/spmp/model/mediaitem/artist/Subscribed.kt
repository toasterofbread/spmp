package com.toasterofbread.spmp.model.mediaitem.artist

import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.youtubeapi.EndpointNotImplementedException
import com.toasterofbread.spmp.youtubeapi.endpoint.SetSubscribedToArtistEndpoint

suspend fun Artist.updateSubscribed(subscribed: Boolean, endpoint: SetSubscribedToArtistEndpoint, context: AppContext): Result<Unit> {
    if (!endpoint.isImplemented()) {
        return Result.failure(EndpointNotImplementedException(endpoint))
    }

    return endpoint.setSubscribedToArtist(this, subscribed).onSuccess {
        Subscribed.set(subscribed, context.database)
    }
}
