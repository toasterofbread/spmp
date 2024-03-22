package com.toasterofbread.spmp.model.appaction

import kotlinx.serialization.Serializable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.model.appaction.action.playback.*
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.composekit.utils.composable.LargeDropdownMenu

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

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    override fun ConfigurationItems(item_modifier: Modifier, onModification: (AppAction) -> Unit) {
        var show_action_selector: Boolean by remember { mutableStateOf(false) }

        LargeDropdownMenu(
            expanded = show_action_selector,
            onDismissRequest = { show_action_selector = false },
            item_count = PlaybackAction.Type.entries.size,
            selected = action.getType().ordinal,
            itemContent = {
                PlaybackAction.Type.entries[it].Preview()
            },
            onSelected = {
                onModification(copy(action = PlaybackAction.Type.entries[it].createAction()))
                show_action_selector = false
            }
        )

        FlowRow(
            item_modifier,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                getString("appaction_config_playback_action_type"),
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
