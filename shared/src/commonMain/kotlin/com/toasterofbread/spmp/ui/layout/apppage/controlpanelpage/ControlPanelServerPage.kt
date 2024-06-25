package com.toasterofbread.spmp.ui.layout.apppage.controlpanelpage

import LocalPlayerState
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.toastbits.composekit.platform.composable.ScrollBarLazyColumn
import dev.toastbits.composekit.utils.common.getContrasted
import dev.toastbits.composekit.utils.common.launchSingle
import dev.toastbits.composekit.utils.composable.SubtleLoadingIndicator
import com.toasterofbread.spmp.platform.playerservice.*
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.resources.getStringTODO
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import dev.toastbits.spms.socketapi.shared.SpMsClientInfo
import dev.toastbits.spms.socketapi.shared.SpMsClientType

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
                    Text(getStringTODO("Getting service..."))
                }
                else {
                    Text(getStringTODO("Not connected to server"))
                }
                return@Crossfade
            }

            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                var show_server_info: Boolean by remember { mutableStateOf(false) }

                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text(getString("control_panel_server_connected_to_server"), style = MaterialTheme.typography.titleLarge)

                    SelectionContainer(Modifier.horizontalScroll(rememberScrollState())) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            Text(getString("control_panel_server_ip_address_\$x").replace("\$x", server.ip), style = MaterialTheme.typography.titleSmall, softWrap = false)
                            Text(getString("control_panel_server_port_\$x").replace("\$x", server.port.toString()), style = MaterialTheme.typography.titleSmall, softWrap = false)

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
                                Text(getString("action_close"))
                            }
                        },
                        title = {
                            Text(getString("control_panel_server_connected_to_server"))
                        },
                        text = {
                            SelectionContainer {
                                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Text(getString("control_panel_server_ip_address_\$x").replace("\$x", server.ip))
                                    Text(getString("control_panel_server_port_\$x").replace("\$x", server.ip))
                                    Text(getString("control_panel_server_name_\$x").replace("\$x", server.name))
                                    Text(getString("control_panel_server_device_name_\$x").replace("\$x", server.device_name))
                                    Text(getString("control_panel_server_spms_api_version_\$x").replace("\$x", server.spms_api_version.toString()))
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
                            Text(getString("control_panel_server_connected_clients"), style = MaterialTheme.typography.titleMedium)

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
                                        getString("control_panel_server_connected_clients_loading"),
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
                Button({ player.context.openUrl(getString("server_documentation_url")) }) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.OpenInNew, null)
                        Text(getString("action_open_documentation"))
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
            containerColor = player.theme.vibrant_accent,
            contentColor = player.theme.vibrant_accent.getContrasted()
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
                        Text(getString("control_panel_server_this_client"), style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.width(10.dp))
                    }
                    else if (client.machine_id == machine_id) {
                        Icon(Icons.Default.Dns, null)
                        Text(getString("control_panel_server_same_machine"), style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.width(10.dp))
                    }

                    if (client.player_port != null) {
                        Text(
                            getString("control_panel_server_client_player_port_\$x")
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
                                        Button(
                                            {
                                                player.context.openUrl(client.type.getInfoUrl())
                                                coroutine_scope.launch {
                                                    tooltip_state.dismiss()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = player.theme.vibrant_accent,
                                                contentColor = player.theme.vibrant_accent.getContrasted()
                                            )
                                        ) {
                                            Text(getString("control_panel_server_client_more_info"))
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
