package com.toasterofbread.spmp.ui.layout.nowplaying.overlay

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import com.toasterofbread.spmp.model.mediaitem.song.Song

@Composable
actual fun notifImagePlayerOverlayMenuButtonText(): String? = null

actual class NotifImagePlayerOverlayMenu actual constructor(): PlayerOverlayMenu() {
    @Composable
    override fun Menu(
        getSong: () -> Song?,
        getExpansion: () -> Float,
        openMenu: (PlayerOverlayMenu?) -> Unit,
        getSeekState: () -> Any,
        getCurrentSongThumb: () -> ImageBitmap?,
    ) {
        throw IllegalAccessError()
    }

    override fun closeOnTap(): Boolean {
        throw IllegalAccessError()
    }
}
