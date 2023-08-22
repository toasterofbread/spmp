package com.toasterofbread.spmp.youtubeapi

import com.toasterofbread.spmp.model.mediaitem.Song
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.RelatedGroup

abstract class SongRelatedContentEndpoint: YoutubeApi.Endpoint() {
    abstract suspend fun getSongRelated(song: Song): Result<List<RelatedGroup>>
}