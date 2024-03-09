@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
package com.toasterofbread.spmp.model.settings.category

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import com.toasterofbread.spmp.ProjectBuildConfig
import com.toasterofbread.spmp.model.settings.SettingsKey
import com.toasterofbread.spmp.platform.DiscordStatus
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.category.getDiscordCategoryItems
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.readBytesSync
import org.jetbrains.compose.resources.resource
import org.jetbrains.compose.resources.toImageVector

data object DiscordSettings: SettingsCategory("discord") {
    override val keys: List<SettingsKey> = Key.entries.toList()

    override fun getPage(): CategoryPage? =
        if (!DiscordStatus.isSupported()) null
        else SimplePage(
            getString("s_cat_discord_status"),
            getString("s_cat_desc_discord_status"),
            { getDiscordCategoryItems(it) }
        ) { getIcon() }

    @OptIn(ExperimentalResourceApi::class)
    @Composable
    fun getIcon(): ImageVector =
        resource("assets/drawable/ic_discord.xml").readBytesSync().toImageVector(LocalDensity.current)

    enum class Key: SettingsKey {
        STATUS_DISABLE_WHEN_INVISIBLE,
        STATUS_DISABLE_WHEN_DND,
        STATUS_DISABLE_WHEN_IDLE,
        STATUS_DISABLE_WHEN_OFFLINE,
        STATUS_DISABLE_WHEN_ONLINE,
        STATUS_NAME,
        STATUS_TEXT_A,
        STATUS_TEXT_B,
        STATUS_TEXT_C,
        SHOW_SONG_BUTTON,
        SONG_BUTTON_TEXT,
        SHOW_PROJECT_BUTTON,
        PROJECT_BUTTON_TEXT;

        override val category: SettingsCategory get() = DiscordSettings

        @Suppress("UNCHECKED_CAST")
        override fun <T> getDefaultValue(): T =
            when (this) {
                STATUS_DISABLE_WHEN_INVISIBLE -> false
                STATUS_DISABLE_WHEN_DND -> false
                STATUS_DISABLE_WHEN_IDLE -> false
                STATUS_DISABLE_WHEN_OFFLINE -> false
                STATUS_DISABLE_WHEN_ONLINE -> false

                STATUS_NAME -> ProjectBuildConfig.DISCORD_STATUS_TEXT_NAME_OVERRIDE ?: getString("discord_status_default_name")
                STATUS_TEXT_A -> ProjectBuildConfig.DISCORD_STATUS_TEXT_TEXT_A_OVERRIDE ?: getString("discord_status_default_text_a")
                STATUS_TEXT_B -> ProjectBuildConfig.DISCORD_STATUS_TEXT_TEXT_B_OVERRIDE ?: getString("discord_status_default_text_b")
                STATUS_TEXT_C -> ProjectBuildConfig.DISCORD_STATUS_TEXT_TEXT_C_OVERRIDE ?: getString("discord_status_default_text_c")
                SHOW_SONG_BUTTON -> true
                SONG_BUTTON_TEXT -> ProjectBuildConfig.DISCORD_STATUS_TEXT_BUTTON_SONG_OVERRIDE ?: getString("discord_status_default_button_song")
                SHOW_PROJECT_BUTTON -> true
                PROJECT_BUTTON_TEXT -> ProjectBuildConfig.DISCORD_STATUS_TEXT_BUTTON_PROJECT_OVERRIDE ?: getString("discord_status_default_button_project")
            } as T
    }
}
