@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
package com.toasterofbread.spmp.model.settings.category

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.toasterofbread.spmp.ProjectBuildConfig
import com.toasterofbread.spmp.platform.DiscordStatus
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.category.getDiscordCategoryItems
import dev.toastbits.composekit.platform.PlatformPreferences
import dev.toastbits.composekit.platform.PreferencesProperty
import org.jetbrains.compose.resources.vectorResource
import spmp.shared.generated.resources.*

class DiscordSettings(val context: AppContext): SettingsGroup("DISCORD", context.getPrefs()) {
    val STATUS_DISABLE_WHEN_INVISIBLE: PreferencesProperty<Boolean> by property(
        getName = { getString("s_key_discord_status_disable_when_invisible") },
        getDescription = { null },
        getDefaultValue = { false }
    )
    val STATUS_DISABLE_WHEN_DND: PreferencesProperty<Boolean> by property(
        getName = { getString("s_key_discord_status_disable_when_dnd") },
        getDescription = { null },
        getDefaultValue = { false }
    )
    val STATUS_DISABLE_WHEN_IDLE: PreferencesProperty<Boolean> by property(
        getName = { getString("s_key_discord_status_disable_when_idle") },
        getDescription = { null },
        getDefaultValue = { false }
    )
    val STATUS_DISABLE_WHEN_OFFLINE: PreferencesProperty<Boolean> by property(
        getName = { getString("s_key_discord_status_disable_when_offline") },
        getDescription = { null },
        getDefaultValue = { false }
    )
    val STATUS_DISABLE_WHEN_ONLINE: PreferencesProperty<Boolean> by property(
        getName = { getString("s_key_discord_status_disable_when_online") },
        getDescription = { null },
        getDefaultValue = { false }
    )

    val STATUS_NAME: PreferencesProperty<String> by property(
        getName = { getString("s_key_discord_status_name") },
        getDescription = { getString("s_sub_discord_status_name") },
        getDefaultValue = { ProjectBuildConfig.DISCORD_STATUS_TEXT_NAME_OVERRIDE ?: getString("discord_status_default_name") }
    )
    val STATUS_TEXT_A: PreferencesProperty<String> by property(
        getName = { getString("s_key_discord_status_text_a") },
        getDescription = { getString("s_sub_discord_status_text_a") },
        getDefaultValue = { ProjectBuildConfig.DISCORD_STATUS_TEXT_TEXT_A_OVERRIDE ?: getString("discord_status_default_text_a") }
    )
    val STATUS_TEXT_B: PreferencesProperty<String> by property(
        getName = { getString("s_key_discord_status_text_b") },
        getDescription = { getString("s_sub_discord_status_text_b") },
        getDefaultValue = { ProjectBuildConfig.DISCORD_STATUS_TEXT_TEXT_B_OVERRIDE ?: getString("discord_status_default_text_b") }
    )
    val STATUS_TEXT_C: PreferencesProperty<String> by property(
        getName = { getString("s_key_discord_status_text_c") },
        getDescription = { getString("s_sub_discord_status_text_c") },
        getDefaultValue = { ProjectBuildConfig.DISCORD_STATUS_TEXT_TEXT_C_OVERRIDE ?: getString("discord_status_default_text_c") }
    )
    val SHOW_SONG_BUTTON: PreferencesProperty<Boolean> by property(
        getName = { getString("s_key_discord_status_show_button_song") },
        getDescription = { getString("s_sub_discord_status_show_button_song") },
        getDefaultValue = { true }
    )
    val SONG_BUTTON_TEXT: PreferencesProperty<String> by property(
        getName = { getString("s_key_discord_status_button_song_text") },
        getDescription = { null },
        getDefaultValue = { ProjectBuildConfig.DISCORD_STATUS_TEXT_BUTTON_SONG_OVERRIDE ?: getString("discord_status_default_button_song") }
    )
    val SHOW_PROJECT_BUTTON: PreferencesProperty<Boolean> by property(
        getName = { getString("s_key_discord_status_show_button_project") },
        getDescription = { getString("s_sub_discord_status_show_button_project") },
        getDefaultValue = { true }
    )
    val PROJECT_BUTTON_TEXT: PreferencesProperty<String> by property(
        getName = { getString("s_key_discord_status_button_project_text") },
        getDescription = { null },
        getDefaultValue = { ProjectBuildConfig.DISCORD_STATUS_TEXT_BUTTON_PROJECT_OVERRIDE ?: getString("discord_status_default_button_project") }
    )

    override val page: CategoryPage? =
        if (!DiscordStatus.isSupported()) null
        else SimplePage(
            { getString("s_cat_discord_status") },
            { getString("s_cat_desc_discord_status") },
            { getDiscordCategoryItems(context) },
            { getIcon() }
        )

    companion object {
        @Composable
        fun getIcon(): ImageVector =
            vectorResource(Res.drawable.ic_discord)
    }
}
