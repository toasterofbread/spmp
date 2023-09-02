package com.toasterofbread.spmp.model.mediaitem.loader

import com.toasterofbread.spmp.model.mediaitem.song.SongRef
import com.toasterofbread.spmp.model.mediaitem.song.SongLikedStatus
import com.toasterofbread.spmp.model.mediaitem.song.toLong
import com.toasterofbread.spmp.platform.PlatformContext
import com.toasterofbread.spmp.youtubeapi.endpoint.SongLikedEndpoint

internal object SongLikedLoader: ItemStateLoader<String, SongLikedStatus>() {
    suspend fun loadSongLiked(song_id: String, context: PlatformContext, endpoint: SongLikedEndpoint): Result<SongLikedStatus> {
        require(endpoint.isImplemented())
        return performLoad(song_id) {
            endpoint.getSongLiked(SongRef(song_id))
                .onSuccess { liked ->
                    context.database.songQueries.updatelikedById(liked.toLong(), song_id)
                }
        }
    }
}
