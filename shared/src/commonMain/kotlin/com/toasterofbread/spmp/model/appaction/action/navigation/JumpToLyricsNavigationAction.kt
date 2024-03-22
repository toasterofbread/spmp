package com.toasterofbread.spmp.model.appaction.action.navigation

import kotlinx.serialization.Serializable
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.nowplaying.overlay.PlayerOverlayMenu
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Serializable
class JumpToLyricsNavigationAction: NavigationAction {
    override fun getType(): NavigationAction.Type =
        NavigationAction.Type.JUMP_TO_LYRICS

    override suspend fun execute(player: PlayerState) {
        player.openNowPlayingPlayerOverlayMenu(PlayerOverlayMenu.getLyricsMenu())
    }
}
