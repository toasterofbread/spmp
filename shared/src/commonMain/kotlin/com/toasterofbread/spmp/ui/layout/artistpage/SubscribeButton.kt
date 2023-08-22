package com.toasterofbread.spmp.ui.layout.artistpage

import LocalPlayerState
import SpMp
import androidx.compose.animation.Crossfade
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.outlined.PersonAddAlt1
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.toasterofbread.spmp.model.mediaitem.Artist
import com.toasterofbread.spmp.model.mediaitem.artist.setSubscribed
import com.toasterofbread.spmp.model.mediaitem.loader.ArtistSubscribedLoader
import com.toasterofbread.spmp.resources.getStringTODO
import com.toasterofbread.spmp.youtubeapi.YoutubeApi
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.isOwnChannel
import com.toasterofbread.utils.composable.ShapedIconButton
import com.toasterofbread.utils.getContrasted
import kotlinx.coroutines.launch

@Composable
fun ArtistSubscribeButton(
    artist: Artist,
    auth_state: YoutubeApi.UserAuthState,
    modifier: Modifier = Modifier,
    accentColourProvider: (() -> Color)? = null,
    icon_modifier: Modifier = Modifier
) {
    val player = LocalPlayerState.current
    val coroutine_scope = rememberCoroutineScope()

    val subscribed_state = ArtistSubscribedLoader.rememberItemState(artist.id)

    LaunchedEffect(artist.id) {
        if (!artist.isOwnChannel(auth_state.api)) {
            coroutine_scope.launch {
                ArtistSubscribedLoader.loadArtistSubscribed(artist, SpMp.context)
            }
        }
    }

    Crossfade(subscribed_state.value, modifier) { subscribed ->
        if (subscribed != null) {
            ShapedIconButton(
                {
                    coroutine_scope.launch {
                        val result = artist.setSubscribed(!subscribed, auth_state.SetSubscribedToArtist, player.context)
                        if (result.isFailure) {
                            val artist_title: String? = artist.Title.get(SpMp.context.database)
                            SpMp.context.sendToast(getStringTODO(
                                if (!subscribed) "Subscribing to $artist_title failed"
                                else "Unsubscribing from $artist_title failed"
                            ))
                        }
                    }
                },
                icon_modifier,
                colours = IconButtonDefaults.iconButtonColors(
                    containerColor = if (subscribed && accentColourProvider != null) accentColourProvider() else Color.Transparent,
                    contentColor = if (subscribed && accentColourProvider != null) accentColourProvider().getContrasted() else LocalContentColor.current
                )
            ) {
                Icon(if (subscribed) Icons.Filled.PersonRemove else Icons.Outlined.PersonAddAlt1, null)
            }
        }
    }
}
