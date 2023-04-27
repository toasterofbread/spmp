package com.spectre7.spmp.ui.layout

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.spectre7.spmp.PlayerServiceHost
import com.spectre7.spmp.model.MediaItem
import com.spectre7.spmp.model.Song
import com.spectre7.spmp.ui.component.PillMenu
import com.spectre7.spmp.ui.theme.Theme
import com.spectre7.utils.getStringTemp

@Composable
fun LibraryPage(
    pill_menu: PillMenu,
    playerProvider: () -> PlayerViewContext,
    close: () -> Unit
) {

    val downloads: MutableList<PlayerDownloadManager.DownloadStatus> by remember { mutableStateListOf() }
    DisposableEffect(Unit) {
        val listener = object : PlayerDownloadManager.DownloadStatusListener {
            override fun onDownloadAdded(status: DownloadStatus) {
                downloads.add(status)
            }
            override fun onDownloadRemoved(id: String) {
                downloads.removeIf { it.id == id }
            }
            override fun onDownloadChanged(status: DownloadStatus) {
                for (i in downloads.indices) [
                    if (downloads[i].id == status.id) [
                        downloads[i] = status
                    ]
                ]
            }
        }
        PlayerServiceHost.download_manager.addDownloadStatusListener(listener)

        onDispose {
            PlayerServiceHost.download_manager.removeDownloadStatusListener(listener)
        }
    }

    var current_page: MediaItem.Type by remember { mutableStateOf(MediaItem.Type.SONG) }

    LaunchedEffect(Unit) {
        pill_menu.addAlongsideAction {
            for (type in MediaItem.Type.values()) {
                ActionButton(type.getIcon()) { current_page = type }
            }

            ActionButton(Icons.Filled.Download) { TODO("Open download menu") }
        }
    }

    LazyColumn(Modifier.padding(10.dp)) {
        item {
            Column {
                Text(
                    getStringTemp("Library"),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        color = Theme.current.on_background
                    )
                )

                Text(
                    current_page.getReadable(true),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        color = Theme.current.on_background
                    )
                )
            }
        }

        items(downloads) { download ->
            download.song.PreviewLong(MediaItem.PreviewParams(playerProvider))
        }
    }
}
