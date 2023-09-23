package com.toasterofbread.spmp.ui.component

import LocalPlayerState
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
import com.toasterofbread.spmp.model.mediaitem.loader.SongLikedLoader
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.song.SongLikedStatus
import com.toasterofbread.spmp.model.mediaitem.song.updateLiked
import com.toasterofbread.spmp.platform.vibrateShort
import com.toasterofbread.spmp.youtubeapi.YoutubeApi
import com.toasterofbread.utils.common.launchSingle
import com.toasterofbread.utils.composable.SubtleLoadingIndicator

@Composable
fun LikeDislikeButton(
    song: Song,
    auth_state: YoutubeApi.UserAuthState,
    modifier: Modifier = Modifier,
    getEnabled: (() -> Boolean)? = null,
    getColour: () -> Color
) {
    val get_liked_endpoint = auth_state.SongLiked
    val set_liked_endpoint = auth_state.SetSongLiked
    check(get_liked_endpoint.isImplemented())
    check(set_liked_endpoint.isImplemented())

    val context = LocalPlayerState.current.context
    val coroutine_scope = rememberCoroutineScope()

    val loading: Boolean = SongLikedLoader.rememberItemState(song.id).loading
    val liked_status: SongLikedStatus? by song.Liked.observe(context.database)
    val rotation: Float by animateFloatAsState(if (liked_status == SongLikedStatus.DISLIKED) 180f else 0f)

    LaunchedEffect(song.id) {
        SongLikedLoader.loadSongLiked(song.id, context, get_liked_endpoint)
    }

    Box(
        modifier,
        contentAlignment = Alignment.Center
    ) {
        Crossfade(liked_status) { status ->
            if (status != null) {
                LikedStatusIcon(
                    status,
                    Modifier.rotate(rotation),
                    getEnabled,
                    getColour
                ) { new_status ->
                    coroutine_scope.launchSingle {
                        song.updateLiked(new_status, set_liked_endpoint, context)
                    }
                }
            }
            else if (loading) {
                SubtleLoadingIndicator(Modifier.size(24.dp), getColour = getColour)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LikedStatusIcon(
    status: SongLikedStatus,
    modifier: Modifier = Modifier,
    getEnabled: (() -> Boolean)?,
    getColour: () -> Color,
    setStatus: (SongLikedStatus) -> Unit
) {
    val player = LocalPlayerState.current

    Icon(
        if (status != SongLikedStatus.NEUTRAL) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
        null,
        modifier
            .combinedClickable(
                remember { MutableInteractionSource() },
                rememberRipple(false),
                enabled = getEnabled?.invoke() != false,

                onClick = {
                    when (status) {
                        SongLikedStatus.LIKED, SongLikedStatus.DISLIKED -> setStatus(SongLikedStatus.NEUTRAL)
                        SongLikedStatus.NEUTRAL -> setStatus(SongLikedStatus.LIKED)
                    }
                },
                onLongClick = {
                    player.context.vibrateShort()
                    when (status) {
                        SongLikedStatus.LIKED, SongLikedStatus.NEUTRAL -> setStatus(SongLikedStatus.DISLIKED)
                        SongLikedStatus.DISLIKED -> setStatus(SongLikedStatus.LIKED)
                    }
                }
            ),
        tint = getColour()
    )
}
