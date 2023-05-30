package com.spectre7.spmp.ui.component

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
import com.spectre7.spmp.api.DataApi
import com.spectre7.spmp.model.mediaitem.Song
import com.spectre7.spmp.platform.vibrateShort
import com.spectre7.utils.composable.SubtleLoadingIndicator

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LikeDislikeButton(
    song: Song,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colourProvider: () -> Color
) {
    if (!DataApi.ytm_authenticated) {
        return
    }

    LaunchedEffect(song) {
        if (song.like_status.status == Song.LikeStatus.UNKNOWN) {
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
                check(status == Song.LikeStatus.LIKED || status == Song.LikeStatus.DISLIKED || status == Song.LikeStatus.NEUTRAL)

                val rotation by animateFloatAsState(if (status == Song.LikeStatus.DISLIKED) 180f else 0f)

                Icon(
                    if (status != Song.LikeStatus.NEUTRAL) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                    null,
                    Modifier
                        .rotate(rotation)
                        .combinedClickable(
                            enabled = enabled,
                            onClick = { when (status) {
                                Song.LikeStatus.LIKED -> song.like_status.setLiked(null)
                                Song.LikeStatus.DISLIKED -> song.like_status.setLiked(null)
                                Song.LikeStatus.NEUTRAL -> song.like_status.setLiked(true)
                                else -> throw IllegalStateException(status.name)
                            }},
                            onLongClick = {
                                SpMp.context.vibrateShort()
                                when (status) {
                                    Song.LikeStatus.LIKED -> song.like_status.setLiked(false)
                                    Song.LikeStatus.DISLIKED -> song.like_status.setLiked(true)
                                    Song.LikeStatus.NEUTRAL -> song.like_status.setLiked(false)
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
