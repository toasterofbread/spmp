package com.toasterofbread.spmp.ui.layout.apppage.settingspage.category

import androidx.compose.ui.Modifier
import com.toasterofbread.composekit.settings.ui.item.GroupSettingsItem
import com.toasterofbread.composekit.settings.ui.item.InfoTextSettingsItem
import com.toasterofbread.composekit.settings.ui.item.SettingsItem
import com.toasterofbread.composekit.settings.ui.item.SettingsValueState
import com.toasterofbread.composekit.settings.ui.item.TextFieldSettingsItem
import com.toasterofbread.composekit.settings.ui.item.ToggleSettingsItem
import com.toasterofbread.spmp.model.settings.category.DesktopSettings
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.appTextField

internal fun getDesktopCategoryItems(): List<SettingsItem> {
    return listOf(
        GroupSettingsItem(
            getString("s_group_desktop_system")
        ),

        TextFieldSettingsItem(
            SettingsValueState(DesktopSettings.Key.STARTUP_COMMAND.getName()),
            getString("s_key_startup_command"), getString("s_sub_startup_command"),
            getFieldModifier = { Modifier.appTextField() }
        ),

        GroupSettingsItem(
            getString("s_group_server")
        )
    ) + getServerGroupItems()
}

fun getServerGroupItems(): List<SettingsItem> {
    // (I will never learn regex)
    // https://stackoverflow.com/a/36760050
    val ip_regex: Regex = "^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}\$".toRegex()
    // https://stackoverflow.com/a/12968117
    val port_regex: Regex = "^([1-9][0-9]{0,3}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5])\$".toRegex()

    check(ip_regex.matches("127.0.0.1"))
    check(port_regex.matches("1111"))

    return listOf(
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
            },
            getFieldModifier = { Modifier.appTextField() }
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
            },
            getFieldModifier = { Modifier.appTextField() }
        ),

        TextFieldSettingsItem(
            SettingsValueState(DesktopSettings.Key.SERVER_LOCAL_COMMAND.getName()),
            getString("s_key_local_server_command"), getString("s_sub_local_server_command"),
            getFieldModifier = { Modifier.appTextField() }
        ),

        ToggleSettingsItem(
            SettingsValueState(DesktopSettings.Key.SERVER_LOCAL_START_AUTOMATICALLY.getName()),
            getString("s_key_server_local_start_automatically"), getString("s_sub_server_local_start_automatically")
        ),

        ToggleSettingsItem(
            SettingsValueState(DesktopSettings.Key.SERVER_KILL_CHILD_ON_EXIT.getName()),
            getString("s_key_server_kill_child_on_exit"), null
        )
    )
}
