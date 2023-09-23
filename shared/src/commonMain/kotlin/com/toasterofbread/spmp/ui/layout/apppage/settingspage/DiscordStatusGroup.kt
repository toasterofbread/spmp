package com.toasterofbread.spmp.ui.layout.apppage.settingspage

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.toasterofbread.composesettings.ui.item.BasicSettingsValueState
import com.toasterofbread.composesettings.ui.item.SettingsComposableItem
import com.toasterofbread.composesettings.ui.item.SettingsGroupItem
import com.toasterofbread.composesettings.ui.item.SettingsItem
import com.toasterofbread.composesettings.ui.item.SettingsItemInfoText
import com.toasterofbread.composesettings.ui.item.SettingsLargeToggleItem
import com.toasterofbread.composesettings.ui.item.SettingsTextFieldItem
import com.toasterofbread.composesettings.ui.item.SettingsToggleItem
import com.toasterofbread.composesettings.ui.item.SettingsValueState
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.platform.DiscordStatus
import com.toasterofbread.spmp.platform.PlatformPreferences
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.DiscordAccountPreview
import com.toasterofbread.spmp.ui.layout.DiscordLoginConfirmation
import com.toasterofbread.utils.composable.LinkifyText

internal fun getDiscordStatusGroup(discord_auth: SettingsValueState<String>): List<SettingsItem> {
    if (!DiscordStatus.isSupported()) {
        return emptyList()
    }

    var account_token by mutableStateOf(discord_auth.get())

    return listOf(
        SettingsComposableItem {
            var accepted: Boolean by Settings.INTERNAL_DISCORD_WARNING_ACCEPTED.rememberMutableState()

            AnimatedVisibility(!accepted, enter = expandVertically(), exit = shrinkVertically()) {
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = background,
                        contentColor = on_background
                    ),
                    border = BorderStroke(2.dp, Color.Red),
                ) {
                    Column(Modifier.fillMaxSize().padding(15.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Icon(Icons.Default.Warning, null, tint = Color.Red)

                        LinkifyText(getString("warning_discord_kizzy"), colour = on_background, style = MaterialTheme.typography.bodyMedium)

                        Button(
                            { accepted = true },
                            Modifier.align(Alignment.End),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accent,
                                contentColor = on_accent
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

        SettingsLargeToggleItem(
            object : BasicSettingsValueState<Boolean> {
                override fun get(): Boolean = discord_auth.get().isNotEmpty()
                override fun set(value: Boolean) {
                    if (!value) {
                        discord_auth.set("")
                    }
                }

                override fun init(prefs: PlatformPreferences, defaultProvider: (String) -> Any): BasicSettingsValueState<Boolean> = this
                override fun release(prefs: PlatformPreferences) {}
                override fun reset() = discord_auth.reset()
                override fun save() = discord_auth.save()
                override fun getDefault(defaultProvider: (String) -> Any): Boolean =
                    (defaultProvider(Settings.KEY_DISCORD_ACCOUNT_TOKEN.name) as String).isNotEmpty()
            },
            enabled_content = { modifier ->
                val auth = discord_auth.get()
                if (auth.isNotEmpty()) {
                    account_token = auth
                }
                if (account_token.isNotEmpty()) {
                    DiscordAccountPreview(account_token, modifier)
                }
            },
            disabled_text = getString("auth_not_signed_in"),
            enable_button = getString("auth_sign_in"),
            disable_button = getString("auth_sign_out"),
            warningDialog = { dismiss, openPage ->
                DiscordLoginConfirmation { manual ->
                    dismiss()
                    if (manual != null) {
                        openPage(PrefsPageScreen.DISCORD_LOGIN.ordinal, manual)
                    }
                }
            },
            infoDialog = { dismiss, _ ->
                DiscordLoginConfirmation(true) {
                    dismiss()
                }
            },
            prerequisite_value = SettingsValueState(Settings.INTERNAL_DISCORD_WARNING_ACCEPTED.name)
        ) { target, setEnabled, _, openPage ->
            if (target) {
                openPage(PrefsPageScreen.DISCORD_LOGIN.ordinal, null)
            } else {
                setEnabled(false)
            }
        },

        SettingsGroupItem(getString("s_group_discord_status_disable_when")),

        SettingsToggleItem(
            SettingsValueState(Settings.KEY_DISCORD_STATUS_DISABLE_WHEN_INVISIBLE.name),
            getString("s_key_discord_status_disable_when_invisible"), null
        ),
        SettingsToggleItem(
            SettingsValueState(Settings.KEY_DISCORD_STATUS_DISABLE_WHEN_DND.name),
            getString("s_key_discord_status_disable_when_dnd"), null
        ),
        SettingsToggleItem(
            SettingsValueState(Settings.KEY_DISCORD_STATUS_DISABLE_WHEN_IDLE.name),
            getString("s_key_discord_status_disable_when_idle"), null
        ),
        SettingsToggleItem(
            SettingsValueState(Settings.KEY_DISCORD_STATUS_DISABLE_WHEN_OFFLINE.name),
            getString("s_key_discord_status_disable_when_offline"), null
        ),
        SettingsToggleItem(
            SettingsValueState(Settings.KEY_DISCORD_STATUS_DISABLE_WHEN_ONLINE.name),
            getString("s_key_discord_status_disable_when_online"), null
        ),

        SettingsGroupItem(getString("s_group_discord_status_content")),

        SettingsItemInfoText(getString("s_discord_status_text_info")),

        SettingsTextFieldItem(
            SettingsValueState(Settings.KEY_DISCORD_STATUS_NAME.name),
            getString("s_key_discord_status_name"), getString("s_sub_discord_status_name")
        ),
        SettingsTextFieldItem(
            SettingsValueState(Settings.KEY_DISCORD_STATUS_TEXT_A.name),
            getString("s_key_discord_status_text_a"), getString("s_sub_discord_status_text_a")
        ),
        SettingsTextFieldItem(
            SettingsValueState(Settings.KEY_DISCORD_STATUS_TEXT_B.name),
            getString("s_key_discord_status_text_b"), getString("s_sub_discord_status_text_b")
        ),
        SettingsTextFieldItem(
            SettingsValueState(Settings.KEY_DISCORD_STATUS_TEXT_C.name),
            getString("s_key_discord_status_text_c"), getString("s_sub_discord_status_text_c")
        ),

        SettingsToggleItem(
            SettingsValueState(Settings.KEY_DISCORD_SHOW_BUTTON_SONG.name),
            getString("s_key_discord_status_show_button_song"), getString("s_sub_discord_status_show_button_song")
        ),
        SettingsTextFieldItem(
            SettingsValueState(Settings.KEY_DISCORD_BUTTON_SONG_TEXT.name),
            getString("s_key_discord_status_button_song_text"), null
        ),
        SettingsToggleItem(
            SettingsValueState(Settings.KEY_DISCORD_SHOW_BUTTON_PROJECT.name),
            getString("s_key_discord_status_show_button_project"), getString("s_sub_discord_status_show_button_project")
        ),
        SettingsTextFieldItem(
            SettingsValueState(Settings.KEY_DISCORD_BUTTON_PROJECT_TEXT.name),
            getString("s_key_discord_status_button_project_text"), null
        )
    )
}
