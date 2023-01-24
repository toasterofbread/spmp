package com.spectre7.spmp.ui.layout.nowplaying.overlay

import androidx.compose.runtime.Composable
import com.spectre7.spmp.model.Song

abstract class OverlayMenu {
    @Composable
    abstract fun Menu(
        song: Song,
        expansion: Float,
        openShutterMenu: (@Composable () -> Unit) -> Unit,
        close: () -> Unit,
        seek_state: Any
    )

    abstract fun closeOnTap(): Boolean
}