package com.toasterofbread.spmp.ui.layout.artistpage

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
import com.toasterofbread.spmp.api.Api
import com.toasterofbread.spmp.api.isOwnChannel
import com.toasterofbread.spmp.model.mediaitem.Artist
import com.toasterofbread.spmp.model.mediaitem.artist.setSubscribed
import com.toasterofbread.spmp.model.mediaitem.loader.ArtistSubscribedLoader
import com.toasterofbread.spmp.resources.getStringTODO
import com.toasterofbread.utils.composable.ShapedIconButton
import com.toasterofbread.utils.getContrasted
import kotlinx.coroutines.launch

@Composable
fun ArtistSubscribeButton(
    artist: Artist,
    modifier: Modifier = Modifier,
    accentColourProvider: (() -> Color)? = null,
    icon_modifier: Modifier = Modifier
) {
    if (!Api.ytm_authenticated) {
        return
    }

    val coroutine_scope = rememberCoroutineScope()

    val subscribed_state = ArtistSubscribedLoader.rememberItemState(artist.id)

    LaunchedEffect(artist.id) {
        if (!artist.isOwnChannel()) {
            coroutine_scope.launch {
                ArtistSubscribedLoader.loadArtistSubscribed(artist, SpMp.context.database)
            }
        }
    }

    Crossfade(subscribed_state.value, modifier) { subscribed ->
        if (subscribed != null) {
            ShapedIconButton(
                {
                    coroutine_scope.launch {
                        val result = artist.setSubscribed(!subscribed)
                        if (result.isFailure) {
                            SpMp.context.sendToast(getStringTODO(
                                if (!subscribed) "Subscribing to ${artist.title} failed"
                                else "Unsubscribing from ${artist.title} failed"
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
