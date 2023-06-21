package com.spectre7.spmp.ui.layout.nowplaying.overlay

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import com.spectre7.spmp.model.mediaitem.Song
import com.spectre7.spmp.ui.component.PillMenu
import com.spectre7.spmp.ui.layout.SongRelatedPage

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
                        else -> throw NotImplementedError(index.toString())
                    }
                }
            )
        }
    }
}
