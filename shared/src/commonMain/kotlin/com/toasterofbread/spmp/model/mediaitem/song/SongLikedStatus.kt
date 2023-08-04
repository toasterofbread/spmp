package com.toasterofbread.spmp.model.mediaitem.song

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
