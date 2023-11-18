package com.toasterofbread.spmp.ui.layout.apppage.settingspage

import com.toasterofbread.composekit.settings.ui.item.SettingsItem
import com.toasterofbread.composekit.settings.ui.item.SettingsItemInfoText
import com.toasterofbread.composekit.settings.ui.item.SettingsTextFieldItem
import com.toasterofbread.composekit.settings.ui.item.SettingsValueState
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.model.Settings

internal fun getServerCategory(): List<SettingsItem> {
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
            SettingsValueState(Settings.KEY_SERVER_IP.name),
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
                Settings.KEY_SERVER_PORT.name,
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
        )
    )
}
