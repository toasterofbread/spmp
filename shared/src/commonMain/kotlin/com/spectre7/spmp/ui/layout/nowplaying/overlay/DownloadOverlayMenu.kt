package com.spectre7.spmp.ui.layout.nowplaying.overlay

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.spectre7.spmp.model.mediaitem.Song

class DownloadOverlayMenu: OverlayMenu() {

    override fun closeOnTap(): Boolean = false

    @Composable
    override fun Menu(
        songProvider: () -> Song,
        expansion: Float,
        openShutterMenu: (@Composable () -> Unit) -> Unit,
        close: () -> Unit,
        getSeekState: () -> Any
    ) {
        Column(Modifier.fillMaxSize()) {
            Text("Download")
        }
    }
}
