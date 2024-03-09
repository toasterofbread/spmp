package com.toasterofbread.spmp.model.settings.category

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toasterofbread.composekit.settings.ui.item.SettingsItem
import com.toasterofbread.spmp.ProjectBuildConfig
import com.toasterofbread.spmp.model.settings.SettingsKey
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.getDiscordAuthItem

data object DiscordAuthSettings: SettingsCategory("discordauth") {
    override val keys: List<SettingsKey> = Key.entries.toList()

    override fun getPage(): CategoryPage? =
        object : CategoryPage(
            this,
            getString("s_cat_discord_auth")
        ) {
            override fun getTitleItem(context: AppContext): SettingsItem? =
                getDiscordAuthItem(
                    context,
                    info_only = true,
                    ignore_prerequisite = true,
                    StartIcon = {
                        Box(Modifier.height(40.dp).padding(end = 20.dp), contentAlignment = Alignment.Center) {
                            Icon(DiscordSettings.getIcon(), null)
                        }
                    }
                )
        }
    override fun showPage(exporting: Boolean): Boolean = exporting

    enum class Key: SettingsKey {
        DISCORD_ACCOUNT_TOKEN,
        DISCORD_WARNING_ACCEPTED;

        override val category: SettingsCategory get() = DiscordAuthSettings

        @Suppress("UNCHECKED_CAST")
        override fun <T> getDefaultValue(): T =
            when (this) {
                DISCORD_ACCOUNT_TOKEN -> ProjectBuildConfig.DISCORD_ACCOUNT_TOKEN ?: ""
                DISCORD_WARNING_ACCEPTED -> false
            } as T
    }
}
