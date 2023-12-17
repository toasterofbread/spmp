package com.toasterofbread.spmp.ui.layout.apppage.controlpanelpage

import LocalPlayerState
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toasterofbread.composekit.platform.composable.ScrollBarLazyColumn
import com.toasterofbread.composekit.utils.common.copy
import com.toasterofbread.composekit.utils.common.getContrasted
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.PlayerState
import com.toasterofbread.spmp.platform.playerservice.ClientServerPlayerService
import com.toasterofbread.spmp.platform.playerservice.SpMsClientInfo
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.resources.getStringTODO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun ControlPanelServerPage(
    modifier: Modifier,
    multiselect_context: MediaItemMultiSelectContext? = null,
    content_padding: PaddingValues = PaddingValues()
) {
    val player: PlayerState = LocalPlayerState.current

    var service_found: Boolean by remember { mutableStateOf(false) }
    var client_player_service: ClientServerPlayerService? by remember { mutableStateOf(null) }
    
    var peers: List<SpMsClientInfo>? by remember { mutableStateOf(null) }
    
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
        peers = null
        peers = client_player_service?.getPeers()?.getOrNull()
    }
    
    Crossfade(client_player_service ?: service_found, modifier) { state ->
        val service: ClientServerPlayerService? = (state as? ClientServerPlayerService)
        val server: ClientServerPlayerService.ServerInfo? = service?.connected_server
        
        if (server == null) {
            Box(Modifier.padding(content_padding)) {
                if (state == false) {
                    Text(getStringTODO("Getting service..."))
                }
                else {
                    Text(getStringTODO("Not connected to server"))
                }
            }
            return@Crossfade
        }
        
        Column(
            Modifier.padding(content_padding.copy(bottom = 0.dp)),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(getString("control_panel_server_connected_to_server"), style = MaterialTheme.typography.titleLarge)
                
                SelectionContainer {
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        Text(getString("control_panel_server_ip_address_\$x").replace("\$x", server.ip), style = MaterialTheme.typography.titleSmall)
                        Text(getString("control_panel_server_port_\$x").replace("\$x", server.port.toString()), style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
            
            Row {
                Column(
                    Modifier.fillMaxWidth(0.5f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(getString("control_panel_server_connected_clients"), style = MaterialTheme.typography.titleMedium)
                    
                    ScrollBarLazyColumn(
                        contentPadding = PaddingValues(bottom = content_padding.calculateBottomPadding()),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        if (peers == null) {
                            item {
                                Text(getString("control_panel_server_connected_clients_loading"), style = MaterialTheme.typography.bodyLarge)
                            }
                            return@ScrollBarLazyColumn
                        }
                        
                        items(peers ?: emptyList()) { peer ->
                            ClientInfoDisplay(peer)
                        }
                    }
                }
                
                Column(Modifier.fillMaxWidth(0.5f)) {

                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClientInfoDisplay(client: SpMsClientInfo, modifier: Modifier = Modifier) {
    val player: PlayerState = LocalPlayerState.current
    val coroutine_scope: CoroutineScope = rememberCoroutineScope()

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

                Row(Modifier.alpha(0.5f), verticalAlignment = Alignment.CenterVertically) {
                    if (client.is_caller) {
                        Icon(Icons.Default.Person, null)
                        Text(getString("control_panel_server_this_client"), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            Spacer(Modifier.fillMaxWidth().weight(1f))
            
            Column(
                Modifier.alpha(0.75f),
                verticalArrangement = Arrangement.spacedBy(5.dp),
                horizontalAlignment = Alignment.End
            ) {
                val tooltip_state: RichTooltipState = remember { RichTooltipState() }

                RichTooltipBox(
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
                    text = { Text(client.type.getInfoText()) },
                    tooltipState = tooltip_state
                ) {
                    IconButton(
                        {
                            coroutine_scope.launch {
                                tooltip_state.show()
                            }
                        },
                        Modifier.tooltipAnchor().size(20.dp)
                    ) {
                        Icon(Icons.Default.Info, null)
                    }
                }
                
                Text(client.type.getName(), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
