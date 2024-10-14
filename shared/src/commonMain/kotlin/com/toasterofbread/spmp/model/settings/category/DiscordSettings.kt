@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
package com.toasterofbread.spmp.model.settings.category

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.toasterofbread.spmp.ProjectBuildConfig
import com.toasterofbread.spmp.platform.DiscordStatus
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.category.getDiscordCategoryItems
import dev.toastbits.composekit.platform.PlatformPreferences
import dev.toastbits.composekit.platform.PreferencesProperty
import dev.toastbits.composekit.settings.ui.component.item.SettingsItem
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.vectorResource
import spmp.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.s_key_discord_status_disable_when_invisible
import spmp.shared.generated.resources.s_key_discord_status_disable_when_dnd
import spmp.shared.generated.resources.s_key_discord_status_disable_when_idle
import spmp.shared.generated.resources.s_key_discord_status_disable_when_offline
import spmp.shared.generated.resources.s_key_discord_status_disable_when_online
import spmp.shared.generated.resources.s_key_discord_status_name
import spmp.shared.generated.resources.s_sub_discord_status_name
import spmp.shared.generated.resources.discord_status_default_name
import spmp.shared.generated.resources.s_key_discord_status_text_a
import spmp.shared.generated.resources.s_sub_discord_status_text_a
import spmp.shared.generated.resources.discord_status_default_text_a
import spmp.shared.generated.resources.s_key_discord_status_text_b
import spmp.shared.generated.resources.s_sub_discord_status_text_b
import spmp.shared.generated.resources.discord_status_default_text_b
import spmp.shared.generated.resources.s_key_discord_status_text_c
import spmp.shared.generated.resources.s_sub_discord_status_text_c
import spmp.shared.generated.resources.discord_status_default_text_c
import spmp.shared.generated.resources.s_key_discord_status_show_button_song
import spmp.shared.generated.resources.s_sub_discord_status_show_button_song
import spmp.shared.generated.resources.s_key_discord_status_button_song_text
import spmp.shared.generated.resources.discord_status_default_button_song
import spmp.shared.generated.resources.s_key_discord_status_show_button_project
import spmp.shared.generated.resources.s_sub_discord_status_show_button_project
import spmp.shared.generated.resources.s_key_discord_status_button_project_text
import spmp.shared.generated.resources.discord_status_default_button_project
import spmp.shared.generated.resources.s_cat_discord_status
import spmp.shared.generated.resources.s_cat_desc_discord_status

class DiscordSettings(val context: AppContext): SettingsGroup("DISCORD", context.getPrefs()) {
    val STATUS_DISABLE_WHEN_INVISIBLE: PreferencesProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_discord_status_disable_when_invisible) },
        getDescription = { null },
        getDefaultValue = { false }
    )
    val STATUS_DISABLE_WHEN_DND: PreferencesProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_discord_status_disable_when_dnd) },
        getDescription = { null },
        getDefaultValue = { false }
    )
    val STATUS_DISABLE_WHEN_IDLE: PreferencesProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_discord_status_disable_when_idle) },
        getDescription = { null },
        getDefaultValue = { false }
    )
    val STATUS_DISABLE_WHEN_OFFLINE: PreferencesProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_discord_status_disable_when_offline) },
        getDescription = { null },
        getDefaultValue = { false }
    )
    val STATUS_DISABLE_WHEN_ONLINE: PreferencesProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_discord_status_disable_when_online) },
        getDescription = { null },
        getDefaultValue = { false }
    )

    val STATUS_NAME: PreferencesProperty<String> by resourceDefaultValueProperty(
        getName = { stringResource(Res.string.s_key_discord_status_name) },
        getDescription = { stringResource(Res.string.s_sub_discord_status_name) },
        getDefaultValueSuspending = { ProjectBuildConfig.DISCORD_STATUS_TEXT_NAME_OVERRIDE ?: getString(Res.string.discord_status_default_name) },
        getDefaultValueComposable = { ProjectBuildConfig.DISCORD_STATUS_TEXT_NAME_OVERRIDE ?: stringResource(Res.string.discord_status_default_name) }
    )
    val STATUS_TEXT_A: PreferencesProperty<String> by resourceDefaultValueProperty(
        getName = { stringResource(Res.string.s_key_discord_status_text_a) },
        getDescription = { stringResource(Res.string.s_sub_discord_status_text_a) },
        getDefaultValueSuspending = { ProjectBuildConfig.DISCORD_STATUS_TEXT_TEXT_A_OVERRIDE ?: getString(Res.string.discord_status_default_text_a) },
        getDefaultValueComposable = { ProjectBuildConfig.DISCORD_STATUS_TEXT_TEXT_A_OVERRIDE ?: stringResource(Res.string.discord_status_default_text_a) }
    )
    val STATUS_TEXT_B: PreferencesProperty<String> by resourceDefaultValueProperty(
        getName = { stringResource(Res.string.s_key_discord_status_text_b) },
        getDescription = { stringResource(Res.string.s_sub_discord_status_text_b) },
        getDefaultValueSuspending = { ProjectBuildConfig.DISCORD_STATUS_TEXT_TEXT_B_OVERRIDE ?: getString(Res.string.discord_status_default_text_b) },
        getDefaultValueComposable = { ProjectBuildConfig.DISCORD_STATUS_TEXT_TEXT_B_OVERRIDE ?: stringResource(Res.string.discord_status_default_text_b) }
    )
    val STATUS_TEXT_C: PreferencesProperty<String> by resourceDefaultValueProperty(
        getName = { stringResource(Res.string.s_key_discord_status_text_c) },
        getDescription = { stringResource(Res.string.s_sub_discord_status_text_c) },
        getDefaultValueSuspending = { ProjectBuildConfig.DISCORD_STATUS_TEXT_TEXT_C_OVERRIDE ?: getString(Res.string.discord_status_default_text_c) },
        getDefaultValueComposable = { ProjectBuildConfig.DISCORD_STATUS_TEXT_TEXT_C_OVERRIDE ?: stringResource(Res.string.discord_status_default_text_c) }
    )
    val SHOW_SONG_BUTTON: PreferencesProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_discord_status_show_button_song) },
        getDescription = { stringResource(Res.string.s_sub_discord_status_show_button_song) },
        getDefaultValue = { true }
    )
    val SONG_BUTTON_TEXT: PreferencesProperty<String> by resourceDefaultValueProperty(
        getName = { stringResource(Res.string.s_key_discord_status_button_song_text) },
        getDescription = { null },
        getDefaultValueSuspending = { ProjectBuildConfig.DISCORD_STATUS_TEXT_BUTTON_SONG_OVERRIDE ?: getString(Res.string.discord_status_default_button_song) },
        getDefaultValueComposable = { ProjectBuildConfig.DISCORD_STATUS_TEXT_BUTTON_SONG_OVERRIDE ?: stringResource(Res.string.discord_status_default_button_song) }
    )
    val SHOW_PROJECT_BUTTON: PreferencesProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_discord_status_show_button_project) },
        getDescription = { stringResource(Res.string.s_sub_discord_status_show_button_project) },
        getDefaultValue = { true }
    )
    val PROJECT_BUTTON_TEXT: PreferencesProperty<String> by resourceDefaultValueProperty(
        getName = { stringResource(Res.string.s_key_discord_status_button_project_text) },
        getDescription = { null },
        getDefaultValueSuspending = { ProjectBuildConfig.DISCORD_STATUS_TEXT_BUTTON_PROJECT_OVERRIDE ?: getString(Res.string.discord_status_default_button_project) },
        getDefaultValueComposable = { ProjectBuildConfig.DISCORD_STATUS_TEXT_BUTTON_PROJECT_OVERRIDE ?: stringResource(Res.string.discord_status_default_button_project) }
    )

    @Composable
    override fun getTitle(): String = stringResource(Res.string.s_cat_discord_status)

    @Composable
    override fun getDescription(): String = stringResource(Res.string.s_cat_desc_discord_status)

    @Composable
    override fun getIcon(): ImageVector = getDiscordIcon()

    override fun getConfigurationItems(): List<SettingsItem> = getDiscordCategoryItems(context)

    companion object {
        @Composable
        fun getDiscordIcon(): ImageVector =
            vectorResource(Res.drawable.ic_discord)
    }
}
