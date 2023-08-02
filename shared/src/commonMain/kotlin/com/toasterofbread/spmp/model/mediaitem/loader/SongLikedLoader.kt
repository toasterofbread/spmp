package com.toasterofbread.spmp.model.mediaitem.loader

import com.toasterofbread.Database
import com.toasterofbread.spmp.model.mediaitem.SongLikedStatus
import com.toasterofbread.spmp.model.mediaitem.toLong

internal object SongLikedLoader: ItemStateLoader<String, SongLikedStatus>() {
    suspend fun loadSongLiked(song_id: String, db: Database): Result<SongLikedStatus> {
        return performLoad(song_id) {
            com.toasterofbread.spmp.api.loadSongLiked(song_id)
                .onSuccess { liked ->
                    db.songQueries.updatelikedById(liked.toLong(), song_id)
                }
        }
    }
}
