package com.spectre7.spmp.ui.layout.nowplaying.overlay

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import com.spectre7.spmp.model.mediaitem.Song
import com.spectre7.spmp.ui.component.PillMenu
import com.spectre7.spmp.ui.layout.SongRelatedPage
import com.spectre7.spmp.ui.theme.Theme

class RelatedContentOverlayMenu : OverlayMenu() {
    override fun closeOnTap(): Boolean = false

    @Composable
    override fun Menu(
        songProvider: () -> Song,
        expansion: Float,
        close: () -> Unit,
        getSeekState: () -> Any,
        getCurrentSongThumb: () -> ImageBitmap?
    ) {
        val pill_menu = remember { PillMenu(
            _background_colour = Theme.current.accent_provider
        ) }

        Box(contentAlignment = Alignment.BottomEnd) {
            SongRelatedPage(
                pill_menu,
                songProvider(),
                Modifier.fillMaxSize().padding(10.dp),
                title_text_style = MaterialTheme.typography.headlineSmall,
                description_text_style = MaterialTheme.typography.bodyMedium,
                close = close
            )

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
