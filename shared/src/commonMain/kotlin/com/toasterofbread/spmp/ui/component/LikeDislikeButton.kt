package com.toasterofbread.spmp.ui.component

import SpMp
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.api.Api
import com.toasterofbread.spmp.api.setSongLiked
import com.toasterofbread.spmp.model.mediaitem.Song
import com.toasterofbread.spmp.model.mediaitem.SongLikedStatus
import com.toasterofbread.spmp.model.mediaitem.loader.SongLikedLoader
import com.toasterofbread.spmp.model.mediaitem.observeAsState
import com.toasterofbread.spmp.model.mediaitem.toLong
import com.toasterofbread.spmp.model.mediaitem.toSongLikedStatus
import com.toasterofbread.spmp.platform.vibrateShort
import com.toasterofbread.utils.composable.SubtleLoadingIndicator

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LikeDislikeButton(
    song: Song,
    modifier: Modifier = Modifier,
    getEnabled: (() -> Boolean)? = null,
    colourProvider: () -> Color
) {
    if (!Api.ytm_authenticated) {
        return
    }

    val db = SpMp.context.database

    var liked_status by db.songQueries
        .likedById(song.id)
        .observeAsState(
            mapValue = { it.executeAsOneOrNull()?.liked.toSongLikedStatus() },
            onExternalChange = { status ->
                if (status != null) {
                    setSongLiked(song.id, status)
                }
                db.songQueries.updatelikedById(status.toLong(), song.id)
            }
        )
    var loading by remember { mutableStateOf(false) }

    LaunchedEffect(song.id) {
        if (liked_status == null) {
            loading = true

            val load_result = SongLikedLoader.loadSongLiked(song.id, SpMp.context.database)
            load_result.onSuccess {
                liked_status = it
            }
        }

        loading = false
    }

    val rotation by animateFloatAsState(if (liked_status == SongLikedStatus.DISLIKED) 180f else 0f)

    Box(
        modifier,
        contentAlignment = Alignment.Center
    ) {
        Crossfade(liked_status) { status ->
            if (status != null) {
                Icon(
                    if (status != SongLikedStatus.NEUTRAL) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                    null,
                    Modifier
                        .rotate(rotation)
                        .combinedClickable(
                            remember { MutableInteractionSource() },
                            rememberRipple(false),
                            enabled = getEnabled?.invoke() != false,
                            onClick = { when (status) {
                                SongLikedStatus.LIKED, SongLikedStatus.DISLIKED -> liked_status = SongLikedStatus.NEUTRAL
                                SongLikedStatus.NEUTRAL -> liked_status = SongLikedStatus.LIKED
                            }},
                            onLongClick = {
                                SpMp.context.vibrateShort()
                                when (status) {
                                    SongLikedStatus.LIKED, SongLikedStatus.NEUTRAL -> liked_status = SongLikedStatus.DISLIKED
                                    SongLikedStatus.DISLIKED -> liked_status = SongLikedStatus.LIKED
                                }
                            }
                        ),
                    tint = colourProvider()
                )
            }
            else if (loading) {
                SubtleLoadingIndicator(Modifier.size(24.dp), colourProvider)
            }
        }
    }
}
