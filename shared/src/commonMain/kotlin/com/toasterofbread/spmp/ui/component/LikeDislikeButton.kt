package com.toasterofbread.spmp.ui.component

import SpMp
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.api.Api
import com.toasterofbread.spmp.model.mediaitem.Song
import com.toasterofbread.spmp.model.mediaitem.SongLikeStatus
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

    LaunchedEffect(song) {
        if (song.like_status.status == SongLikeStatus.Status.UNKNOWN) {
            song.like_status.updateStatus()
        }
    }

    Box(
        modifier,
        contentAlignment = Alignment.Center
    ) {
        Crossfade(
            Pair(
                song.like_status.status,
                song.like_status.loading
            )
        ) {
            val (status, loading) = it

            if (status.is_available) {
                check(status == SongLikeStatus.Status.LIKED || status == SongLikeStatus.Status.DISLIKED || status == SongLikeStatus.Status.NEUTRAL)

                val rotation by animateFloatAsState(if (status == SongLikeStatus.Status.DISLIKED) 180f else 0f)

                Icon(
                    if (status != SongLikeStatus.Status.NEUTRAL) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                    null,
                    Modifier
                        .rotate(rotation)
                        .combinedClickable(
                            enabled = getEnabled?.invoke() != false,
                            onClick = { when (status) {
                                SongLikeStatus.Status.LIKED -> song.like_status.setLiked(null)
                                SongLikeStatus.Status.DISLIKED -> song.like_status.setLiked(null)
                                SongLikeStatus.Status.NEUTRAL -> song.like_status.setLiked(true)
                                else -> throw IllegalStateException(status.name)
                            }},
                            onLongClick = {
                                SpMp.context.vibrateShort()
                                when (status) {
                                    SongLikeStatus.Status.LIKED -> song.like_status.setLiked(false)
                                    SongLikeStatus.Status.DISLIKED -> song.like_status.setLiked(true)
                                    SongLikeStatus.Status.NEUTRAL -> song.like_status.setLiked(false)
                                    else -> throw IllegalStateException(status.name)
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
