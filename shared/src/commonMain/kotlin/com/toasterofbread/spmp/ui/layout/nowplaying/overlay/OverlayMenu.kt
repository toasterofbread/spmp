package com.toasterofbread.spmp.ui.layout.nowplaying.overlay

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import com.toasterofbread.spmp.model.mediaitem.Song
import com.toasterofbread.spmp.ui.layout.nowplaying.overlay.lyrics.LyricsOverlayMenu

abstract class OverlayMenu {
    @Composable
    abstract fun Menu(
        getSong: () -> Song,
        getExpansion: () -> Float,
        openMenu: (OverlayMenu?) -> Unit,
        getSeekState: () -> Any,
        getCurrentSongThumb: () -> ImageBitmap?
    )

    abstract fun closeOnTap(): Boolean

    companion object {
        fun getLyricsMenu(): OverlayMenu = LyricsOverlayMenu()
    }
}
