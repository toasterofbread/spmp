package com.toasterofbread.spmp.ui.layout.artistpage

import LocalPlayerState
import dev.toastbits.ytmkt.model.ApiAuthenticationState
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
import dev.toastbits.composekit.util.getContrasted
import dev.toastbits.composekit.components.utils.composable.ShapedIconButton
import dev.toastbits.composekit.components.utils.composable.SubtleLoadingIndicator
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.artist.updateSubscribed
import com.toasterofbread.spmp.model.mediaitem.loader.ArtistSubscribedLoader
import com.toasterofbread.spmp.resources.getStringTODO
import kotlinx.coroutines.launch

@Composable
fun ArtistSubscribeButton(
    artist: Artist,
    auth_state: ApiAuthenticationState,
    modifier: Modifier = Modifier,
    getAccentColour: (() -> Color)? = null,
    icon_modifier: Modifier = Modifier
) {
    val player = LocalPlayerState.current
    val coroutine_scope = rememberCoroutineScope()

    val subscribed_state = ArtistSubscribedLoader.rememberItemState(artist.id)
    val artist_subscribed: Boolean? by artist.Subscribed.observe(player.database)

    LaunchedEffect(artist.id) {
        assert(!artist.isForItem()) { artist.toString() }

        if (artist.id != auth_state.own_channel_id) {
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
                        val target: Boolean = !subscribed
                        val result: Result<Unit> = artist.updateSubscribed(target, auth_state.SetSubscribedToArtist, player.context)
                        result.onFailure { exception ->
                            RuntimeException("Ignoring failure to set artist ${artist.id} subscribed status to $target", exception).printStackTrace()

                            val artist_title: String? = artist.getActiveTitle(player.database)
                            player.context.sendToast(
                                getStringTODO(
                                    if (!subscribed) "Subscribing to $artist_title failed"
                                    else "Unsubscribing from $artist_title failed"
                                )
                            )
                        }
                    }
                },
                IconButtonDefaults.iconButtonColors(
                    containerColor = if (subscribed && getAccentColour != null) getAccentColour() else Color.Transparent,
                    contentColor = if (subscribed && getAccentColour != null) getAccentColour().getContrasted() else LocalContentColor.current
                ),
                icon_modifier
            ) {
                Icon(if (subscribed) Icons.Filled.PersonRemove else Icons.Outlined.PersonAddAlt1, null)
            }
        }
    }
}
