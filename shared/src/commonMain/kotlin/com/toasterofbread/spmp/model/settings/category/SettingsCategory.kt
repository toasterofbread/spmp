package com.toasterofbread.spmp.model.settings.category

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.toasterofbread.composekit.settings.ui.SettingsPageWithItems
import com.toasterofbread.composekit.settings.ui.item.ComposableSettingsItem
import com.toasterofbread.composekit.settings.ui.item.SettingsItem
import com.toasterofbread.composekit.utils.common.blendWith
import com.toasterofbread.spmp.model.settings.SettingsKey
import com.toasterofbread.spmp.platform.AppContext

sealed class SettingsCategory(id: String) {
    val id: String = id.uppercase()
    abstract val keys: List<SettingsKey>

    abstract fun getPage(): Page?
    open fun showPage(exporting: Boolean): Boolean = true

    fun getNameOfKey(key: SettingsKey): String =
        "${id}_${(key as Enum<*>).name}"

    fun getKeyOfName(name: String): SettingsKey? {
        val split: List<String> = name.split('_', limit = 2)
        if (split.size != 2 || split[0] != id) {
            return null
        }

        return keys.firstOrNull {
            (it as Enum<*>).name == split[1]
        }
    }

    abstract class Page(
        val category: SettingsCategory,
        val name: String
    ) {
        abstract fun getTitleItem(context: AppContext): SettingsItem?
    }

    @OptIn(ExperimentalMaterial3Api::class)
    protected fun Page(
        title: String,
        description: String,
        getPageItems: (AppContext) -> List<SettingsItem>,
        getPageIcon: @Composable () -> ImageVector
    ): Page =
        object : Page(
            this,
            title
        ) {
            override fun getTitleItem(context: AppContext): SettingsItem? =
                ComposableSettingsItem { modifier ->
                    ElevatedCard(
                        onClick = {
                            openPage(
                                SettingsPageWithItems(
                                    getTitle = { title },
                                    getItems = { getPageItems(context) },
                                    getIcon = { getPageIcon() }
                                )
                            )
                        },
                        modifier = modifier.fillMaxWidth(),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = theme.accent.blendWith(theme.background, 0.05f),
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
                                Text(title, style = MaterialTheme.typography.titleMedium)
                                Text(description, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
        }

    companion object {
        val all: List<SettingsCategory> get() =
            listOf(
                YoutubeAuthSettings,

                SystemSettings,
                BehaviourSettings,
                PlayerSettings,
                FeedSettings,
                ThemeSettings,
                LyricsSettings,
                TopBarSettings,
                DiscordSettings,
                DiscordAuthSettings,
                FilterSettings,
                StreamingSettings,
                ServerSettings,
                MiscSettings,

                YTApiSettings,
                InternalSettings
            )

        val with_page: List<SettingsCategory> get() =
            all.filter { it.getPage() != null }

        val pages: List<Page> get() =
            all.mapNotNull { it.getPage() }

        fun fromId(id: String): SettingsCategory =
            all.first { it.id == id }
    }
}
