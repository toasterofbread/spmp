package com.toasterofbread.spmp.ui.component

import LocalPlayerState
import dev.toastbits.ytmkt.model.ApiAuthenticationState
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.toastbits.composekit.context.vibrateShort
import dev.toastbits.composekit.util.platform.launchSingle
import dev.toastbits.composekit.util.thenIf
import dev.toastbits.composekit.components.utils.composable.PlatformClickableIconButton
import dev.toastbits.composekit.components.utils.composable.SubtleLoadingIndicator
import dev.toastbits.composekit.components.utils.modifier.bounceOnClick
import com.toasterofbread.spmp.model.mediaitem.loader.SongLikedLoader
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.song.updateLiked
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.theme.appHover
import com.toasterofbread.spmp.util.getToggleTarget
import dev.toastbits.ytmkt.endpoint.SetSongLikedEndpoint
import dev.toastbits.ytmkt.endpoint.SongLikedEndpoint
import dev.toastbits.ytmkt.model.external.SongLikedStatus
import kotlinx.coroutines.CoroutineScope

@Composable
fun LikeDislikeButton(
    song: Song,
    auth_state: ApiAuthenticationState?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    getEnabled: (() -> Boolean)? = null,
    getColour: (() -> Color)? = null
) {
    val get_liked_endpoint: SongLikedEndpoint? = auth_state?.SongLiked
    val set_liked_endpoint: SetSongLikedEndpoint? = auth_state?.SetSongLiked

    val player: PlayerState = LocalPlayerState.current
    val coroutine_scope: CoroutineScope = rememberCoroutineScope()

    val loading: Boolean = SongLikedLoader.rememberItemState(song.id).loading
    val liked_status: SongLikedStatus? by song.Liked.observe(player.database)
    val rotation: Float by animateFloatAsState(if (liked_status == SongLikedStatus.DISLIKED) 180f else 0f)

    LaunchedEffect(song.id) {
        SongLikedLoader.loadSongLiked(song.id, player.context, get_liked_endpoint).onFailure {
            it.printStackTrace()
        }
    }

    val enabled: Boolean = getEnabled?.invoke() != false

    PlatformClickableIconButton(
        onClick = {
            if (onClick != null) {
                onClick()
                return@PlatformClickableIconButton
            }

            coroutine_scope.launchSingle {
                song.updateLiked(
                    liked_status.getToggleTarget(),
                    set_liked_endpoint,
                    player.context
                )
            }
        },
        onAltClick = {
            if (onClick != null || set_liked_endpoint == null) {
                return@PlatformClickableIconButton
            }

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
        modifier = modifier.thenIf(enabled) {
            bounceOnClick().appHover(true)
        },
        enabled = enabled,
        apply_minimum_size = false,
        indication = null
    ) {
        Crossfade(
            if (auth_state == null) liked_status ?: SongLikedStatus.NEUTRAL else liked_status
        ) { status ->
            if (status != null) {
                LikedStatusIcon(
                    status,
                    Modifier.rotate(rotation),
                    getColour,
                    alt_shape = auth_state == null
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
    getColour: (() -> Color)?,
    alt_shape: Boolean = false
) {
    Icon(
        if (!alt_shape)
            if (status != SongLikedStatus.NEUTRAL) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp
        else
            if (status != SongLikedStatus.NEUTRAL) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
        null,
        modifier,
        tint = getColour?.invoke() ?: LocalContentColor.current
    )
}
