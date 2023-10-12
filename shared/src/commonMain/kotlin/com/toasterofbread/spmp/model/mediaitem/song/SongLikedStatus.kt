package com.toasterofbread.spmp.model.mediaitem.song

import com.toasterofbread.spmp.platform.PlatformContext
import com.toasterofbread.spmp.youtubeapi.EndpointNotImplementedException
import com.toasterofbread.spmp.youtubeapi.endpoint.SetSongLikedEndpoint

enum class SongLikedStatus {
    NEUTRAL, LIKED, DISLIKED;

    fun interface Listener {
        fun onSongLikedStatusChanged(song: Song, liked_status: SongLikedStatus)
    }

    companion object {
        private val listeners: MutableList<Listener> = mutableListOf()

        fun addListener(listener: Listener) {
            synchronized(listeners) {
                listeners.add(listener)
            }
        }
        fun removeListener(listener: Listener) {
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

suspend fun Song.updateLiked(liked: SongLikedStatus, endpoint: SetSongLikedEndpoint, context: PlatformContext): Result<Unit> {
    if (!endpoint.isImplemented()) {
        return Result.failure(EndpointNotImplementedException(endpoint))
    }

    Liked.set(liked, context.database)
    SongLikedStatus.onSongLikedStatusChanged(this, liked)

    return endpoint.setSongLiked(this, liked).onSuccess {
        Liked.set(liked, context.database)
        SongLikedStatus.onSongLikedStatusChanged(this, liked)
    }
}
