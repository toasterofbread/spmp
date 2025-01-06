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
import dev.toastbits.composekit.components.utils.composable.ShapedIconButton
import com.toasterofbread.spmp.model.settings.Settings
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.category.getServerGroupItems
import com.toasterofbread.spmp.ui.component.ErrorInfoDisplay
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.platform.playerservice.LocalServer
import dev.toastbits.composekit.components.utils.composable.ShapedIconButton
import dev.toastbits.composekit.settingsitem.domain.SettingsItem
import LocalProgramArguments
import ProgramArguments
import dev.toastbits.composekit.theme.core.onAccent
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.loading_splash_button_configure_connection
import spmp.shared.generated.resources.server_info_url
import spmp.shared.generated.resources.action_close
import spmp.shared.generated.resources.loading_splash_title_configure_server_connection

private const val LOCAL_SERVER_AUTOSTART_DELAY_MS: Long = 100

@Composable
fun SplashExtraLoadingContent(item_modifier: Modifier) {
    val player: PlayerState = LocalPlayerState.current
    var show_config_dialog: Boolean by remember { mutableStateOf(false) }

    val button_colours: ButtonColors =
        ButtonDefaults.buttonColors(
            containerColor = player.theme.accent,
            contentColor = player.theme.onAccent
        )

    Button(
        { show_config_dialog = true },
        colors = button_colours,
        modifier = item_modifier
    ) {
        Text(stringResource(Res.string.loading_splash_button_configure_connection))
    }

    if (player.context.canOpenUrl()) {
        val server_info_url: String = stringResource(Res.string.server_info_url)

        ShapedIconButton(
            {
                player.context.openUrl(server_info_url)
            },
            colours = IconButtonDefaults.iconButtonColors(
                containerColor = player.theme.accent,
                contentColor = player.theme.onAccent
            ),
            modifier = item_modifier
        ) {
            Icon(Icons.Default.Info, null)
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
                    Text(stringResource(Res.string.action_close))
                }
            },
            title = {
                Text(stringResource(Res.string.loading_splash_title_configure_server_connection))
            },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    items(settings_items) { item ->
                        item.Item(Modifier)
                    }
                }
            }
        )
    }
}
