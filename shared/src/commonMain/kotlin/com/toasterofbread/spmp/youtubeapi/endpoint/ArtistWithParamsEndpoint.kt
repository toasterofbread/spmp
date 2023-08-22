package com.toasterofbread.spmp.youtubeapi.endpoint

import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.youtubeapi.YoutubeApi

data class ArtistWithParamsRow(val title: String?, val items: List<MediaItem>)

abstract class ArtistWithParamsEndpoint: YoutubeApi.Endpoint() {
    abstract suspend fun loadArtistWithParams(artist_id: String, browse_params: String): Result<List<ArtistWithParamsRow>>
}
