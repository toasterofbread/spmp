package com.toasterofbread.spmp.youtubeapi.endpoint

import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.layout.BrowseParamsData
import com.toasterofbread.spmp.youtubeapi.YoutubeApi

data class ArtistWithParamsRow(val title: String?, val items: List<MediaItem>)

abstract class ArtistWithParamsEndpoint: YoutubeApi.Endpoint() {
    abstract suspend fun loadArtistWithParams(browse_params: BrowseParamsData): Result<List<ArtistWithParamsRow>>
}
