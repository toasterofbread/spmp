package com.spectre7.spmp.ui.layout.artistpage

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
