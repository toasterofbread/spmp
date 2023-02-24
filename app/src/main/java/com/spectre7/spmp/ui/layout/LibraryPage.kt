package com.spectre7.spmp.ui.layout

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
            song.PreviewLong(MainActivity.theme.getOnBackgroundProvider(true), playerProvider, true, Modifier)
        }
    }
}
