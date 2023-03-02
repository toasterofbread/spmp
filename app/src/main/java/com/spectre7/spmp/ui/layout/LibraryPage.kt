package com.spectre7.spmp.ui.layout

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.spectre7.spmp.PlayerServiceHost
import com.spectre7.spmp.ui.component.PillMenu
import com.spectre7.spmp.ui.theme.Theme

@Composable
fun LibraryPage(
    pill_menu: PillMenu,
    playerProvider: () -> PlayerViewContext,
    close: () -> Unit
) {
    val download_manager = PlayerServiceHost.download_manager
    download_manager.download_state

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
                    "Library",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        color = settings_interface.theme.on_background
                    )
                )

                Text(
                    current_page.getReadable(true),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        color = settings_interface.theme.on_background
                    )
                )
            }
        }

        download_manager.iterateDownloadedFiles { file, data ->
            val song = Song.fromId(data.id)

            item(data) {
                song.PreviewLong(Theme.current.on_background_provider, playerProvider, true, Modifier)
            }
        }
    }
}
