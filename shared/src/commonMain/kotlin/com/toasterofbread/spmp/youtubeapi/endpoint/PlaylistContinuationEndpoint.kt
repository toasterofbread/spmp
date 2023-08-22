package com.toasterofbread.spmp.youtubeapi.endpoint

import com.toasterofbread.spmp.model.mediaitem.MediaItemData
import com.toasterofbread.spmp.youtubeapi.YoutubeApi

abstract class PlaylistContinuationEndpoint: YoutubeApi.Endpoint() {
    abstract suspend fun getPlaylistContinuation(
        initial: Boolean,
        token: String,
        skip_initial: Int = 0,
    ): Result<Pair<List<MediaItemData>, String?>>
}
