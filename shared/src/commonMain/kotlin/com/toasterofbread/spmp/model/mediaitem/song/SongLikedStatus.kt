package com.toasterofbread.spmp.model.mediaitem.song

import com.toasterofbread.spmp.platform.PlatformContext
import com.toasterofbread.spmp.youtubeapi.EndpointNotImplementedException
import com.toasterofbread.spmp.youtubeapi.endpoint.SetSongLikedEndpoint

enum class SongLikedStatus {
    NEUTRAL, LIKED, DISLIKED
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

suspend fun Song.updateLiked(liked: SongLikedStatus, endpoint: SetSongLikedEndpoint, context: PlatformContext = SpMp.context): Result<Unit> {
    if (!endpoint.isImplemented()) {
        return Result.failure(EndpointNotImplementedException(endpoint))
    }

    return endpoint.setSongLiked(this, liked).onSuccess {
        Liked.set(liked, context.database)
    }
}

