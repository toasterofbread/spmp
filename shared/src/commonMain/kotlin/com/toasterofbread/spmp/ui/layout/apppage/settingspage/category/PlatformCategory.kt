package com.toasterofbread.spmp.ui.layout.apppage.settingspage.category

import LocalPlayerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import dev.toastbits.composekit.settings.ui.item.GroupSettingsItem
import dev.toastbits.composekit.settings.ui.item.InfoTextSettingsItem
import dev.toastbits.composekit.settings.ui.item.SettingsItem
import dev.toastbits.composekit.platform.PreferencesProperty
import dev.toastbits.composekit.platform.Platform
import dev.toastbits.composekit.settings.ui.item.TextFieldSettingsItem
import dev.toastbits.composekit.settings.ui.item.ToggleSettingsItem
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.appTextField
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.playerservice.PlatformInternalPlayerService
import com.toasterofbread.spmp.platform.playerservice.PlatformExternalPlayerService
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import LocalProgramArguments
import ProgramArguments

internal fun getPlatformCategoryItems(context: AppContext): List<SettingsItem> {
    val platform_items: List<SettingsItem> =
        when (Platform.current) {
            Platform.ANDROID -> getAndroidGroupItems(context)
            Platform.DESKTOP -> getDesktopGroupItems(context)
        }

    return platform_items + getServerGroupItems(context)
}

private fun getAndroidGroupItems(context: AppContext): List<SettingsItem> =
    listOf()

private fun getDesktopGroupItems(context: AppContext): List<SettingsItem> =
    listOf(
        GroupSettingsItem(
            getString("s_group_desktop_system")
        ),

        TextFieldSettingsItem(
            context.settings.platform.STARTUP_COMMAND,
            getFieldModifier = { Modifier.appTextField() }
        ),

        ToggleSettingsItem(
            context.settings.platform.FORCE_SOFTWARE_RENDERER,
        ),

        GroupSettingsItem(
            getString("s_group_server")
        )
    )

fun getServerGroupItems(context: AppContext): List<SettingsItem> {
    // (I will never learn regex)
    // https://stackoverflow.com/a/36760050
    val ip_regex: Regex = "^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}\$".toRegex()
    // https://regexr.com/3au3g
    val domain_regex: Regex = "(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z0-9][a-z0-9-]{0,61}[a-z0-9]".toRegex()
    // https://stackoverflow.com/a/12968117
    val port_regex: Regex = "^([1-9][0-9]{0,3}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5])\$".toRegex()

    check(ip_regex.matches("127.0.0.1"))
    check(!ip_regex.matches("0.0"))
    check(!ip_regex.matches("a.b.c.d"))

    check(domain_regex.matches("domain.name"))
    check(!domain_regex.matches("http://domain.name"))
    check(!domain_regex.matches("domain.name:port"))
    check(!domain_regex.matches("domain.name/path"))

    check(port_regex.matches("1111"))
    check(!port_regex.matches("a"))

    return listOfNotNull(
        InfoTextSettingsItem(
            getString("s_info_server")
        ),

        ToggleSettingsItem(
            context.settings.platform.ENABLE_EXTERNAL_SERVER_MODE,
            getEnabled = {
                getLocalServerUnavailabilityReason() == null
            },
            getValueOverride = {
                if (getLocalServerUnavailabilityReason() != null) {
                    true
                }
                else {
                    null
                }
            },
            getSubtitleOverride = {
                getLocalServerUnavailabilityReason()
            }
        ).takeIf { !Platform.DESKTOP.isCurrent() },

        ToggleSettingsItem(context.settings.platform.EXTERNAL_SERVER_MODE_UI_ONLY).takeIf { PlatformExternalPlayerService.playsAudio() },

        TextFieldSettingsItem(
            context.settings.platform.SERVER_IP_ADDRESS,
            getStringError = { input ->
                if (!ip_regex.matches(input) && !domain_regex.matches(input)) {
                    return@TextFieldSettingsItem getString("settings_value_not_ipv4_or_domain")
                }
                return@TextFieldSettingsItem null
            },
            getFieldModifier = { Modifier.appTextField() }
        ),

        TextFieldSettingsItem(
            context.settings.platform.SERVER_PORT.getConvertedProperty(
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
            context.settings.platform.SERVER_LOCAL_COMMAND,
            getFieldModifier = { Modifier.appTextField() }
        ).takeIf { Platform.DESKTOP.isCurrent() },

        ToggleSettingsItem(
            context.settings.platform.SERVER_LOCAL_START_AUTOMATICALLY
        ).takeIf { Platform.DESKTOP.isCurrent() }
    )
}

@Composable
private fun getLocalServerUnavailabilityReason(): String? {
    val player: PlayerState = LocalPlayerState.current
    val launch_arguments: ProgramArguments = LocalProgramArguments.current
    return remember { PlatformInternalPlayerService.getUnavailabilityReason(player.context, launch_arguments) }
}
