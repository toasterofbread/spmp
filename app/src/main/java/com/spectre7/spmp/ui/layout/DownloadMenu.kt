package com.spectre7.spmp.ui.layout

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.spectre7.spmp.model.Song

@Composable
fun DownloadMenu(song: Song, on_close_request: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Text("Download")
    }
}