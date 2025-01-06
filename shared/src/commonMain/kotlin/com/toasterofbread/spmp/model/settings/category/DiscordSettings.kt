@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
package com.toasterofbread.spmp.model.settings.category

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.toasterofbread.spmp.ProjectBuildConfig
import com.toasterofbread.spmp.model.settings.SettingsGroupImpl
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.category.getDiscordCategoryItems
import dev.toastbits.composekit.settingsitem.domain.PlatformSettingsProperty
import dev.toastbits.composekit.settingsitem.domain.SettingsItem
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.discord_status_default_button_project
import spmp.shared.generated.resources.discord_status_default_button_song
import spmp.shared.generated.resources.discord_status_default_name
import spmp.shared.generated.resources.discord_status_default_text_a
import spmp.shared.generated.resources.discord_status_default_text_b
import spmp.shared.generated.resources.discord_status_default_text_c
import spmp.shared.generated.resources.ic_discord
import spmp.shared.generated.resources.s_cat_desc_discord_status
import spmp.shared.generated.resources.s_cat_discord_status
import spmp.shared.generated.resources.s_key_discord_status_button_project_text
import spmp.shared.generated.resources.s_key_discord_status_button_song_text
import spmp.shared.generated.resources.s_key_discord_status_disable_when_dnd
import spmp.shared.generated.resources.s_key_discord_status_disable_when_idle
import spmp.shared.generated.resources.s_key_discord_status_disable_when_invisible
import spmp.shared.generated.resources.s_key_discord_status_disable_when_offline
import spmp.shared.generated.resources.s_key_discord_status_disable_when_online
import spmp.shared.generated.resources.s_key_discord_status_enable
import spmp.shared.generated.resources.s_key_discord_status_large_image_source
import spmp.shared.generated.resources.s_key_discord_status_name
import spmp.shared.generated.resources.s_key_discord_status_show_button_project
import spmp.shared.generated.resources.s_key_discord_status_show_button_song
import spmp.shared.generated.resources.s_key_discord_status_small_image_source
import spmp.shared.generated.resources.s_key_discord_status_text_a
import spmp.shared.generated.resources.s_key_discord_status_text_b
import spmp.shared.generated.resources.s_key_discord_status_text_c
import spmp.shared.generated.resources.s_sub_discord_status_name
import spmp.shared.generated.resources.s_sub_discord_status_show_button_project
import spmp.shared.generated.resources.s_sub_discord_status_show_button_song
import spmp.shared.generated.resources.s_sub_discord_status_text_a
import spmp.shared.generated.resources.s_sub_discord_status_text_b
import spmp.shared.generated.resources.s_sub_discord_status_text_c

class DiscordSettings(val context: AppContext): SettingsGroupImpl("DISCORD", context.getPrefs()) {
    val STATUS_ENABLE: PlatformSettingsProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_discord_status_enable) },
        getDescription = { null },
        getDefaultValue = { true }
    )

    val STATUS_DISABLE_WHEN_INVISIBLE: PlatformSettingsProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_discord_status_disable_when_invisible) },
        getDescription = { null },
        getDefaultValue = { false }
    )
    val STATUS_DISABLE_WHEN_DND: PlatformSettingsProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_discord_status_disable_when_dnd) },
        getDescription = { null },
        getDefaultValue = { false }
    )
    val STATUS_DISABLE_WHEN_IDLE: PlatformSettingsProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_discord_status_disable_when_idle) },
        getDescription = { null },
        getDefaultValue = { false }
    )
    val STATUS_DISABLE_WHEN_OFFLINE: PlatformSettingsProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_discord_status_disable_when_offline) },
        getDescription = { null },
        getDefaultValue = { false }
    )
    val STATUS_DISABLE_WHEN_ONLINE: PlatformSettingsProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_discord_status_disable_when_online) },
        getDescription = { null },
        getDefaultValue = { false }
    )

    val LARGE_IMAGE_SOURCE: PlatformSettingsProperty<ImageSource> by enumProperty(
        getName = { stringResource(Res.string.s_key_discord_status_large_image_source) },
        getDescription = { null },
        getDefaultValue = { ImageSource.SONG }
    )
    val SMALL_IMAGE_SOURCE: PlatformSettingsProperty<ImageSource> by enumProperty(
        getName = { stringResource(Res.string.s_key_discord_status_small_image_source) },
        getDescription = { null },
        getDefaultValue = { ImageSource.ARTIST }
    )

    val STATUS_NAME: PlatformSettingsProperty<String> by resourceDefaultValueProperty(
        getName = { stringResource(Res.string.s_key_discord_status_name) },
        getDescription = { stringResource(Res.string.s_sub_discord_status_name) },
        getDefaultValue = { ProjectBuildConfig.DISCORD_STATUS_TEXT_NAME_OVERRIDE ?: runBlocking { getString(Res.string.discord_status_default_name) } }
    )
    val STATUS_TEXT_A: PlatformSettingsProperty<String> by resourceDefaultValueProperty(
        getName = { stringResource(Res.string.s_key_discord_status_text_a) },
        getDescription = { stringResource(Res.string.s_sub_discord_status_text_a) },
        getDefaultValue = { ProjectBuildConfig.DISCORD_STATUS_TEXT_TEXT_A_OVERRIDE ?: runBlocking { getString(Res.string.discord_status_default_text_a) } }
    )
    val STATUS_TEXT_B: PlatformSettingsProperty<String> by resourceDefaultValueProperty(
        getName = { stringResource(Res.string.s_key_discord_status_text_b) },
        getDescription = { stringResource(Res.string.s_sub_discord_status_text_b) },
        getDefaultValue = { ProjectBuildConfig.DISCORD_STATUS_TEXT_TEXT_B_OVERRIDE ?: runBlocking { getString(Res.string.discord_status_default_text_b) } }
    )
    val STATUS_TEXT_C: PlatformSettingsProperty<String> by resourceDefaultValueProperty(
        getName = { stringResource(Res.string.s_key_discord_status_text_c) },
        getDescription = { stringResource(Res.string.s_sub_discord_status_text_c) },
        getDefaultValue = { ProjectBuildConfig.DISCORD_STATUS_TEXT_TEXT_C_OVERRIDE ?: runBlocking { getString(Res.string.discord_status_default_text_c) } }
    )
    val SHOW_SONG_BUTTON: PlatformSettingsProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_discord_status_show_button_song) },
        getDescription = { stringResource(Res.string.s_sub_discord_status_show_button_song) },
        getDefaultValue = { true }
    )
    val SONG_BUTTON_TEXT: PlatformSettingsProperty<String> by resourceDefaultValueProperty(
        getName = { stringResource(Res.string.s_key_discord_status_button_song_text) },
        getDescription = { null },
        getDefaultValue = { ProjectBuildConfig.DISCORD_STATUS_TEXT_BUTTON_SONG_OVERRIDE ?: runBlocking { getString(Res.string.discord_status_default_button_song) } }
    )
    val SHOW_PROJECT_BUTTON: PlatformSettingsProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_discord_status_show_button_project) },
        getDescription = { stringResource(Res.string.s_sub_discord_status_show_button_project) },
        getDefaultValue = { true }
    )
    val PROJECT_BUTTON_TEXT: PlatformSettingsProperty<String> by resourceDefaultValueProperty(
        getName = { stringResource(Res.string.s_key_discord_status_button_project_text) },
        getDescription = { null },
        getDefaultValue = { ProjectBuildConfig.DISCORD_STATUS_TEXT_BUTTON_PROJECT_OVERRIDE ?: runBlocking { getString(Res.string.discord_status_default_button_project) } }
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

    enum class ImageSource {
        SONG,
        ARTIST,
        ALBUM,
        ALT
    }
}
