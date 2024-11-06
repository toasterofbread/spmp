package com.toasterofbread.spmp.util

import dev.toastbits.ytmkt.model.external.SongLikedStatus

fun SongLikedStatus?.getToggleTarget(): SongLikedStatus =
    when (this) {
        null,
        SongLikedStatus.NEUTRAL -> SongLikedStatus.LIKED
        SongLikedStatus.DISLIKED,
        SongLikedStatus.LIKED -> SongLikedStatus.NEUTRAL
    }
