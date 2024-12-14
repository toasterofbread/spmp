package com.toasterofbread.spmp.model.appaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.toasterofbread.spmp.model.appaction.action.playback.PlaybackAction
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import dev.toastbits.composekit.components.utils.composable.LargeDropdownMenu
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.appaction_config_playback_action_type

@Serializable
data class PlaybackAppAction(
    val action: PlaybackAction = PlaybackAction.Type.DEFAULT.createAction()
): AppAction {
    override fun getType(): AppAction.Type = AppAction.Type.PLAYBACK
    override fun getIcon(): ImageVector = action.getType().getIcon()
    override suspend fun executeAction(player: PlayerState) {
        action.execute(player)
    }

    @Composable
    override fun Preview(modifier: Modifier) {
        action.getType().Preview(modifier)
    }

    @Composable
    override fun ConfigurationItems(item_modifier: Modifier, onModification: (AppAction) -> Unit) {
        var show_action_selector: Boolean by remember { mutableStateOf(false) }

        LargeDropdownMenu(
            title = stringResource(Res.string.appaction_config_playback_action_type),
            isOpen = show_action_selector,
            onDismissRequest = { show_action_selector = false },
            items = PlaybackAction.Type.entries,
            selectedItem = action.getType(),
            itemContent = { action ->
                action.Preview()
            },
            onSelected = { _, action ->
                onModification(copy(action = action.createAction()))
                show_action_selector = false
            }
        )

        FlowRow(
            item_modifier,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                stringResource(Res.string.appaction_config_playback_action_type),
                Modifier.align(Alignment.CenterVertically),
                softWrap = false
            )

            Button({ show_action_selector = !show_action_selector }) {
                Preview(Modifier)
            }
        }

        action.ConfigurationItems(item_modifier) {
            onModification(copy(action = it))
        }
    }
}
