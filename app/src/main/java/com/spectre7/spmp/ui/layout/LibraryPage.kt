package com.spectre7.spmp.ui.layout

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.spectre7.spmp.PlayerServiceHost
import com.spectre7.spmp.ui.component.PillMenu
import com.spectre7.utils.Theme

@Composable
fun LibraryPage(
    pill_menu: PillMenu,
    playerProvider: () -> PlayerViewContext,
    close: () -> Unit
) {
    LazyColumn {
        item {
            Text("Downloaded songs", style = MaterialTheme.typography.titleMedium)
        }

        items(PlayerServiceHost.download_manager.downloaded_songs) { song ->
            song.PreviewLong(Theme.current.on_background_provider, playerProvider, true, Modifier)
        }
    }
}
