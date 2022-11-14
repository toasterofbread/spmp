package com.spectre7.spmp.ui.layout.nowplaying.overlay

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.spectre7.spmp.model.Song

@Composable
fun DownloadMenu(song: Song, close: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Text("Download")
    }
}