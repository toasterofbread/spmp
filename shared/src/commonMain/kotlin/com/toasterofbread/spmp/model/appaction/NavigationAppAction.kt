package com.toasterofbread.spmp.model.appaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.model.appaction.action.navigation.AppPageNavigationAction
import com.toasterofbread.spmp.model.appaction.action.navigation.NavigationAction
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.apppage.AppPage
import dev.toastbits.composekit.components.utils.composable.LargeDropdownMenu
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.appaction_config_navigation_action

@Serializable
data class NavigationAppAction(
    val action: NavigationAction = NavigationAction.Type.DEFAULT.createAction()
): AppAction {
    override fun getType(): AppAction.Type = AppAction.Type.NAVIGATION
    override fun getIcon(): ImageVector = action.getIcon()
    override suspend fun executeAction(player: PlayerState) {
        action.execute(player)
    }

    @Composable
    override fun Preview(modifier: Modifier) {
        action.Preview(modifier)
    }

    @Composable
    override fun ConfigurationItems(item_modifier: Modifier, onModification: (AppAction) -> Unit) {
        var show_action_selector: Boolean by remember { mutableStateOf(false) }

        LargeDropdownMenu(
            title = stringResource(Res.string.appaction_config_navigation_action),
            isOpen = show_action_selector,
            onDismissRequest = { show_action_selector = false },
            items = (0 until AppPage.Type.entries.size + NavigationAction.Type.entries.size - 1).toList(),
            selectedItem =
                if (action is AppPageNavigationAction) action.page.ordinal + 1
                else action.getType().ordinal - 1,
            itemContent = {
                if (it < NavigationAction.Type.entries.size - 1) {
                    NavigationAction.Type.entries[it + 1].Preview()
                }
                else {
                    val page: AppPage.Type = AppPage.Type.entries[it + 1 - NavigationAction.Type.entries.size]

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(page.getIcon(), null)
                        Text(page.getName(), softWrap = false)
                    }
                }
            },
            onSelected = { _, item ->
                val action: NavigationAction
                if (item < NavigationAction.Type.entries.size - 1) {
                    action = NavigationAction.Type.entries[item + 1].createAction()
                }
                else {
                    action = AppPageNavigationAction(AppPage.Type.entries[item + 1 - NavigationAction.Type.entries.size])
                }

                onModification(copy(action = action))
                show_action_selector = false
            }
        )

        FlowRow(
            item_modifier,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                stringResource(Res.string.appaction_config_navigation_action),
                Modifier.align(Alignment.CenterVertically),
                softWrap = false
            )

            Button({ show_action_selector = !show_action_selector }) {
                Preview(Modifier)
            }
        }

        action.ConfigurationItems(
            item_modifier,
            onModification = {
                onModification(copy(action = it))
            }
        )
    }
}
