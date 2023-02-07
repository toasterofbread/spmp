package com.spectre7.spmp.ui.layout.nowplaying.overlay

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.spectre7.spmp.model.Song
import com.spectre7.spmp.ui.layout.PlayerViewContext

class DownloadOverlayMenu: OverlayMenu() {

    override fun closeOnTap(): Boolean = false
    
    @Composable
    override fun Menu(
        song: Song,
        expansion: Float,
        openShutterMenu: (@Composable () -> Unit) -> Unit,
        close: () -> Unit,
        seek_state: Any,
        playerProvider: () -> PlayerViewContext
    ) {
        Column(Modifier.fillMaxSize()) {
            Text("Download")
        }
    }
}
