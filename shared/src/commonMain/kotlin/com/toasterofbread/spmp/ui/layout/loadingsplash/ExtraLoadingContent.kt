package com.toasterofbread.spmp.ui.layout.loadingsplash

import LocalPlayerState
import SpMp
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.toastbits.composekit.utils.composable.ShapedIconButton
import com.toasterofbread.spmp.model.settings.Settings
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.category.getServerGroupItems
import com.toasterofbread.spmp.ui.component.ErrorInfoDisplay
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.platform.playerservice.LocalServerProcess
import com.toasterofbread.spmp.platform.playerservice.LocalServer
import dev.toastbits.composekit.utils.composable.ShapedIconButton
import dev.toastbits.composekit.settings.ui.item.SettingsItem
import LocalProgramArguments
import ProgramArguments

private const val LOCAL_SERVER_AUTOSTART_DELAY_MS: Long = 100

@Composable
fun SplashExtraLoadingContent(modifier: Modifier) {
    val player: PlayerState = LocalPlayerState.current
    val launch_arguments: ProgramArguments = LocalProgramArguments.current

    val button_colours: ButtonColors =
        ButtonDefaults.buttonColors(
            containerColor = player.theme.accent,
            contentColor = player.theme.on_accent
        )

    var show: Boolean by remember { mutableStateOf(false) }
    var show_config_dialog: Boolean by remember { mutableStateOf(false) }
    var local_server_error: Throwable? by remember { mutableStateOf(null) }
    var local_server_process: LocalServerProcess? by remember { mutableStateOf(null) }

    val external_server_mode: Boolean by player.settings.platform.ENABLE_EXTERNAL_SERVER_MODE.observe()

    fun startServer(stop_if_running: Boolean, automatic: Boolean) {
        if (automatic && launch_arguments.no_auto_server) {
            return
        }

        local_server_process?.also { process ->
            if (stop_if_running) {
                local_server_process = null
                process.process.destroy()
            }
            return
        }

        try {
            local_server_process =
                LocalServer.startLocalServer(
                    player.context,
                    launch_arguments,
                    player.settings.platform.SERVER_PORT.get()
                ) { result, output ->
                    if (local_server_process != null) {
                        local_server_process = null
                        local_server_error = RuntimeException("Local server failed ($result)\n$output")
                    }
                }

            if (!automatic && local_server_process == null) {
                local_server_error = RuntimeException(getString("loading_splash_local_server_command_not_set"))
            }
        }
        catch (e: Throwable) {
            local_server_process = null
            local_server_error = e
        }
    }

    Column(modifier.animateContentSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (local_server_process != null || LocalServer.canStartLocalServer()) {
                Button(
                    { startServer(stop_if_running = true, automatic = false) },
                    colors = button_colours,
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    Crossfade(local_server_process) { process ->
                        if (process == null) {
                            Text(getString("loading_splash_button_start_server"))
                        }
                        else {
                            Text(getString("loading_splash_button_stop_process"))
                        }
                    }
                }
            }

            Button(
                { show_config_dialog = true },
                colors = button_colours,
                modifier = Modifier.align(Alignment.CenterVertically)
            ) {
                Text(getString("loading_splash_button_configure_connection"))
            }

            if (player.context.canOpenUrl()) {
                ShapedIconButton(
                    {
                        player.context.openUrl(getString("server_info_url"))
                    },
                    colours = IconButtonDefaults.iconButtonColors(
                        containerColor = player.theme.accent,
                        contentColor = player.theme.on_accent
                    ),
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    Icon(Icons.Default.Info, null)
                }
            }
        }

        Crossfade(local_server_error ?: local_server_process as Any?) { state ->
            if (state != null) {
                Column(
                    Modifier.padding(top = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (state is Throwable) {
                        Text(getString("error_on_server_command_execution"))
                        ErrorInfoDisplay(
                            state,
                            show_throw_button = true,
                            onDismiss = { local_server_error = null }
                        )
                    }
                    else if (state is LocalServerProcess) {
                        Text(getString("loading_splash_process_running_with_command_\$x").replace("\$x", state.launch_command))
                    }
                }
            }
        }
    }

    if (show_config_dialog) {
        val settings_items: List<SettingsItem> = remember { getServerGroupItems(player.context) }

        AlertDialog(
            onDismissRequest = { show_config_dialog = false },
            confirmButton = {
                Button(
                    { show_config_dialog = false },
                    colors = button_colours
                ) {
                    Text(getString("action_close"))
                }
            },
            title = {
                Text(getString("loading_splash_title_configure_server_connection"))
            },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    items(settings_items) { item ->
                        item.Item(player.app_page_state.Settings.settings_interface, { _, _ -> }, {}, Modifier)
                    }
                }
            }
        )
    }
}