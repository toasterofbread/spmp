package com.toasterofbread.spmp.ui

import androidx.annotation.DrawableRes
import com.toasterofbread.spmp.shared.R
import dev.toastbits.ytmkt.model.external.SongLikedStatus

@DrawableRes
fun SongLikedStatus?.getAndroidIcon(authenticated: Boolean): Int =
    if (authenticated)
        when (this) {
            null,
            SongLikedStatus.NEUTRAL -> R.drawable.ic_thumb_up_off
            SongLikedStatus.LIKED -> R.drawable.ic_thumb_up
            SongLikedStatus.DISLIKED -> R.drawable.ic_thumb_down
        }
    else
        when (this) {
            null,
            SongLikedStatus.DISLIKED,
            SongLikedStatus.NEUTRAL -> R.drawable.ic_heart_off
            SongLikedStatus.LIKED -> R.drawable.ic_heart
        }
