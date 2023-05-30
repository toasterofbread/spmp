package com.spectre7.spmp.ui.layout.nowplaying.overlay

import androidx.compose.runtime.Composable
import com.spectre7.spmp.model.mediaitem.Song

abstract class OverlayMenu {
    @Composable
    abstract fun Menu(
        songProvider: () -> Song,
        expansion: Float,
        openShutterMenu: (@Composable () -> Unit) -> Unit,
        close: () -> Unit,
        getSeekState: () -> Any
    )

    abstract fun closeOnTap(): Boolean
}