package com.toasterofbread.spmp.ui.layout.prefspage

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.toasterofbread.settings.model.*
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.platform.DiscordStatus
import com.toasterofbread.spmp.platform.ProjectPreferences
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.DiscordAccountPreview
import com.toasterofbread.spmp.ui.layout.DiscordLoginConfirmation

internal fun getDiscordStatusGroup(discord_auth: SettingsValueState<String>): List<SettingsItem> {
    if (!DiscordStatus.isSupported()) {
        return emptyList()
    }

    var account_token by mutableStateOf(discord_auth.value)

    return listOf(
        SettingsItemLargeToggle(
            object : BasicSettingsValueState<Boolean> {
                override var value: Boolean
                    get() = discord_auth.value.isNotEmpty()
                    set(value) {
                        if (!value) {
                            discord_auth.value = ""
                        }
                    }

                override fun init(prefs: ProjectPreferences, defaultProvider: (String) -> Any): BasicSettingsValueState<Boolean> = this
                override fun reset() = discord_auth.reset()
                override fun save() = discord_auth.save()
                override fun getDefault(defaultProvider: (String) -> Any): Boolean =
                    (defaultProvider(Settings.KEY_DISCORD_ACCOUNT_TOKEN.name) as String).isNotEmpty()
            },
            enabled_content = { modifier ->
                if (discord_auth.value.isNotEmpty()) {
                    account_token = discord_auth.value
                }
                if (account_token.isNotEmpty()) {
                    DiscordAccountPreview(account_token, modifier)
                }
            },
            disabled_text = "Not signed in",
            enable_button = "Sign in",
            disable_button = "Sign out",
            warningDialog = { dismiss, openPage ->
                DiscordLoginConfirmation { proceed ->
                    dismiss()
                    if (proceed) {
                        openPage(PrefsPageScreen.DISCORD_LOGIN.ordinal)
                    }
                }
            },
            infoDialog = { dismiss, _ ->
                DiscordLoginConfirmation(true) {
                    dismiss()
                }
            }
        ) { target, setEnabled, _, openPage ->
            if (target) {
                openPage(PrefsPageScreen.DISCORD_LOGIN.ordinal)
            } else {
                setEnabled(false)
            }
        },

        SettingsItemInfoText(getString("s_discord_status_text_info")),

        SettingsItemTextField(
            SettingsValueState(Settings.KEY_DISCORD_STATUS_NAME.name),
            getString("s_key_discord_status_name"), getString("s_sub_discord_status_name")
        ),
        SettingsItemTextField(
            SettingsValueState(Settings.KEY_DISCORD_STATUS_TEXT_A.name),
            getString("s_key_discord_status_text_a"), getString("s_sub_discord_status_text_a")
        ),
        SettingsItemTextField(
            SettingsValueState(Settings.KEY_DISCORD_STATUS_TEXT_B.name),
            getString("s_key_discord_status_text_b"), getString("s_sub_discord_status_text_b")
        ),
        SettingsItemTextField(
            SettingsValueState(Settings.KEY_DISCORD_STATUS_TEXT_C.name),
            getString("s_key_discord_status_text_c"), getString("s_sub_discord_status_text_c")
        ),

        SettingsItemToggle(
            SettingsValueState(Settings.KEY_DISCORD_SHOW_BUTTON_SONG.name),
            getString("s_key_discord_status_show_button_song"), getString("s_sub_discord_status_show_button_song")
        ),
        SettingsItemTextField(
            SettingsValueState(Settings.KEY_DISCORD_BUTTON_SONG_TEXT.name),
            getString("s_key_discord_status_button_song_text"), null
        ),
        SettingsItemToggle(
            SettingsValueState(Settings.KEY_DISCORD_SHOW_BUTTON_PROJECT.name),
            getString("s_key_discord_status_show_button_project"), getString("s_sub_discord_status_show_button_project")
        ),
        SettingsItemTextField(
            SettingsValueState(Settings.KEY_DISCORD_BUTTON_PROJECT_TEXT.name),
            getString("s_key_discord_status_button_project_text"), null
        )
    )
}
