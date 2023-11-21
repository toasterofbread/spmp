package com.toasterofbread.spmp.model.settings.category

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.toasterofbread.composekit.settings.ui.item.SettingsItem
import com.toasterofbread.spmp.model.settings.SettingsKey
import com.toasterofbread.spmp.platform.AppContext

sealed class SettingsCategory(id: String) {
    val id: String = id.uppercase()
    abstract val keys: List<SettingsKey>

    abstract fun getPage(): Page?

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
        val title: String,
        val description: String,
    ) {
        abstract fun getItems(context: AppContext): List<SettingsItem>
        @Composable
        abstract fun getIcon(): ImageVector
    }

    protected fun Page(
        title: String,
        description: String,
        getPageItems: (AppContext) -> List<SettingsItem>,
        getPageIcon: @Composable () -> ImageVector
    ): Page =
        object : Page(this, title, description) {
            override fun getItems(context: AppContext): List<SettingsItem> = getPageItems(context)
            @Composable
            override fun getIcon(): ImageVector = getPageIcon()
        }

    companion object {
        val all: List<SettingsCategory> get() =
            listOf(
                SystemSettings,
                BehaviourSettings,
                PlayerSettings,
                FeedSettings,
                ThemeSettings,
                LyricsSettings,
                TopBarSettings,
                DiscordSettings,
                FilterSettings,
                StreamingSettings,
                ServerSettings,
                MiscSettings,

                YTApiSettings,
                AuthSettings,
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
