package com.toasterofbread.spmp.model.mediaitem.artist

import com.toasterofbread.spmp.platform.AppContext
import dev.toastbits.ytmkt.endpoint.SetSubscribedToArtistEndpoint

suspend fun Artist.updateSubscribed(
    subscribed: Boolean,
    endpoint: SetSubscribedToArtistEndpoint,
    context: AppContext
): Result<Unit> = runCatching {
    endpoint.setSubscribedToArtist(id, subscribed).getOrThrow()
    Subscribed.set(subscribed, context.database)
}
