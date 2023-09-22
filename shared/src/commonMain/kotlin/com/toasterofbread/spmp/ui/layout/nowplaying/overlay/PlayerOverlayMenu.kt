package com.toasterofbread.spmp.ui.layout.nowplaying.overlay

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.nowplaying.overlay.lyrics.LyricsPlayerOverlayMenu

enum class PlayerOverlayMenuAction {
    OPEN_MAIN_MENU,
    OPEN_THEMING,
    PICK_THEME_COLOUR,
    ADJUST_NOTIFICATION_IMAGE_OFFSET,
    OPEN_LYRICS,
    OPEN_RELATED,
    DOWNLOAD;

    fun getReadable(): String =
        when(this) {
            OPEN_MAIN_MENU -> getString("player_overlay_menu_action_open_main_menu")
            OPEN_THEMING -> getString("player_overlay_menu_action_open_theming")
            PICK_THEME_COLOUR -> getString("player_overlay_menu_action_pick_theme_colour")
            ADJUST_NOTIFICATION_IMAGE_OFFSET -> getString("player_overlay_menu_action_adjust_notification_image_offset")
            OPEN_LYRICS -> getString("player_overlay_menu_action_open_lyrics")
            OPEN_RELATED -> getString("player_overlay_menu_action_open_related")
            DOWNLOAD -> getString("player_overlay_menu_action_download")
        }

    companion object {
        val DEFAULT: PlayerOverlayMenuAction = OPEN_MAIN_MENU
        val DEFAULT_CUSTOM: PlayerOverlayMenuAction = OPEN_LYRICS
    }
}

abstract class PlayerOverlayMenu {
    @Composable
    abstract fun Menu(
        getSong: () -> Song,
        getExpansion: () -> Float,
        openMenu: (PlayerOverlayMenu?) -> Unit,
        getSeekState: () -> Any,
        getCurrentSongThumb: () -> ImageBitmap?
    )

    abstract fun closeOnTap(): Boolean

    companion object {
        fun getLyricsMenu(): PlayerOverlayMenu = LyricsPlayerOverlayMenu()
    }
}
