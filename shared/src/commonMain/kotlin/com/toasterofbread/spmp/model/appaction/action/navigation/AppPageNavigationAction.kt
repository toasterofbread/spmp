package com.toasterofbread.spmp.model.appaction.action.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.dp
import com.toasterofbread.composekit.utils.composable.LargeDropdownMenu
import com.toasterofbread.spmp.model.appaction.AppAction
import com.toasterofbread.spmp.model.settings.category.SettingsCategory
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.apppage.AppPage
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.SettingsAppPage
import kotlinx.serialization.Serializable

@Serializable
data class AppPageNavigationAction(
    val page: AppPage.Type = AppPage.Type.DEFAULT,
    val settings_category: String? = null
): NavigationAction {
    override fun getType(): NavigationAction.Type =
        NavigationAction.Type.APP_PAGE

    override suspend fun execute(player: PlayerState) {
        val page: AppPage = page.getPage(player, player.app_page_state) ?: return

        if (page != player.app_page) {
            player.openAppPage(page)
        }

        if (page is SettingsAppPage && settings_category != null) {
            val category_page: SettingsCategory.CategoryPage = SettingsCategory.fromId(settings_category).getPage() ?: return
            category_page.openPageOnInterface(player.context, page.settings_interface)
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

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    override fun ConfigurationItems(item_modifier: Modifier, onModification: (NavigationAction) -> Unit) {
        var show_settings_category_selector: Boolean by remember { mutableStateOf(false) }
        val settings_pages: List<SettingsCategory.CategoryPage> = remember { SettingsCategory.pages }

        LargeDropdownMenu(
            expanded = show_settings_category_selector,
            onDismissRequest = { show_settings_category_selector = false },
            item_count = settings_pages.size + 1,
            selected = settings_category?.let { category_id ->
                settings_pages.indexOfFirst { it.category.id == category_id } + 1
            } ?: 0,
            itemContent = {
                if (it == 0) {
                    Text(getString("appaction_config_navigation_settings_category_none"))
                }
                else {
                    Text(settings_pages[it - 1].name)
                }
            },
            onSelected = {
                val category_id: String? =
                    if (it == 0) null
                    else settings_pages[it - 1].category.id

                onModification(copy(settings_category = category_id))
                show_settings_category_selector = false
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
                    getString("appaction_config_navigation_settings_category"),
                    Modifier.align(Alignment.CenterVertically),
                    softWrap = false
                )

                Button({ show_settings_category_selector = !show_settings_category_selector }) {
                    if (settings_category == null) {
                        Text(getString("appaction_config_navigation_settings_category_none"))
                    }
                    else {
                        Text(settings_pages.first { it.category.id == settings_category }.name)
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
