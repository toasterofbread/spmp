package com.toasterofbread.spmp.platform.splash

import LocalPlayerState
import SpMp
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toasterofbread.composekit.settings.ui.item.SettingsItem
import com.toasterofbread.composekit.utils.composable.ShapedIconButton
import com.toasterofbread.spmp.model.settings.Settings
import com.toasterofbread.spmp.model.settings.category.DesktopSettings
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.component.ErrorInfoDisplay
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.PlayerState
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.category.getServerGroupItems
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val LOCAL_SERVER_AUTOSTART_DELAY_MS: Long = 100

@Composable
actual fun SplashExtraLoadingContent(modifier: Modifier) {
    val player: PlayerState = LocalPlayerState.current
    val button_colours: ButtonColors = ButtonDefaults.buttonColors(
        containerColor = player.theme.accent,
        contentColor = player.theme.on_accent
    )

    var show: Boolean by remember { mutableStateOf(false) }
    var show_config_dialog: Boolean by remember { mutableStateOf(false) }
    var local_server_error: Throwable? by remember { mutableStateOf(null) }
    var local_server_process: Pair<String, Process>? by remember { mutableStateOf(null) }

    fun startServer(stop_if_running: Boolean) {
        local_server_process?.also { process ->
            if (stop_if_running) {
                local_server_process = null
                process.second.destroy()
            }
            return
        }

        try {
            local_server_process = startLocalServer(DesktopSettings.Key.SERVER_PORT.get()) {
                if (local_server_process != null) {
                    local_server_process = null
                    local_server_error = RuntimeException(it.toString())
                }
            }
        }
        catch (e: Throwable) {
            local_server_process = null
            local_server_error = e
        }
    }

    LaunchedEffect(Unit) {
        if (DesktopSettings.Key.SERVER_LOCAL_START_AUTOMATICALLY.get()) {
            delay(LOCAL_SERVER_AUTOSTART_DELAY_MS)
            startServer(stop_if_running = false)
            delay(500)
        }
        show = true
    }

    if (!show) {
        return
    }

    Column(modifier.animateContentSize().fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                { startServer(stop_if_running = true) },
                colors = button_colours
            ) {
                Crossfade(local_server_process) { process ->
                    if (process == null) {
                        Text(getString("desktop_splash_button_start_server"))
                    }
                    else {
                        Text(getString("desktop_splash_button_stop_process"))
                    }
                }
            }

            Button(
                { show_config_dialog = true },
                colors = button_colours
            ) {
                Text(getString("desktop_splash_button_configure_connection"))
            }

            if (player.context.canOpenUrl()) {
                ShapedIconButton(
                    {
                        player.context.openUrl(getString("server_info_url"))
                    },
                    colours = IconButtonDefaults.iconButtonColors(
                        containerColor = player.theme.accent,
                        contentColor = player.theme.on_accent
                    )
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
                            show_throw_button = false,
                            onDismiss = { local_server_error = null },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    else if (state is Pair<*, *>) {
                        Text(getString("desktop_splash_process_running_with_command_\$x").replace("\$x", state.first as String))
                    }
                }
            }
        }
    }

    if (show_config_dialog) {
        val settings_items: List<SettingsItem> = remember { getServerGroupItems() }

        LaunchedEffect(settings_items) {
            for (item in settings_items) {
                item.setEnableAutosave(false)
            }
        }

        AlertDialog(
            onDismissRequest = { show_config_dialog = false },
            confirmButton = {
                Button(
                    {
                        player.context.getPrefs().edit {
                            for (item in settings_items) {
                                with (item) {
                                    saveItem()
                                }
                            }
                        }
                        show_config_dialog = false
                    },
                    colors = button_colours
                ) {
                    Text(getString("action_save"))
                }
            },
            dismissButton = {
                Button(
                    { show_config_dialog = false },
                    colors = button_colours
                ) {
                    Text(getString("action_close"))
                }
            },
            title = {
                Text(getString("desktop_splash_title_configure_server_connection"))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    for (item in settings_items) {
                        item.initialise(SpMp.prefs, Settings::provideDefault)
                        item.Item(player.app_page_state.Settings.settings_interface, { _, _ -> }, {}, Modifier)
                    }
                }
            }
        )
    }
}

@OptIn(DelicateCoroutinesApi::class)
@Suppress("NewApi")
private fun startLocalServer(port: Int, onExit: (Int) -> Unit): Pair<String, Process>? {
    var command: String = DesktopSettings.Key.SERVER_LOCAL_COMMAND.get<String>().trim()
    if (command.isEmpty()) {
        return null
    }

    val args: String = "--port $port"

    val args_index: Int = command.indexOf("\$@")
    if (args_index != -1) {
        command = command.substring(0, args_index) + args + command.substring(args_index + 2)
    }
    else {
        command += ' ' + args
    }

    val builder: ProcessBuilder = ProcessBuilder(command.split(' '))
    builder.inheritIO()

    val process: Process = builder.start()

    Runtime.getRuntime().addShutdownHook(Thread {
        if (DesktopSettings.Key.SERVER_KILL_CHILD_ON_EXIT.get()) {
            process.destroy()
        }
    })

    GlobalScope.launch {
        withContext(Dispatchers.IO) {
            onExit(process.waitFor())
        }
    }

    return Pair(command, process)
}
