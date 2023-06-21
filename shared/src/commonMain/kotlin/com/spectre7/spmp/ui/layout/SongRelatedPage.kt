package com.spectre7.spmp.ui.layout

@Composable
fun SongRelatedPage(
    pill_menu: PillMenu,
    song: Song,
    modifier: Modifier = Modifier,
    previous_item: MediaItem? = null,
    bottom_padding: Dp = 0.dp,
    close: () -> Unit
) {
    var related_result: Result<List<RelatedGroup<MediaItem>>>? by remember { mutableStateOf(null) }
    LaunchedEffect(song) {
        related_result = null
        related_result = getMediaItemRelated(song)
    }

    Crossfade(related_result, modifier) { result ->
        if (result == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                SubtleLoadingIndicator()
            }
            return@Crossfade
        }

        val related: List<RelatedGroup<MediaItem>> = result.getOrNull()
        if (related == null) {
            Box(Modifier.fillMaxSize(), contentAlignment.Center) {
                ErrorInfoDisplay(result.exceptionOrNull()!!)
            }
        }
        else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(related) { group ->
                    Column {
                        Text(group.title)
                        MediaItemGrid(group.contents)
                    }
                }
            }
        }
    }
}
