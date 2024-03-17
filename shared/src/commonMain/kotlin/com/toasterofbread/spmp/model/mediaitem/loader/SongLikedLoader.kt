package com.toasterofbread.spmp.model.mediaitem.loader

import com.toasterofbread.spmp.model.mediaitem.song.toLong
import com.toasterofbread.spmp.model.mediaitem.song.toSongLikedStatus
import com.toasterofbread.spmp.platform.AppContext
import dev.toastbits.ytmkt.endpoint.SongLikedEndpoint
import dev.toastbits.ytmkt.model.external.SongLikedStatus

internal object SongLikedLoader: ItemStateLoader<String, SongLikedStatus>() {
    suspend fun loadSongLiked(song_id: String, context: AppContext, endpoint: SongLikedEndpoint?): Result<SongLikedStatus> {
        return performLoad(song_id) {
            if (endpoint?.isImplemented() == true) {
                endpoint.getSongLiked(song_id)
                    .onSuccess { liked ->
                        context.database.songQueries.updatelikedById(liked.toLong(), song_id)
                    }
            }
            else {
                Result.success(
                    context.database.songQueries.likedById(song_id).executeAsOneOrNull()?.liked.toSongLikedStatus() ?: SongLikedStatus.NEUTRAL
                )
            }
        }
    }
}
