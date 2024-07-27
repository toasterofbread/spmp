package com.toasterofbread.spmp.ui.layout.nowplaying.overlay

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.ui.layout.nowplaying.overlay.lyrics.LyricsPlayerOverlayMenu
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.player_overlay_menu_action_open_main_menu
import spmp.shared.generated.resources.player_overlay_menu_action_open_theming
import spmp.shared.generated.resources.player_overlay_menu_action_pick_theme_colour
import spmp.shared.generated.resources.player_overlay_menu_action_adjust_notification_image_offset
import spmp.shared.generated.resources.player_overlay_menu_action_open_lyrics
import spmp.shared.generated.resources.player_overlay_menu_action_open_related
import spmp.shared.generated.resources.player_overlay_menu_action_download

enum class PlayerOverlayMenuAction {
    OPEN_MAIN_MENU,
    OPEN_THEMING,
    PICK_THEME_COLOUR,
    ADJUST_NOTIFICATION_IMAGE_OFFSET,
    OPEN_LYRICS,
    OPEN_RELATED,
    DOWNLOAD;

    @Composable
    fun getReadable(): String =
        when(this) {
            OPEN_MAIN_MENU -> stringResource(Res.string.player_overlay_menu_action_open_main_menu)
            OPEN_THEMING -> stringResource(Res.string.player_overlay_menu_action_open_theming)
            PICK_THEME_COLOUR -> stringResource(Res.string.player_overlay_menu_action_pick_theme_colour)
            ADJUST_NOTIFICATION_IMAGE_OFFSET -> stringResource(Res.string.player_overlay_menu_action_adjust_notification_image_offset)
            OPEN_LYRICS -> stringResource(Res.string.player_overlay_menu_action_open_lyrics)
            OPEN_RELATED -> stringResource(Res.string.player_overlay_menu_action_open_related)
            DOWNLOAD -> stringResource(Res.string.player_overlay_menu_action_download)
        }

    companion object {
        val DEFAULT: PlayerOverlayMenuAction = OPEN_MAIN_MENU
        val DEFAULT_CUSTOM: PlayerOverlayMenuAction = OPEN_LYRICS
    }
}

abstract class PlayerOverlayMenu {
    @Composable
    abstract fun Menu(
        getSong: () -> Song?,
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
