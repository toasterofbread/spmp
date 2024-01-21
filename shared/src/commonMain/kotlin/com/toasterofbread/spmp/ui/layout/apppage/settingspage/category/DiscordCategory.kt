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
import com.toasterofbread.composekit.settings.ui.item.ComposableSettingsItem
import com.toasterofbread.composekit.settings.ui.item.GroupSettingsItem
import com.toasterofbread.composekit.settings.ui.item.SettingsItem
import com.toasterofbread.composekit.settings.ui.item.InfoTextSettingsItem
import com.toasterofbread.composekit.settings.ui.item.TextFieldSettingsItem
import com.toasterofbread.composekit.settings.ui.item.ToggleSettingsItem
import com.toasterofbread.composekit.settings.ui.item.SettingsValueState
import com.toasterofbread.composekit.utils.composable.LinkifyText
import com.toasterofbread.spmp.model.settings.category.DiscordSettings
import com.toasterofbread.spmp.model.settings.category.InternalSettings
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.DiscordStatus
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.appTextField
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.getDiscordAuthItem

internal fun getDiscordCategoryItems(context: AppContext): List<SettingsItem> {
    if (!DiscordStatus.isSupported()) {
        return emptyList()
    }

    return listOf(
        ComposableSettingsItem { modifier ->
            var accepted: Boolean by InternalSettings.Key.DISCORD_WARNING_ACCEPTED.rememberMutableState()

            AnimatedVisibility(!accepted, enter = expandVertically(), exit = shrinkVertically()) {
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

                        LinkifyText(getString("warning_discord_kizzy"), theme.accent, colour = theme.on_background, style = MaterialTheme.typography.bodyMedium)

                        Button(
                            { accepted = true },
                            Modifier.align(Alignment.End),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = theme.accent,
                                contentColor = theme.on_accent
                            )
                        ) {
                            Text(
                                getString("action_warning_accept")
                            )
                        }
                    }
                }
            }
        },

        getDiscordAuthItem(context),

        GroupSettingsItem(getString("s_group_discord_status_disable_when")),

        ToggleSettingsItem(
            SettingsValueState(DiscordSettings.Key.STATUS_DISABLE_WHEN_INVISIBLE.getName()),
            getString("s_key_discord_status_disable_when_invisible"), null
        ),
        ToggleSettingsItem(
            SettingsValueState(DiscordSettings.Key.STATUS_DISABLE_WHEN_DND.getName()),
            getString("s_key_discord_status_disable_when_dnd"), null
        ),
        ToggleSettingsItem(
            SettingsValueState(DiscordSettings.Key.STATUS_DISABLE_WHEN_IDLE.getName()),
            getString("s_key_discord_status_disable_when_idle"), null
        ),
        ToggleSettingsItem(
            SettingsValueState(DiscordSettings.Key.STATUS_DISABLE_WHEN_OFFLINE.getName()),
            getString("s_key_discord_status_disable_when_offline"), null
        ),
        ToggleSettingsItem(
            SettingsValueState(DiscordSettings.Key.STATUS_DISABLE_WHEN_ONLINE.getName()),
            getString("s_key_discord_status_disable_when_online"), null
        ),

        GroupSettingsItem(getString("s_group_discord_status_content")),

        InfoTextSettingsItem(getString("s_discord_status_text_info")),

        TextFieldSettingsItem(
            SettingsValueState(DiscordSettings.Key.STATUS_NAME.getName()),
            getString("s_key_discord_status_name"), getString("s_sub_discord_status_name"),
            getFieldModifier = { Modifier.appTextField() }
        ),
        TextFieldSettingsItem(
            SettingsValueState(DiscordSettings.Key.STATUS_TEXT_A.getName()),
            getString("s_key_discord_status_text_a"), getString("s_sub_discord_status_text_a"),
            getFieldModifier = { Modifier.appTextField() }
        ),
        TextFieldSettingsItem(
            SettingsValueState(DiscordSettings.Key.STATUS_TEXT_B.getName()),
            getString("s_key_discord_status_text_b"), getString("s_sub_discord_status_text_b"),
            getFieldModifier = { Modifier.appTextField() }
        ),
        TextFieldSettingsItem(
            SettingsValueState(DiscordSettings.Key.STATUS_TEXT_C.getName()),
            getString("s_key_discord_status_text_c"), getString("s_sub_discord_status_text_c"),
            getFieldModifier = { Modifier.appTextField() }
        ),

        ToggleSettingsItem(
            SettingsValueState(DiscordSettings.Key.SHOW_SONG_BUTTON.getName()),
            getString("s_key_discord_status_show_button_song"), getString("s_sub_discord_status_show_button_song")
        ),
        TextFieldSettingsItem(
            SettingsValueState(DiscordSettings.Key.SONG_BUTTON_TEXT.getName()),
            getString("s_key_discord_status_button_song_text"), null
        ),
        ToggleSettingsItem(
            SettingsValueState(DiscordSettings.Key.SHOW_PROJECT_BUTTON.getName()),
            getString("s_key_discord_status_show_button_project"), getString("s_sub_discord_status_show_button_project")
        ),
        TextFieldSettingsItem(
            SettingsValueState(DiscordSettings.Key.PROJECT_BUTTON_TEXT.getName()),
            getString("s_key_discord_status_button_project_text"), null,
            getFieldModifier = { Modifier.appTextField() }
        )
    )
}
