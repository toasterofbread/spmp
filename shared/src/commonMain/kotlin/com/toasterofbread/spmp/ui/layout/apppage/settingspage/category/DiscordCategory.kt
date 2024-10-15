package com.toasterofbread.spmp.ui.layout.apppage.settingspage.category

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.toastbits.composekit.settings.ui.component.item.ComposableSettingsItem
import dev.toastbits.composekit.settings.ui.component.item.GroupSettingsItem
import dev.toastbits.composekit.settings.ui.component.item.SettingsItem
import dev.toastbits.composekit.settings.ui.component.item.InfoTextSettingsItem
import dev.toastbits.composekit.settings.ui.component.item.TextFieldSettingsItem
import dev.toastbits.composekit.settings.ui.component.item.ToggleSettingsItem
import dev.toastbits.composekit.platform.PreferencesProperty
import dev.toastbits.composekit.utils.composable.LinkifyText
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.DiscordStatus
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.appTextField
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.getDiscordAuthItem
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import LocalProgramArguments
import ProgramArguments
import LocalPlayerState
import dev.toastbits.composekit.platform.composable.theme.LocalApplicationTheme
import dev.toastbits.composekit.settings.ui.ThemeValues
import dev.toastbits.composekit.settings.ui.on_accent
import dev.toastbits.composekit.settings.ui.vibrant_accent
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.action_warning_accept
import spmp.shared.generated.resources.flatpak_documentation_url
import spmp.shared.generated.resources.`info_flatpak_discord_$url`
import spmp.shared.generated.resources.s_group_discord_status_disable_when
import spmp.shared.generated.resources.s_group_discord_status_content
import spmp.shared.generated.resources.s_discord_status_text_info

internal fun getDiscordCategoryItems(context: AppContext): List<SettingsItem> {
    if (!DiscordStatus.isSupported()) {
        return emptyList()
    }

    return listOf(
        ComposableSettingsItem { modifier ->
            val theme: ThemeValues = LocalApplicationTheme.current
            var accepted: Boolean by context.settings.discord_auth.DISCORD_WARNING_ACCEPTED.observe()
            val warning_text: String? = DiscordStatus.getWarningText()

            AnimatedVisibility(warning_text != null && !accepted, enter = expandVertically(), exit = shrinkVertically()) {
                Card(
                    modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = theme.background,
                        contentColor = theme.on_background
                    ),
                    border = BorderStroke(2.dp, Color.Red),
                ) {
                    Column(Modifier.fillMaxSize().padding(15.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Icon(Icons.Default.Warning, null, tint = Color.Red)

                        LinkifyText(warning_text ?: "", theme.accent, style = MaterialTheme.typography.bodyMedium.copy(color = theme.on_background))

                        Button(
                            { accepted = true },
                            Modifier.align(Alignment.End),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = theme.accent,
                                contentColor = theme.on_accent
                            )
                        ) {
                            Text(
                                stringResource(Res.string.action_warning_accept)
                            )
                        }
                    }
                }
            }
        },

        ComposableSettingsItem {
            val program_arguments: ProgramArguments = LocalProgramArguments.current
            val player: PlayerState = LocalPlayerState.current

            if (program_arguments.is_flatpak) {
                LinkifyText(
                    stringResource(Res.string.`info_flatpak_discord_$url`).replace("\$url", stringResource(Res.string.flatpak_documentation_url) + " "),
                    player.theme.vibrant_accent
                )
            }
        },

        getDiscordAuthItem(context),

        GroupSettingsItem(Res.string.s_group_discord_status_disable_when),

        ToggleSettingsItem(
            context.settings.discord.STATUS_DISABLE_WHEN_INVISIBLE
        ),
        ToggleSettingsItem(
            context.settings.discord.STATUS_DISABLE_WHEN_DND
        ),
        ToggleSettingsItem(
            context.settings.discord.STATUS_DISABLE_WHEN_IDLE
        ),
        ToggleSettingsItem(
            context.settings.discord.STATUS_DISABLE_WHEN_OFFLINE
        ),
        ToggleSettingsItem(
            context.settings.discord.STATUS_DISABLE_WHEN_ONLINE
        ),

        GroupSettingsItem(Res.string.s_group_discord_status_content),

        InfoTextSettingsItem(Res.string.s_discord_status_text_info),

        TextFieldSettingsItem(
            context.settings.discord.STATUS_NAME,
            getFieldModifier = { Modifier.appTextField() }
        ),
        TextFieldSettingsItem(
            context.settings.discord.STATUS_TEXT_A,
            getFieldModifier = { Modifier.appTextField() }
        ),
        TextFieldSettingsItem(
            context.settings.discord.STATUS_TEXT_B,
            getFieldModifier = { Modifier.appTextField() }
        ),
        TextFieldSettingsItem(
            context.settings.discord.STATUS_TEXT_C,
            getFieldModifier = { Modifier.appTextField() }
        ),

        ToggleSettingsItem(
            context.settings.discord.SHOW_SONG_BUTTON
        ),
        TextFieldSettingsItem(
            context.settings.discord.SONG_BUTTON_TEXT
        ),
        ToggleSettingsItem(
            context.settings.discord.SHOW_PROJECT_BUTTON
        ),
        TextFieldSettingsItem(
            context.settings.discord.PROJECT_BUTTON_TEXT,
            getFieldModifier = { Modifier.appTextField() }
        )
    )
}
