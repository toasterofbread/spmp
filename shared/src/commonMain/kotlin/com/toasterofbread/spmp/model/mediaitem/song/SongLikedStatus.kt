package com.toasterofbread.spmp.model.mediaitem.song

import com.toasterofbread.spmp.platform.AppContext
import dev.toastbits.ytmkt.endpoint.SetSongLikedEndpoint
import dev.toastbits.ytmkt.model.external.SongLikedStatus

fun interface SongLikedStatusListener {
    fun onSongLikedStatusChanged(song: Song, liked_status: SongLikedStatus)

    companion object {
        private val listeners: MutableList<SongLikedStatusListener> = mutableListOf()

        fun addListener(listener: SongLikedStatusListener) {
            synchronized(listeners) {
                listeners.add(listener)
            }
        }

        fun removeListener(listener: SongLikedStatusListener) {
            synchronized(listeners) {
                listeners.remove(listener)
            }
        }

        fun onSongLikedStatusChanged(song: Song, liked_status: SongLikedStatus) {
            for (listener in listeners) {
                listener.onSongLikedStatusChanged(song, liked_status)
            }
        }
    }
}

fun Long?.toSongLikedStatus(): SongLikedStatus? =
    when(this) {
        0L -> SongLikedStatus.DISLIKED
        1L -> SongLikedStatus.NEUTRAL
        2L -> SongLikedStatus.LIKED
        else -> null
    }

fun SongLikedStatus?.toLong(): Long? =
    when (this) {
        SongLikedStatus.DISLIKED -> 0L
        SongLikedStatus.NEUTRAL -> 1L
        SongLikedStatus.LIKED -> 2L
        null -> null
    }

suspend fun Song.updateLiked(liked: SongLikedStatus, endpoint: SetSongLikedEndpoint?, context: AppContext): Result<Unit> {
    Liked.set(liked, context.database)
    SongLikedStatusListener.onSongLikedStatusChanged(this, liked)

    if (endpoint?.isImplemented() == true) {
        return endpoint.setSongLiked(id, liked).onSuccess {
            Liked.set(liked, context.database)
            SongLikedStatusListener.onSongLikedStatusChanged(this, liked)
        }
    }

    return Result.success(Unit)
}
