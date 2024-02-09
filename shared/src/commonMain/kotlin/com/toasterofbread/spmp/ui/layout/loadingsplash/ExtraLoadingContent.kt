package com.toasterofbread.spmp.ui.layout.loadingsplash

import LocalPlayerState
import ProgramArguments
import SpMp
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toasterofbread.composekit.settings.ui.item.SettingsItem
import com.toasterofbread.composekit.utils.composable.ShapedIconButton
import com.toasterofbread.spmp.model.settings.Settings
import com.toasterofbread.spmp.model.settings.category.ServerSettings
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.PlayerState
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.category.getServerCategoryItems

private const val LOCAL_SERVER_AUTOSTART_DELAY_MS: Long = 100

@Composable
fun SplashExtraLoadingContent(modifier: Modifier) {
    val player: PlayerState = LocalPlayerState.current
    val arguments: ProgramArguments = player.context.launch_arguments

    val button_colours: ButtonColors = ButtonDefaults.buttonColors(
        containerColor = player.theme.accent,
        contentColor = player.theme.on_accent
    )

    var show: Boolean by remember { mutableStateOf(false) }
    var show_config_dialog: Boolean by remember { mutableStateOf(false) }
//    var local_server_error: Throwable? by remember { mutableStateOf(null) }
//    var local_server_process: LocalServerProcess? by remember { mutableStateOf(null) }

    val external_server_mode: Boolean by ServerSettings.Key.ENABLE_EXTERNAL_SERVER_MODE.rememberMutableState()

    Column(modifier.animateContentSize().fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
//            if (local_server_process != null || LocalServer.isAvailable()) {
//                Button(
//                    { startServer(stop_if_running = true, automatic = false) },
//                    colors = button_colours
//                ) {
//                    Crossfade(local_server_process) { process ->
//                        if (process == null) {
//                            Text(getString("desktop_splash_button_start_server"))
//                        }
//                        else {
//                            Text(getString("desktop_splash_button_stop_process"))
//                        }
//                    }
//                }
//            }

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

//        Crossfade(local_server_error ?: local_server_process as Any?) { state ->
//            if (state != null) {
//                Column(
//                    Modifier.padding(top = 20.dp),
//                    horizontalAlignment = Alignment.CenterHorizontally,
//                    verticalArrangement = Arrangement.spacedBy(10.dp)
//                ) {
//                    if (state is Throwable) {
//                        Text(getString("error_on_server_command_execution"))
//                        ErrorInfoDisplay(
//                            state,
//                            show_throw_button = true,
//                            onDismiss = { local_server_error = null },
//                            modifier = Modifier.fillMaxWidth()
//                        )
//                    }
//                    else if (state is LocalServerProcess) {
//                        Text(getString("desktop_splash_process_running_with_command_\$x").replace("\$x", state.launch_command))
//                    }
//                }
//            }
//        }
    }

    if (show_config_dialog) {
        val settings_items: List<SettingsItem> = remember { getServerCategoryItems() }

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
