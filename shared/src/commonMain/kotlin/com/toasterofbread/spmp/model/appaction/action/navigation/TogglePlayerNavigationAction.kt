package com.toasterofbread.spmp.model.appaction.action.navigation

import kotlinx.serialization.Serializable
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Serializable
class TogglePlayerNavigationAction: NavigationAction {
    override fun getType(): NavigationAction.Type =
        NavigationAction.Type.TOGGLE_PLAYER

    override suspend fun execute(player: PlayerState) {
        player.expansion.toggle()
    }
}
