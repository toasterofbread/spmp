package com.toasterofbread.spmp.model.appaction

import kotlinx.serialization.Serializable
import com.toasterofbread.spmp.ui.layout.apppage.AppPage
import LocalAppState
import com.toasterofbread.spmp.model.appaction.AppAction
import com.toasterofbread.spmp.model.appaction.action.navigation.*
import dev.toastbits.composekit.utils.composable.LargeDropdownMenu
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import androidx.compose.animation.AnimatedVisibility
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.appaction_config_navigation_action

@Serializable
data class NavigationAppAction(
    val action: NavigationAction = NavigationAction.Type.DEFAULT.createAction()
): AppAction {
    override fun getType(): AppAction.Type = AppAction.Type.NAVIGATION
    override fun getIcon(): ImageVector = action.getIcon()
    override suspend fun executeAction(state: SpMp.State) {
        action.execute(state)
    }

    @Composable
    override fun Preview(modifier: Modifier) {
        action.Preview(modifier)
    }

    @Composable
    override fun ConfigurationItems(item_modifier: Modifier, onModification: (AppAction) -> Unit) {
        var show_action_selector: Boolean by remember { mutableStateOf(false) }

        LargeDropdownMenu(
            expanded = show_action_selector,
            onDismissRequest = { show_action_selector = false },
            item_count = AppPage.Type.entries.size + NavigationAction.Type.entries.size - 1,
            selected =
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
            onSelected = {
                val action: NavigationAction
                if (it < NavigationAction.Type.entries.size - 1) {
                    action = NavigationAction.Type.entries[it + 1].createAction()
                }
                else {
                    action = AppPageNavigationAction(AppPage.Type.entries[it + 1 - NavigationAction.Type.entries.size])
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
