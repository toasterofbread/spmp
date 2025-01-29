package com.toasterofbread.spmp.ui.layout.apppage.controlpanelpage

import LocalPlayerState
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toasterofbread.spmp.platform.playerservice.ClientServerPlayerService
import com.toasterofbread.spmp.platform.playerservice.getInfoText
import com.toasterofbread.spmp.platform.playerservice.getInfoUrl
import com.toasterofbread.spmp.platform.playerservice.getName
import com.toasterofbread.spmp.platform.playerservice.getSpMsMachineId
import com.toasterofbread.spmp.resources.stringResourceTODO
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import dev.toastbits.composekit.components.platform.composable.ScrollBarLazyColumn
import dev.toastbits.composekit.theme.core.vibrantAccent
import dev.toastbits.composekit.util.getContrasted
import dev.toastbits.composekit.util.platform.launchSingle
import dev.toastbits.composekit.components.utils.composable.SubtleLoadingIndicator
import dev.toastbits.spms.socketapi.shared.SpMsClientInfo
import dev.toastbits.spms.socketapi.shared.SpMsClientType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.action_close
import spmp.shared.generated.resources.action_open_documentation
import spmp.shared.generated.resources.control_panel_server_client_more_info
import spmp.shared.generated.resources.`control_panel_server_client_player_port_$x`
import spmp.shared.generated.resources.control_panel_server_connected_clients
import spmp.shared.generated.resources.control_panel_server_connected_clients_loading
import spmp.shared.generated.resources.control_panel_server_connected_to_server
import spmp.shared.generated.resources.`control_panel_server_device_name_$x`
import spmp.shared.generated.resources.`control_panel_server_ip_address_$x`
import spmp.shared.generated.resources.`control_panel_server_name_$x`
import spmp.shared.generated.resources.`control_panel_server_port_$x`
import spmp.shared.generated.resources.control_panel_server_same_machine
import spmp.shared.generated.resources.`control_panel_server_spms_api_version_$x`
import spmp.shared.generated.resources.control_panel_server_this_client
import spmp.shared.generated.resources.server_documentation_url

@Composable
fun ControlPanelServerPage(
    modifier: Modifier,
    multiselect_context: MediaItemMultiSelectContext? = null,
    content_padding: PaddingValues = PaddingValues()
) {
    val player: PlayerState = LocalPlayerState.current
    val coroutine_scope: CoroutineScope = rememberCoroutineScope()

    var service_found: Boolean by remember { mutableStateOf(false) }
    var client_player_service: ClientServerPlayerService? by remember { mutableStateOf(null) }

    var peers: List<SpMsClientInfo>? by remember { mutableStateOf(null) }
    suspend fun reloadPeers() {
        peers = null
        peers = client_player_service?.getPeers()?.getOrNull()
    }

    LaunchedEffect(Unit) {
        player.interactService { service: Any ->
            service_found = true

            if (service !is ClientServerPlayerService) {
                client_player_service = null
                return@interactService
            }

            client_player_service = service
        }
    }

    LaunchedEffect(client_player_service) {
        reloadPeers()
    }

    Column(modifier.padding(content_padding)) {
        Crossfade(client_player_service ?: service_found, Modifier.fillMaxHeight().weight(1f)) { state ->
            val service: ClientServerPlayerService? = (state as? ClientServerPlayerService)
            val server: ClientServerPlayerService.ServerInfo? = service?.connected_server

            if (server == null) {
                if (state == false) {
                    Text(stringResourceTODO("Getting service..."))
                }
                else {
                    Text(stringResourceTODO("Not connected to server"))
                }
                return@Crossfade
            }

            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                var show_server_info: Boolean by remember { mutableStateOf(false) }

                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text(stringResource(Res.string.control_panel_server_connected_to_server), style = MaterialTheme.typography.titleLarge)

                    SelectionContainer(Modifier.horizontalScroll(rememberScrollState())) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            Text(stringResource(Res.string.`control_panel_server_ip_address_$x`).replace("\$x", server.ip), style = MaterialTheme.typography.titleSmall, softWrap = false)
                            Text(stringResource(Res.string.`control_panel_server_port_$x`).replace("\$x", server.port.toString()), style = MaterialTheme.typography.titleSmall, softWrap = false)

                            IconButton(
                                { show_server_info = !show_server_info },
                                Modifier.size(25.dp)
                            ) {
                                Icon(Icons.Default.MoreVert, null)
                            }
                        }
                    }
                }

                if (show_server_info) {
                    AlertDialog(
                        onDismissRequest = { show_server_info = false },
                        confirmButton = {
                            Button({ show_server_info = false })  {
                                Text(stringResource(Res.string.action_close))
                            }
                        },
                        title = {
                            Text(stringResource(Res.string.control_panel_server_connected_to_server))
                        },
                        text = {
                            SelectionContainer {
                                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Text(stringResource(Res.string.`control_panel_server_ip_address_$x`).replace("\$x", server.ip))
                                    Text(stringResource(Res.string.`control_panel_server_port_$x`).replace("\$x", server.ip))
                                    Text(stringResource(Res.string.`control_panel_server_name_$x`).replace("\$x", server.name))
                                    Text(stringResource(Res.string.`control_panel_server_device_name_$x`).replace("\$x", server.device_name))
                                    Text(stringResource(Res.string.`control_panel_server_spms_api_version_$x`).replace("\$x", server.spms_api_version.toString()))
                                }
                            }
                        }
                    )
                }

                Row {
                    Column(
                        Modifier.fillMaxWidth(0.5f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(Res.string.control_panel_server_connected_clients), style = MaterialTheme.typography.titleMedium)

                            Spacer(Modifier.fillMaxWidth().weight(1f))

                            var loading: Boolean by remember { mutableStateOf(false) }
                            IconButton({
                                if (loading) {
                                    return@IconButton
                                }

                                coroutine_scope.launchSingle {
                                    loading = true
                                    reloadPeers()
                                    loading = false
                                }
                            }) {
                                Crossfade(loading) {
                                    if (it) {
                                        SubtleLoadingIndicator()
                                    }
                                    else {
                                        Icon(Icons.Default.Refresh, null)
                                    }
                                }
                            }
                        }

                        ScrollBarLazyColumn(
                            Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (peers == null) {
                                item {
                                    Text(
                                        stringResource(Res.string.control_panel_server_connected_clients_loading),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                                return@ScrollBarLazyColumn
                            }

                            items(peers ?: emptyList()) { peer ->
                                if (peer.type == SpMsClientType.SERVER) {
                                    return@items
                                }

                                ClientInfoDisplay(peer)
                            }
                        }
                    }

                    Column(Modifier.fillMaxWidth(0.5f)) {
                        // TODO
                    }
                }
            }
        }

        Row(Modifier.align(Alignment.End)) {
            if (player.context.canOpenUrl()) {
                val server_documentation_url: String = stringResource(Res.string.server_documentation_url)
                Button({ player.context.openUrl(server_documentation_url) }) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, null)
                        Text(stringResource(Res.string.action_open_documentation))
                    }
                }
            }
        }
    }
}

@Composable
private fun ClientInfoDisplay(client: SpMsClientInfo, modifier: Modifier = Modifier) {
    val player: PlayerState = LocalPlayerState.current
    val coroutine_scope: CoroutineScope = rememberCoroutineScope()

    val machine_id: String = remember { getSpMsMachineId(player.context) }

    Card(
        modifier,
        colors = CardDefaults.cardColors(
            containerColor = player.theme.vibrantAccent,
            contentColor = player.theme.vibrantAccent.getContrasted()
        )
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(client.name, fontSize = 18.sp)

                Row(
                    Modifier.alpha(0.75f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (client.is_caller) {
                        Icon(Icons.Default.Person, null)
                        Text(stringResource(Res.string.control_panel_server_this_client), style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.width(10.dp))
                    }
                    else if (client.machine_id == machine_id) {
                        Icon(Icons.Default.Dns, null)
                        Text(stringResource(Res.string.control_panel_server_same_machine), style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.width(10.dp))
                    }

                    if (client.player_port != null) {
                        Text(
                            stringResource(Res.string.`control_panel_server_client_player_port_$x`)
                                .replace("\$x", client.player_port.toString()),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
            Spacer(Modifier.fillMaxWidth().weight(1f))

            Column(
                Modifier.alpha(0.75f),
                verticalArrangement = Arrangement.spacedBy(5.dp),
                horizontalAlignment = Alignment.End
            ) {
                val tooltip_state: TooltipState = remember { TooltipState() }

                TooltipBox(
                    tooltip = {
                        RichTooltip(
                            title = { Text(client.type.getName(), style = MaterialTheme.typography.titleMedium) },
                            action = {
                                if (player.context.canOpenUrl()) {
                                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomEnd) {
                                        val info_url: String = client.type.getInfoUrl()
                                        Button(
                                            {
                                                player.context.openUrl(info_url)
                                                coroutine_scope.launch {
                                                    tooltip_state.dismiss()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = player.theme.vibrantAccent,
                                                contentColor = player.theme.vibrantAccent.getContrasted()
                                            )
                                        ) {
                                            Text(stringResource(Res.string.control_panel_server_client_more_info))
                                        }
                                    }
                                }
                            },
                            text = { Text(client.type.getInfoText()) }
                        )
                    },
                    state = tooltip_state,
                    positionProvider = TooltipDefaults.rememberRichTooltipPositionProvider()
                ) {
                    IconButton(
                        {
                            coroutine_scope.launch {
                                tooltip_state.show()
                            }
                        },
                        Modifier.size(20.dp)
                    ) {
                        Icon(Icons.Default.Info, null)
                    }
                }

                Text(client.type.getName(), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
