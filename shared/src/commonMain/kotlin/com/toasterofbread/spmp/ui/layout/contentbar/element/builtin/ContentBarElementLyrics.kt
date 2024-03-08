package com.toasterofbread.spmp.ui.layout.contentbar.element

import LocalPlayerState
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ContentBarElementLyrics(modifier: Modifier = Modifier) {
    LocalPlayerState.current.controller?.Visualiser(
        LocalContentColor.current,
        modifier,
        opacity = 0.5f
    )
}
