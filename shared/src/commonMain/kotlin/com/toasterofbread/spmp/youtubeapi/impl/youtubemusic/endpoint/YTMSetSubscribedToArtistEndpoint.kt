package com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.endpoint

import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.youtubeapi.endpoint.SetSubscribedToArtistEndpoint
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.YoutubeMusicAuthInfo
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.unit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

class YTMSetSubscribedToArtistEndpoint(override val auth: YoutubeMusicAuthInfo): SetSubscribedToArtistEndpoint() {
    override suspend fun setSubscribedToArtist(artist: Artist, subscribed: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        val subscribe_channel_id =
            api.db.artistQueries.subscribeChannelIdById(artist.id).executeAsOneOrNull()?.subscribe_channel_id
                ?: artist.id

        val request: Request = Request.Builder()
            .endpointUrl("/youtubei/v1/subscription/${if (subscribed) "subscribe" else "unsubscribe"}")
            .addAuthApiHeaders()
            .postWithBody(
                mapOf("channelIds" to listOf(subscribe_channel_id))
            )
            .build()

        return@withContext api.performRequest(request).unit()
    }
}
