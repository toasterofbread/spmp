package com.toasterofbread.spmp.ui.layout.apppage.settingspage.category

import LocalPlayerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.toasterofbread.composekit.settings.ui.item.InfoTextSettingsItem
import com.toasterofbread.composekit.settings.ui.item.SettingsItem
import com.toasterofbread.composekit.settings.ui.item.SettingsValueState
import com.toasterofbread.composekit.settings.ui.item.TextFieldSettingsItem
import com.toasterofbread.composekit.settings.ui.item.ToggleSettingsItem
import com.toasterofbread.spmp.model.settings.category.ServerSettings
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.appTextField
import com.toasterofbread.spmp.platform.playerservice.PlatformInternalPlayerService

@Composable
private fun isInternalServerAvailable(): Boolean =
    PlatformInternalPlayerService.isAvailable(LocalPlayerState.current.context)

internal fun getServerCategoryItems(): List<SettingsItem> {
    // (I will never learn regex)
    // https://stackoverflow.com/a/36760050
    val ip_regex: Regex = "^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}\$".toRegex()
    // https://stackoverflow.com/a/12968117
    val port_regex: Regex = "^([1-9][0-9]{0,3}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5])\$".toRegex()

    check(ip_regex.matches("127.0.0.1"))
    check(port_regex.matches("1111"))

    return listOfNotNull(
        InfoTextSettingsItem(
            getString("s_info_server")
        ),

        ToggleSettingsItem(
            SettingsValueState(ServerSettings.Key.ENABLE_EXTERNAL_SERVER_MODE.getName()),
            getString("s_key_enable_external_server_mode"), getString("s_sub_enable_external_server_mode"),
            getEnabled = {
                isInternalServerAvailable()
            },
            getValueOverride = {
                if (!isInternalServerAvailable()) {
                    true
                }
                else {
                    null
                }
            },
            getSubtitleOverride = {
                if (!isInternalServerAvailable()) {
                    getString("s_sub_always_enabled_because_spms_not_packaged")
                }
                else {
                    null
                }
            }
        ),

        TextFieldSettingsItem(
            SettingsValueState(ServerSettings.Key.EXTERNAL_SERVER_IP_ADDRESS.getName()),
            getString("s_key_external_server_ip"), null,
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
                ServerSettings.Key.SERVER_PORT.getName(),
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

        ToggleSettingsItem(
            SettingsValueState(ServerSettings.Key.KILL_CHILD_SERVER_ON_EXIT.getName()),
            getString("s_key_kill_child_server_on_exit"), null
        )
    )
}
