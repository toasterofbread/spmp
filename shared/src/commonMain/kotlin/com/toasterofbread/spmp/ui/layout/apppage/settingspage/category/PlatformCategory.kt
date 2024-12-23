package com.toasterofbread.spmp.ui.layout.apppage.settingspage.category

import LocalPlayerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import dev.toastbits.composekit.settingsitem.presentation.ui.component.item.GroupSettingsItem
import dev.toastbits.composekit.settingsitem.presentation.ui.component.item.InfoTextSettingsItem
import dev.toastbits.composekit.settingsitem.domain.SettingsItem
import dev.toastbits.composekit.util.platform.Platform
import dev.toastbits.composekit.settingsitem.presentation.ui.component.item.TextFieldSettingsItem
import dev.toastbits.composekit.settingsitem.presentation.ui.component.item.ToggleSettingsItem
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.appTextField
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.playerservice.PlatformInternalPlayerService
import com.toasterofbread.spmp.platform.playerservice.PlatformExternalPlayerService
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import LocalProgramArguments
import ProgramArguments
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.toastbits.composekit.settingsitem.presentation.util.getConvertedProperty
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.s_group_desktop_system
import spmp.shared.generated.resources.s_group_server
import spmp.shared.generated.resources.s_info_server
import spmp.shared.generated.resources.settings_value_not_ipv4_or_domain
import spmp.shared.generated.resources.settings_value_not_port

internal fun getPlatformCategoryItems(context: AppContext): List<SettingsItem> {
    val platform_items: List<SettingsItem> =
        when (Platform.current) {
            Platform.ANDROID -> getAndroidGroupItems(context)
            Platform.DESKTOP -> getDesktopGroupItems(context)
            Platform.WEB -> getWebGroupItems(context)
        }

    return platform_items + getServerGroupItems(context)
}

private fun getAndroidGroupItems(context: AppContext): List<SettingsItem> =
    listOf()

private fun getDesktopGroupItems(context: AppContext): List<SettingsItem> =
    listOf(
        GroupSettingsItem(Res.string.s_group_desktop_system),

        TextFieldSettingsItem(
            context.settings.Platform.STARTUP_COMMAND,
            getFieldModifier = { Modifier.appTextField() }
        ),

        ToggleSettingsItem(
            context.settings.Platform.FORCE_SOFTWARE_RENDERER,
        ),

        GroupSettingsItem(Res.string.s_group_server)
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
        InfoTextSettingsItem(Res.string.s_info_server),

        ToggleSettingsItem(
            context.settings.Platform.ENABLE_EXTERNAL_SERVER_MODE,
            getEnabled = {
                val reason: LocalServerUnavailabilityReason? = getLocalServerUnavailabilityReason()
                return@ToggleSettingsItem reason != null && reason.reason == null
            },
            getValueOverride = {
                if (getLocalServerUnavailabilityReason()?.reason != null) {
                    true
                }
                else {
                    null
                }
            },
            getSubtitleOverride = {
                getLocalServerUnavailabilityReason()?.reason
            }
        ).takeIf { !Platform.DESKTOP.isCurrent() },

        ToggleSettingsItem(context.settings.Platform.EXTERNAL_SERVER_MODE_UI_ONLY).takeIf { PlatformExternalPlayerService.playsAudio() },

        TextFieldSettingsItem(
            context.settings.Platform.SERVER_IP_ADDRESS,
            getStringErrorProvider = {
                val settings_value_not_ipv4_or_domain: String = stringResource(Res.string.settings_value_not_ipv4_or_domain)
                return@TextFieldSettingsItem { input ->
                    if (!ip_regex.matches(input) && !domain_regex.matches(input)) settings_value_not_ipv4_or_domain
                    else null
                }
            },
            getFieldModifier = { Modifier.appTextField() }
        ),

        TextFieldSettingsItem(
            context.settings.Platform.SERVER_PORT.getConvertedProperty(
                fromProperty = { it.toString() },
                toProperty = { it.toIntOrNull() ?: 0 }
            ),
            getStringErrorProvider = {
                val settings_value_not_port: String = stringResource(Res.string.settings_value_not_port)
                return@TextFieldSettingsItem { input ->
                    if (!port_regex.matches(input)) settings_value_not_port
                    else null
                }
            },
            getFieldModifier = { Modifier.appTextField() }
        ),

        TextFieldSettingsItem(
            context.settings.Platform.SERVER_LOCAL_COMMAND,
            getFieldModifier = { Modifier.appTextField() }
        ).takeIf { Platform.DESKTOP.isCurrent() },

        ToggleSettingsItem(
            context.settings.Platform.SERVER_LOCAL_START_AUTOMATICALLY
        ).takeIf { Platform.DESKTOP.isCurrent() }
    )
}

private fun getWebGroupItems(context: AppContext): List<SettingsItem> =
    listOf()

@Composable
private fun getLocalServerUnavailabilityReason(): LocalServerUnavailabilityReason? {
    val player: PlayerState = LocalPlayerState.current
    val launch_arguments: ProgramArguments = LocalProgramArguments.current

    var reason: LocalServerUnavailabilityReason? by remember { mutableStateOf(null) }

    LaunchedEffect(Unit) {
        reason = LocalServerUnavailabilityReason(PlatformInternalPlayerService.getUnavailabilityReason(player.context, launch_arguments))
    }

    return reason
}

private data class LocalServerUnavailabilityReason(val reason: String?)
