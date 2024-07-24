package com.toasterofbread.spmp.model.appaction.action.navigation

import kotlinx.serialization.Serializable
import com.toasterofbread.spmp.model.state.OldPlayerStateImpl
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp

@Serializable
class TogglePlayerNavigationAction: NavigationAction {
    override fun getType(): NavigationAction.Type =
        NavigationAction.Type.TOGGLE_PLAYER

    override fun getIcon(): ImageVector =
        Icons.Default.KeyboardArrowUp

    override suspend fun execute(player: OldPlayerStateImpl) {
        player.expansion.toggle()
    }
}
