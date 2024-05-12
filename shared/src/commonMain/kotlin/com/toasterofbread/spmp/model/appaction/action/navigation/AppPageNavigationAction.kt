package com.toasterofbread.spmp.model.appaction.action.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import dev.toastbits.composekit.utils.composable.LargeDropdownMenu
import com.toasterofbread.spmp.model.appaction.AppAction
import com.toasterofbread.spmp.model.settings.category.SettingsGroup
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.apppage.AppPage
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.SettingsAppPage
import kotlinx.serialization.Serializable
import LocalPlayerState

@Serializable
data class AppPageNavigationAction(
    val page: AppPage.Type = AppPage.Type.DEFAULT,
    val settings_group: String? = null
): NavigationAction {
    override fun getType(): NavigationAction.Type =
        NavigationAction.Type.APP_PAGE
    override fun getIcon(): ImageVector =
        page.getIcon()

    override suspend fun execute(player: PlayerState) {
        val page: AppPage = page.getPage(player, player.app_page_state) ?: return
        player.openAppPage(page)

        if (page is SettingsAppPage && settings_group != null) {
            val group_page: SettingsGroup.CategoryPage = player.settings.groupFromKey(settings_group)?.page ?: return
            group_page.openPageOnInterface(player.context, page.settings_interface)
        }
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

    @Composable
    override fun ConfigurationItems(item_modifier: Modifier, onModification: (NavigationAction) -> Unit) {
        val player: PlayerState = LocalPlayerState.current
        var show_settings_group_selector: Boolean by remember { mutableStateOf(false) }
        val settings_pages: List<SettingsGroup.CategoryPage> = remember { player.settings.group_pages }

        LargeDropdownMenu(
            expanded = show_settings_group_selector,
            onDismissRequest = { show_settings_group_selector = false },
            item_count = settings_pages.size + 1,
            selected = settings_group?.let { group_key ->
                settings_pages.indexOfFirst { it.group.group_key == group_key } + 1
            } ?: 0,
            itemContent = {
                if (it == 0) {
                    Text(getString("appaction_config_navigation_settings_group_none"))
                }
                else {
                    Text(settings_pages[it - 1].getTitle())
                }
            },
            onSelected = {
                val group_key: String? =
                    if (it == 0) null
                    else settings_pages[it - 1].group.group_key

                onModification(copy(settings_group = group_key))
                show_settings_group_selector = false
            }
        )

        AnimatedVisibility(
            page == AppPage.Type.SETTINGS,
            item_modifier,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            FlowRow(horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    getString("appaction_config_navigation_settings_group"),
                    Modifier.align(Alignment.CenterVertically),
                    softWrap = false
                )

                Button({ show_settings_group_selector = !show_settings_group_selector }) {
                    if (settings_group == null) {
                        Text(getString("appaction_config_navigation_settings_group_none"))
                    }
                    else {
                        Text(settings_pages.first { it.group.group_key == settings_group }.getTitle())
                    }
                }
            }
        }
    }
}

fun AppPage.Type.getName(): String =
    when (this) {
        AppPage.Type.SONG_FEED -> getString("appaction_navigation_open_page_feed")
        AppPage.Type.LIBRARY -> getString("appaction_navigation_open_page_library")
        AppPage.Type.SEARCH -> getString("appaction_navigation_open_page_search")
        AppPage.Type.RADIO_BUILDER -> getString("appaction_navigation_open_page_radiobuilder")
        AppPage.Type.CONTROL_PANEL -> getString("appaction_navigation_open_page_control")
        AppPage.Type.SETTINGS -> getString("appaction_navigation_open_page_settings")
        AppPage.Type.PROFILE -> getString("appaction_navigation_open_page_profile")
    }
