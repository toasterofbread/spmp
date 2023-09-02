package com.toasterofbread.spmp.ui.layout.nowplaying.overlay

import LocalPlayerState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.ui.component.PillMenu
import com.toasterofbread.spmp.ui.layout.SongRelatedPage
import com.toasterofbread.spmp.ui.layout.nowplaying.getNPBackground
import com.toasterofbread.spmp.youtubeapi.SongRelatedContentEndpoint

class RelatedContentOverlayMenu(
    private val related_endpoint: SongRelatedContentEndpoint
) : OverlayMenu() {
    override fun closeOnTap(): Boolean = false

    @Composable
    override fun Menu(
        getSong: () -> Song,
        getExpansion: () -> Float,
        openMenu: (OverlayMenu?) -> Unit,
        getSeekState: () -> Any,
        getCurrentSongThumb: () -> ImageBitmap?
    ) {
        val player = LocalPlayerState.current

        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
            SongRelatedPage(
                getSong(),
                related_endpoint,
                Modifier.fillMaxSize(),
                title_text_style = MaterialTheme.typography.headlineSmall,
                description_text_style = MaterialTheme.typography.bodyMedium,
                close = { openMenu(null) },
                padding = PaddingValues(10.dp),
                accent_colour = player.getNPBackground()
            )

            val pill_menu = remember { PillMenu(
                _background_colour = { player.getNPBackground() }
            ) }
            pill_menu.PillMenu(
                1,
                { index, _ ->
                    when (index) {
                        0 -> 
                            ActionButton(Icons.Filled.Close) {
                                openMenu(null)
                            }
                        else -> throw NotImplementedError(index.toString())
                    }
                }
            )
        }
    }
}
