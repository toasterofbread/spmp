package com.spectre7.spmp.ui.layout.nowplaying.overlay

class RelatedContentOverlayMenu(): OverlayMenu() {
    override fun closeOnTap(): Boolean = false

    @Composable
    override fun Menu(
        songProvider: () -> Song,
        expansion: Float,
        openShutterMenu: (@Composable () -> Unit) -> Unit,
        close: () -> Unit,
        getSeekState: () -> Any,
        getCurrentSongThumb: () -> ImageBitmap?
    ) {
        val pill_menu = remember { PillMenu() }

        Box(contentAlignment = Alignment.BottomEnd) {
            SongRelatedPage(pill_menu, songProvider(), Modifier.fillMaxSize(), close = close)

            pill_menu.PillMenu(
                1,
                { index, _ ->
                    when (index) {
                        0 -> 
                            ActionButton(Icons.Filled.Close) {
                                close()
                            }
                        else -> throw NotImplementedException(index.toString())
                    }
                }
            )
        }
    }
}
