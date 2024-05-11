package com.toasterofbread.spmp.platform.splash

import LocalPlayerState
import ProgramArguments
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
import dev.toastbits.composekit.platform.PlatformContext
import dev.toastbits.composekit.settings.ui.item.SettingsItem
import dev.toastbits.composekit.utils.composable.ShapedIconButton
import dev.toastbits.composekit.platform.PlatformFile
import com.toasterofbread.spmp.model.settings.Settings
import com.toasterofbread.spmp.platform.playerservice.getServerExecutableFilename
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.component.ErrorInfoDisplay
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.category.getServerGroupItems
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.skiko.OS
import org.jetbrains.skiko.hostOs
import java.io.File
import java.io.BufferedReader

private const val LOCAL_SERVER_AUTOSTART_DELAY_MS: Long = 100

private fun ProgramArguments.getServerExecutable(context: PlatformContext): PlatformFile? {
    val server_executable_filename: String? = getServerExecutableFilename()
    val server_executable: PlatformFile? =
        if (server_executable_filename != null && bin_dir != null)
            PlatformFile.fromFile(
                File(bin_dir).resolve(server_executable_filename),
                context
            ).takeIf { it.is_file }
        else null
    return server_executable
}

@Composable
actual fun SplashExtraLoadingContent(modifier: Modifier, arguments: ProgramArguments) {
    val player: PlayerState = LocalPlayerState.current
    val button_colours: ButtonColors = ButtonDefaults.buttonColors(
        containerColor = player.theme.accent,
        contentColor = player.theme.on_accent
    )

    var show: Boolean by remember { mutableStateOf(false) }
    var show_config_dialog: Boolean by remember { mutableStateOf(false) }
    var local_server_error: Throwable? by remember { mutableStateOf(null) }
    var local_server_process: Pair<String, Process>? by remember { mutableStateOf(null) }

    fun startServer(stop_if_running: Boolean, automatic: Boolean) {
        if (automatic && arguments.no_auto_server) {
            return
        }

        local_server_process?.also { process ->
            if (stop_if_running) {
                local_server_process = null
                process.second.destroy()
            }
            return
        }

        try {
            local_server_process = startLocalServer(
                player.context,
                player.settings.desktop.SERVER_PORT.get(),
                arguments.getServerExecutable(player.context)
            ) { result, output ->
                if (local_server_process != null) {
                    local_server_process = null
                    local_server_error = RuntimeException("Local server failed ($result)\n$output")
                }
            }

            if (!automatic && local_server_process == null) {
                local_server_error = RuntimeException(getString("desktop_splash_local_server_command_not_set"))
            }
        }
        catch (e: Throwable) {
            local_server_process = null
            local_server_error = e
        }
    }

    LaunchedEffect(Unit) {
        if (player.settings.desktop.SERVER_LOCAL_START_AUTOMATICALLY.get()) {
            delay(LOCAL_SERVER_AUTOSTART_DELAY_MS)
            startServer(stop_if_running = false, automatic = true)
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
                { startServer(stop_if_running = true, automatic = false) },
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
                            show_throw_button = true,
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
                        item.Item(player.app_page_state.Settings.settings_interface, { _, _ -> }, {}, Modifier)
                    }
                }
            }
        )
    }
}

@OptIn(DelicateCoroutinesApi::class)
@Suppress("NewApi")
private fun startLocalServer(
    context: AppContext,
    port: Int,
    server_executable: PlatformFile?,
    onExit: (Int, String) -> Unit,
): Pair<String, Process>? {
    var command: String = context.settings.desktop.SERVER_LOCAL_COMMAND.get().trim()
    if (command.isEmpty()) {
        val executable_path: String

        if (server_executable != null) {
            executable_path = server_executable.absolute_path
        }
        else {
            val packaged_server_filename: String = getServerExecutableFilename() ?: return null
            val packaged_server: File =
                File(System.getProperty("compose.application.resources.dir"))
                    .resolve(packaged_server_filename)

            if (packaged_server.isFile) {
                executable_path = packaged_server.absolutePath
            }
            else {
                return null
            }
        }

        command =
            when (hostOs) {
                OS.Windows -> "\"" + executable_path + "\""
                else -> executable_path.replace(" ", "\\ ")
            }
    }

    val args: String = "--port $port --no-media-session"

    val args_index: Int = command.indexOf("\$@")
    if (args_index != -1) {
        command = command.substring(0, args_index) + args + command.substring(args_index + 2)
    }
    else {
        command += ' ' + args
    }

    val builder: ProcessBuilder = ProcessBuilder(command.split(' '))
    // builder.inheritIO()

    val process: Process = builder.start()

    Runtime.getRuntime().addShutdownHook(Thread {
        if (context.settings.desktop.SERVER_KILL_CHILD_ON_EXIT.get()) {
            process.destroy()
        }
    })

    GlobalScope.launch {
        withContext(Dispatchers.IO) {
            val reader: BufferedReader = process.getErrorStream().bufferedReader()
            val output: StringBuilder = StringBuilder()

            while (true) {
                val line: String = reader.readLine() ?: break
                output.appendLine(line)
            }

            val result: Int = process.waitFor()
            onExit(result, output.toString())
        }
    }

    return Pair(command, process)
}
