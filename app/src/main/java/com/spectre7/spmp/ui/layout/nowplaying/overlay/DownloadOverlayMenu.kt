package com.spectre7.spmp.ui.layout.nowplaying.overlay

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.spectre7.spmp.model.Song

class DownloadOverlayMenu(): OverlayMenu() {

    fun closeOnTap(): Boolean = false
    
    @Composable
    fun Menu(song: Song, seek_state: Any, openShutterMenu: (@Composable () -> Unit) -> Unit, close: () -> Unit) {
        Column(Modifier.fillMaxSize()) {
            Text("Download")
        }
    }
}
