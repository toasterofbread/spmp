package com.toasterofbread.spmp.youtubeapi.endpoint

import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.youtubeapi.YoutubeApi

abstract class GenericFeedViewMorePageEndpoint: YoutubeApi.Endpoint() {
    abstract suspend fun getGenericFeedViewMorePage(browse_id: String): Result<List<MediaItem>>
}
