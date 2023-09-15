package com.toasterofbread.spmp.ui.layout.artistpage

import LocalPlayerState
import androidx.compose.animation.Crossfade
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.outlined.PersonAddAlt1
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.artist.updateSubscribed
import com.toasterofbread.spmp.model.mediaitem.loader.ArtistSubscribedLoader
import com.toasterofbread.spmp.resources.getStringTODO
import com.toasterofbread.spmp.youtubeapi.YoutubeApi
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.isOwnChannel
import com.toasterofbread.utils.common.getContrasted
import com.toasterofbread.utils.composable.ShapedIconButton
import com.toasterofbread.utils.composable.SubtleLoadingIndicator
import kotlinx.coroutines.launch

@Composable
fun ArtistSubscribeButton(
    artist: Artist,
    auth_state: YoutubeApi.UserAuthState,
    modifier: Modifier = Modifier,
    getAccentColour: (() -> Color)? = null,
    icon_modifier: Modifier = Modifier
) {
    val player = LocalPlayerState.current
    val coroutine_scope = rememberCoroutineScope()

    val subscribed_state = ArtistSubscribedLoader.rememberItemState(artist.id)
    val artist_subscribed: Boolean? by artist.Subscribed.observe(player.database)

    LaunchedEffect(artist.id) {
        if (!artist.isOwnChannel(auth_state.api)) {
            coroutine_scope.launch {
                ArtistSubscribedLoader.loadArtistSubscribed(artist, player.context)
            }
        }
    }

    Crossfade(if (subscribed_state.loading) Unit else artist_subscribed, modifier) { subscribed ->
        if (subscribed == Unit) {
            SubtleLoadingIndicator()
        }
        else if (subscribed is Boolean) {
            ShapedIconButton(
                {
                    coroutine_scope.launch {
                        val result = artist.updateSubscribed(!subscribed, auth_state.SetSubscribedToArtist, player.context)
                        if (result.isFailure) {
                            val artist_title: String? = artist.getActiveTitle(player.database)
                            player.context.sendToast(getStringTODO(
                                if (!subscribed) "Subscribing to $artist_title failed"
                                else "Unsubscribing from $artist_title failed"
                            ))
                        }
                    }
                },
                icon_modifier,
                colours = IconButtonDefaults.iconButtonColors(
                    containerColor = if (subscribed && getAccentColour != null) getAccentColour() else Color.Transparent,
                    contentColor = if (subscribed && getAccentColour != null) getAccentColour().getContrasted() else LocalContentColor.current
                )
            ) {
                Icon(if (subscribed) Icons.Filled.PersonRemove else Icons.Outlined.PersonAddAlt1, null)
            }
        }
    }
}
