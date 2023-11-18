package com.toasterofbread.spmp.ui.component

import LocalPlayerState
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.toasterofbread.composekit.platform.vibrateShort
import com.toasterofbread.composekit.utils.common.launchSingle
import com.toasterofbread.composekit.utils.composable.PlatformClickableIconButton
import com.toasterofbread.composekit.utils.composable.SubtleLoadingIndicator
import com.toasterofbread.composekit.utils.modifier.bounceOnClick
import com.toasterofbread.spmp.model.mediaitem.loader.SongLikedLoader
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.song.SongLikedStatus
import com.toasterofbread.spmp.model.mediaitem.song.updateLiked
import com.toasterofbread.spmp.youtubeapi.YoutubeApi
import com.toasterofbread.spmp.youtubeapi.endpoint.SetSongLikedEndpoint
import com.toasterofbread.spmp.youtubeapi.endpoint.SongLikedEndpoint

@Composable
fun LikeDislikeButton(
    song: Song,
    auth_state: YoutubeApi.UserAuthState,
    modifier: Modifier = Modifier,
    getEnabled: (() -> Boolean)? = null,
    getColour: () -> Color
) {
    val get_liked_endpoint: SongLikedEndpoint = auth_state.SongLiked
    val set_liked_endpoint: SetSongLikedEndpoint = auth_state.SetSongLiked
    check(get_liked_endpoint.isImplemented())
    check(set_liked_endpoint.isImplemented())

    val player = LocalPlayerState.current
    val coroutine_scope = rememberCoroutineScope()

    val loading: Boolean = SongLikedLoader.rememberItemState(song.id).loading
    val liked_status: SongLikedStatus? by song.Liked.observe(player.database)
    val rotation: Float by animateFloatAsState(if (liked_status == SongLikedStatus.DISLIKED) 180f else 0f)

    LaunchedEffect(song.id) {
        SongLikedLoader.loadSongLiked(song.id, player.context, get_liked_endpoint)
    }

    PlatformClickableIconButton(
        onClick = {
            coroutine_scope.launchSingle {
                song.updateLiked(
                    when (liked_status) {
                        SongLikedStatus.LIKED, SongLikedStatus.DISLIKED -> SongLikedStatus.NEUTRAL
                        SongLikedStatus.NEUTRAL, null -> SongLikedStatus.LIKED
                    },
                    set_liked_endpoint,
                    player.context
                )
            }
        },
        onAltClick = {
            player.context.vibrateShort()
            coroutine_scope.launchSingle {
                song.updateLiked(
                    when (liked_status) {
                        SongLikedStatus.LIKED, SongLikedStatus.NEUTRAL, null -> SongLikedStatus.DISLIKED
                        SongLikedStatus.DISLIKED -> SongLikedStatus.LIKED
                    },
                    set_liked_endpoint,
                    player.context
                )
            }
        },
        modifier.bounceOnClick(),
        apply_minimum_size = false
    ) {
        Crossfade(liked_status) { status ->
            if (status != null) {
                LikedStatusIcon(
                    status,
                    Modifier.rotate(rotation),
                    getColour
                )
            }
            else if (loading) {
                SubtleLoadingIndicator(Modifier.size(24.dp), getColour = getColour)
            }
        }
    }
}

@Composable
private fun LikedStatusIcon(
    status: SongLikedStatus,
    modifier: Modifier = Modifier,
    getColour: () -> Color
) {
    Icon(
        if (status != SongLikedStatus.NEUTRAL) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
        null,
        modifier,
        tint = getColour()
    )
}
