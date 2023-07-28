package com.toasterofbread.spmp.model.mediaitem.loader

import com.toasterofbread.Database
import com.toasterofbread.spmp.api.getOrThrowHere
import com.toasterofbread.spmp.model.mediaitem.SongData
import com.toasterofbread.spmp.model.mediaitem.toLong
import kotlinx.coroutines.Deferred
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal object SongLikedLoader {
    private val lock = ReentrantLock()
    private val loading_items: MutableMap<String, Deferred<SongData>> = mutableMapOf()

    inline fun <T> withLock(action: () -> T): T = lock.withLock(action)

    suspend fun loadSongLiked(song: SongData, db: Database): Result<SongData> {
        return performSafeLoad(
            song.id,
            lock,
            loading_items,
            keep_results = true
        ) {
            song.liked = com.toasterofbread.spmp.api.loadSongLiked(song.id).getOrThrowHere()
            db.songQueries.updatelikedById(song.liked.toLong(), song.id)
            song
        }
    }
}
