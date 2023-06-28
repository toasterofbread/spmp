package com.spectre7.spmp.ui.layout.artistpage

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
import com.spectre7.spmp.api.Api
import com.spectre7.spmp.model.mediaitem.Artist
import com.spectre7.spmp.resources.getStringTODO
import com.spectre7.utils.composable.ShapedIconButton
import com.spectre7.utils.getContrasted
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

    LaunchedEffect(artist, artist.is_own_channel) {
        if (!artist.is_own_channel) {
            coroutine_scope.launch {
                artist.updateSubscribed()
            }
        }
    }

    Crossfade(artist.subscribed, modifier) { subscribed ->
        if (subscribed != null) {
            ShapedIconButton(
                {
                    coroutine_scope.launch {
                        val result = artist.toggleSubscribe(false)
                        if (result.isFailure) {
                            SpMp.context.sendToast(getStringTODO(
                                if (artist.subscribed != true) "Subscribing to ${artist.title} failed"
                                else "Unsubscribing from ${artist.title} failed"
                            ))
                        }
                    }
                },
                icon_modifier,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (subscribed && accentColourProvider != null) accentColourProvider() else Color.Transparent,
                    contentColor = if (subscribed && accentColourProvider != null) accentColourProvider().getContrasted() else LocalContentColor.current
                )
            ) {
                Icon(if (subscribed) Icons.Filled.PersonRemove else Icons.Outlined.PersonAddAlt1, null)
            }
        }
    }
}
