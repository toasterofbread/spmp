package com.toasterofbread.spmp.model.settings.category

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import dev.toastbits.composekit.settings.ui.SettingsPageWithItems
import dev.toastbits.composekit.settings.ui.item.ComposableSettingsItem
import dev.toastbits.composekit.settings.ui.item.SettingsItem
import dev.toastbits.composekit.platform.PreferencesGroup
import dev.toastbits.composekit.platform.PlatformPreferences
import com.toasterofbread.spmp.platform.AppContext
import dev.toastbits.composekit.platform.PreferencesProperty
import dev.toastbits.composekit.utils.common.amplifyPercent
import dev.toastbits.composekit.settings.ui.SettingsInterface
import LocalPlayerState

sealed class SettingsGroup(
    key: String,
    prefs: PlatformPreferences
): PreferencesGroup(key, prefs) {
    override val group_key: String = key

    // val id: String = id.uppercase()
    // abstract val keys: List<SettingsKey>

    abstract val page: CategoryPage?

    open fun showPage(exporting: Boolean): Boolean = true

    // fun getNameOfKey(key: SettingsKey): String =
    //     "${id}_${(key as Enum<*>).name}"

    // fun getKeyOfName(name: String): SettingsKey? {
    //     val split: List<String> = name.split('_', limit = 2)
    //     if (split.size != 2 || split[0] != id) {
    //         return null
    //     }

    //     return keys.firstOrNull {
    //         (it as Enum<*>).name == split[1]
    //     }
    // }

    abstract class CategoryPage(
        val group: SettingsGroup,
        val getTitle: @Composable () -> String
    ) {
        abstract fun getTitleItem(context: AppContext): SettingsItem?
        abstract fun openPageOnInterface(context: AppContext, settings_interface: SettingsInterface)
        open fun getItems(context: AppContext): List<SettingsItem>? = null
    }

    protected open inner class SimplePage(
        getTitle: @Composable () -> String,
        val getDescription: @Composable () -> String,
        private val getPageItems: () -> List<SettingsItem>,
        private val getPageIcon: @Composable () -> ImageVector,
        private val titleBarEndContent: @Composable (Modifier) -> Unit = {}
    ): CategoryPage(this, getTitle) {
        private var items: List<SettingsItem>? = null

        override fun openPageOnInterface(context: AppContext, settings_interface: SettingsInterface) {
            settings_interface.openPage(
                object : SettingsPageWithItems(
                    getTitle = getTitle,
                    getItems = { getItems(context) },
                    getIcon = getPageIcon
                ) {
                    @Composable
                    override fun TitleBarEndContent(modifier: Modifier) {
                        titleBarEndContent(modifier)
                        super.TitleBarEndContent(modifier)
                    }

                    @Composable
                    override fun canResetKeys(): Boolean =
                        getItems(LocalPlayerState.current.context).isNotEmpty()
                }
            )
        }

        override fun getItems(context: AppContext): List<SettingsItem> {
            if (items == null) {
                items = getPageItems().filter { item ->
                    item.getProperties().none { it.isHidden() }
                }
            }
            return items!!
        }

        override fun getTitleItem(context: AppContext): SettingsItem? =
            ComposableSettingsItem { modifier ->
                ElevatedCard(
                    onClick = {
                        openPageOnInterface(context, this)
                    },
                    modifier = modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = theme.background.amplifyPercent(0.03f),
                        contentColor = theme.on_background
                    )
                ) {
                    Row(
                        Modifier.padding(15.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(15.dp)
                    ) {
                        Icon(getPageIcon(), null)
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(getTitle(), style = MaterialTheme.typography.titleMedium)
                            Text(getDescription(), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
    }
}
