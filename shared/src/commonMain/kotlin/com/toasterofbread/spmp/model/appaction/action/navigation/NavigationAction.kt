package com.toasterofbread.spmp.model.appaction.action.navigation

import com.toasterofbread.spmp.service.playercontroller.PlayerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.appaction_navigation_toggle_player
import spmp.shared.generated.resources.appaction_navigation_jump_to_lyrics

@Serializable
sealed interface NavigationAction {
    fun getType(): Type
    fun getIcon(): ImageVector
    suspend fun execute(player: PlayerState)

    @Composable
    fun Preview(modifier: Modifier) {
        Row(
            modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val type: Type = getType()
            Icon(type.getIcon(), null)
            Text(type.getName(), softWrap = false)
        }
    }

    @Composable
    fun ConfigurationItems(item_modifier: Modifier, onModification: (NavigationAction) -> Unit) {}

    enum class Type {
        APP_PAGE,
        TOGGLE_PLAYER,
        JUMP_TO_LYRICS;

        companion object {
            val DEFAULT: Type = TOGGLE_PLAYER
        }

        @Composable
        fun Preview(modifier: Modifier = Modifier) {
            Row(
                modifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(getIcon(), null)
                Text(getName(), softWrap = false)
            }
        }

        @Composable
        fun getName(): String =
            when (this) {
                APP_PAGE -> throw IllegalStateException()
                TOGGLE_PLAYER -> stringResource(Res.string.appaction_navigation_toggle_player)
                JUMP_TO_LYRICS -> stringResource(Res.string.appaction_navigation_jump_to_lyrics)
            }

        fun getIcon(): ImageVector =
            when (this) {
                APP_PAGE -> Icons.Default.OpenInBrowser
                TOGGLE_PLAYER -> Icons.Default.SwapVert
                JUMP_TO_LYRICS -> Icons.Default.MusicNote
            }

        fun createAction(): NavigationAction =
            when (this) {
                APP_PAGE -> AppPageNavigationAction()
                TOGGLE_PLAYER -> TogglePlayerNavigationAction()
                JUMP_TO_LYRICS -> JumpToLyricsNavigationAction()
            }
    }
}
