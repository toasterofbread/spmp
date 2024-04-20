package com.toasterofbread.spmp.ui.layout.apppage.settingspage.category

import androidx.compose.ui.Modifier
import dev.toastbits.composekit.settings.ui.item.GroupSettingsItem
import dev.toastbits.composekit.settings.ui.item.InfoTextSettingsItem
import dev.toastbits.composekit.settings.ui.item.SettingsItem
import dev.toastbits.composekit.platform.PreferencesProperty
import dev.toastbits.composekit.settings.ui.item.TextFieldSettingsItem
import dev.toastbits.composekit.settings.ui.item.ToggleSettingsItem
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.appTextField
import com.toasterofbread.spmp.platform.AppContext

internal fun getDesktopCategoryItems(context: AppContext): List<SettingsItem> {
    return listOf(
        GroupSettingsItem(
            getString("s_group_desktop_system")
        ),

        TextFieldSettingsItem(
            context.settings.desktop.STARTUP_COMMAND,
            getFieldModifier = { Modifier.appTextField() }
        ),

        ToggleSettingsItem(
            context.settings.desktop.FORCE_SOFTWARE_RENDERER,
        ),

        GroupSettingsItem(
            getString("s_group_server")
        )
    ) + getServerGroupItems(context)
}

fun getServerGroupItems(context: AppContext): List<SettingsItem> {
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
            context.settings.desktop.SERVER_IP_ADDRESS,
            getStringError = { input ->
                if (!ip_regex.matches(input)) {
                    return@TextFieldSettingsItem getString("settings_value_not_ipv4")
                }
                return@TextFieldSettingsItem null
            },
            getFieldModifier = { Modifier.appTextField() }
        ),

        TextFieldSettingsItem(
            context.settings.desktop.SERVER_PORT.getConvertedProperty(
                fromProperty = { it.toString() },
                toProperty = { it.toIntOrNull() ?: 0 }
            ),
            getStringError = { input ->
                if (!port_regex.matches(input)) {
                    return@TextFieldSettingsItem getString("settings_value_not_port")
                }
                return@TextFieldSettingsItem null
            },
            getFieldModifier = { Modifier.appTextField() }
        ),

        TextFieldSettingsItem(
            context.settings.desktop.SERVER_LOCAL_COMMAND,
            getFieldModifier = { Modifier.appTextField() }
        ),

        ToggleSettingsItem(
            context.settings.desktop.SERVER_LOCAL_START_AUTOMATICALLY
        ),

        ToggleSettingsItem(
            context.settings.desktop.SERVER_KILL_CHILD_ON_EXIT
        )
    )
}
