package com.toasterofbread.spmp.model.mediaitem

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

//fun updateStatus(in_thread: Boolean = true) {
//    synchronized(status_state) {
//        if (loading || status == Status.UNAVAILABLE) {
//            return
//        }
//        loading = true
//    }
//
//    fun update() {
//        val result = getSongLiked(id)
//        synchronized(status_state) {
//            loading = false
//            result.fold(
//                { status ->
//                    this.status = when (status) {
//                        true -> Status.LIKED
//                        false -> Status.DISLIKED
//                        null -> Status.NEUTRAL
//                    }
//                },
//                {
//                    status = Status.UNAVAILABLE
//                }
//            )
//        }
//    }
//
//    if (in_thread) {
//        thread { update() }
//    }
//    else {
//        update()
//    }
//}
