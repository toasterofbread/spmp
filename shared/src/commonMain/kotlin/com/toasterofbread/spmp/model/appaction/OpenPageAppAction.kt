package com.toasterofbread.spmp.model.appaction

import kotlinx.serialization.Serializable
import com.toasterofbread.spmp.ui.layout.apppage.AppPage
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.composekit.utils.composable.LargeDropdownMenu
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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

@Serializable
data class OpenPageAppAction(
    val page: AppPage.Type = AppPage.Type.DEFAULT,
    val settings_category: String? = null
): AppAction {
    override fun getType(): AppAction.Type = AppAction.Type.OPEN_PAGE
    override suspend fun executeAction(player: PlayerState) {
        val page: AppPage = page.getPage(player, player.app_page_state) ?: return
        player.openAppPage(page)
    }

    @Composable
    override fun Preview(modifier: Modifier) {
        Row(
            modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(page.getIcon(), null)
            Text(page.getName(), softWrap = false)
        }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    override fun ConfigurationItems(item_modifier: Modifier, onModification: (AppAction) -> Unit) {
        var show_page_selector: Boolean by remember { mutableStateOf(false) }

        LargeDropdownMenu(
            expanded = show_page_selector,
            onDismissRequest = { show_page_selector = false },
            item_count = AppPage.Type.entries.size,
            selected = page.ordinal,
            itemContent = {
                Text(AppPage.Type.entries[it].getName())
            },
            onSelected = {
                onModification(copy(page = AppPage.Type.entries[it]))
                show_page_selector = false
            }
        )


        FlowRow(
            item_modifier,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                getString("appaction_config_open_page_page"),
                Modifier.align(Alignment.CenterVertically),
                softWrap = false
            )

            Button({ show_page_selector = !show_page_selector }) {
                Preview(Modifier)
            }
        }

        AnimatedVisibility(page == AppPage.Type.SETTINGS) {

        }
    }
}
