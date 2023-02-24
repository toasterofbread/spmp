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
    }
}
