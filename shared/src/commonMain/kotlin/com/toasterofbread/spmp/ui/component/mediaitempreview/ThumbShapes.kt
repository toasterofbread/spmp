package com.toasterofbread.spmp.ui.component.mediaitempreview

import SpMp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.platform.FormFactor

fun getSongThumbShape(): Shape =
    if (FormFactor.getCurrent(SpMp.player_state).is_large) RoundedCornerShape(4.dp)
    else RoundedCornerShape(10.dp)

fun getArtistThumbShape(): Shape = RoundedCornerShape(50)

fun getPlaylistThumbShape(): Shape = RoundedCornerShape(10.dp)

fun MediaItemType.getThumbShape(): Shape =
    when (this) {
        MediaItemType.SONG -> getSongThumbShape()
        MediaItemType.ARTIST -> getArtistThumbShape()
        MediaItemType.PLAYLIST_REM, MediaItemType.PLAYLIST_LOC -> getPlaylistThumbShape()
    }
