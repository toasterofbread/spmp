package com.toasterofbread.spmp.ui.layout.apppage.settingspage.category

import com.toasterofbread.composekit.settings.ui.item.SettingsItem
import com.toasterofbread.composekit.settings.ui.item.InfoTextSettingsItem
import com.toasterofbread.composekit.settings.ui.item.TextFieldSettingsItem
import com.toasterofbread.composekit.settings.ui.item.ToggleSettingsItem
import com.toasterofbread.composekit.settings.ui.item.SettingsValueState
import com.toasterofbread.spmp.model.settings.category.DesktopSettings
import com.toasterofbread.spmp.resources.getString

internal fun getDesktopCategoryItems(): List<SettingsItem> {
    // (I will never learn regex)
    // https://stackoverflow.com/a/36760050
    val ip_regex: Regex = "^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}\$".toRegex()
    // https://stackoverflow.com/a/12968117
    val port_regex: Regex = "^([1-9][0-9]{0,3}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5])\$".toRegex()

    check(ip_regex.matches("127.0.0.1"))
    check(port_regex.matches("1111"))

    return listOf(
        TextFieldSettingsItem(
            SettingsValueState(DesktopSettings.Key.STARTUP_COMMAND.getName()),
            getString("s_key_startup_command"), getString("s_sub_startup_command")
        ),

        InfoTextSettingsItem(
            getString("s_info_server")
        ),

        TextFieldSettingsItem(
            SettingsValueState(DesktopSettings.Key.SERVER_IP_ADDRESS.getName()),
            getString("s_key_server_ip"), null,
            getStringError = { input ->
                if (!ip_regex.matches(input)) {
                    return@TextFieldSettingsItem getString("settings_value_not_ipv4")
                }
                return@TextFieldSettingsItem null
            }
        ),

        TextFieldSettingsItem(
            SettingsValueState(
                DesktopSettings.Key.SERVER_PORT.getName(),
                getValueConverter = {
                    it?.toString()
                },
                setValueConverter = {
                    it.toInt()
                }
            ),
            getString("s_key_server_port"), null,
            getStringError = { input ->
                if (!port_regex.matches(input)) {
                    return@TextFieldSettingsItem getString("settings_value_not_port")
                }
                return@TextFieldSettingsItem null
            }
        ),

        TextFieldSettingsItem(
            SettingsValueState(DesktopSettings.Key.SERVER_LOCAL_COMMAND.getName()),
            getString("s_key_server_command"), getString("s_sub_server_command")
        ),

        ToggleSettingsItem(
            SettingsValueState(DesktopSettings.Key.SERVER_KILL_CHILD_ON_EXIT.getName()),
            getString("s_key_server_kill_child_on_exit"), null
        )
    )
}
