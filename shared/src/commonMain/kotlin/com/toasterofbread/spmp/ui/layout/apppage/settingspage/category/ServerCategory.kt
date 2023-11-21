package com.toasterofbread.spmp.ui.layout.apppage.settingspage.category

import com.toasterofbread.composekit.settings.ui.item.SettingsItem
import com.toasterofbread.composekit.settings.ui.item.SettingsItemInfoText
import com.toasterofbread.composekit.settings.ui.item.SettingsTextFieldItem
import com.toasterofbread.composekit.settings.ui.item.SettingsToggleItem
import com.toasterofbread.composekit.settings.ui.item.SettingsValueState
import com.toasterofbread.spmp.model.settings.category.ServerSettings
import com.toasterofbread.spmp.resources.getString

internal fun getServerCategoryItems(): List<SettingsItem> {
    // (I will never learn regex)
    // https://stackoverflow.com/a/36760050
    val ip_regex: Regex = "^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}\$".toRegex()
    // https://stackoverflow.com/a/12968117
    val port_regex: Regex = "^([1-9][0-9]{0,3}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5])\$".toRegex()

    check(ip_regex.matches("127.0.0.1"))
    check(port_regex.matches("1111"))

    return listOf(
        SettingsItemInfoText(
            getString("s_info_server")
        ),

        SettingsTextFieldItem(
            SettingsValueState(ServerSettings.Key.IP_ADDRESS.getName()),
            getString("s_key_server_ip"), null,
            getStringError = { input ->
                if (!ip_regex.matches(input)) {
                    return@SettingsTextFieldItem getString("settings_value_not_ipv4")
                }
                return@SettingsTextFieldItem null
            }
        ),

        SettingsTextFieldItem(
            SettingsValueState(
                ServerSettings.Key.PORT.getName(),
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
                    return@SettingsTextFieldItem getString("settings_value_not_port")
                }
                return@SettingsTextFieldItem null
            }
        ),

        SettingsTextFieldItem(
            SettingsValueState(ServerSettings.Key.LOCAL_COMMAND.getName()),
            getString("s_key_server_command"), getString("s_sub_server_command")
        ),

        SettingsToggleItem(
            SettingsValueState(ServerSettings.Key.KILL_CHILD_ON_EXIT.getName()),
            getString("s_key_server_kill_child_on_exit"), null
        )
    )
}
