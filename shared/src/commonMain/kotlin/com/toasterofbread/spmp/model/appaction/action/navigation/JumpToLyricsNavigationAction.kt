package com.toasterofbread.spmp.model.appaction.action.navigation

import kotlinx.serialization.Serializable
import LocalAppState
import com.toasterofbread.spmp.ui.layout.nowplaying.overlay.PlayerOverlayMenu
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lyrics

@Serializable
class JumpToLyricsNavigationAction: NavigationAction {
    override fun getType(): NavigationAction.Type =
        NavigationAction.Type.JUMP_TO_LYRICS

    override fun getIcon(): ImageVector =
        Icons.Default.Lyrics

    override suspend fun execute(state: SpMp.State) {
        state.player.openNpOverlayMenu(PlayerOverlayMenu.getLyricsMenu())
    }
}
