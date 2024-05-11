package com.toasterofbread.spmp.ui.layout.apppage.searchpage

import LocalPlayerState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import dev.toastbits.composekit.settings.ui.item.SettingsItem

@Composable
fun SearchSettingsDialog(modifier: Modifier = Modifier, close: () -> Unit) {
    val player: PlayerState = LocalPlayerState.current
    val settings_items: List<SettingsItem> = remember { player.settings.search.getItems() }

    AlertDialog(
        onDismissRequest = close,
        confirmButton = {
            Button(close) {
                Text(getString("action_close"))
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.Search, null)
                Text(getString("s_cat_search"))
            }
        },
        text = {
            LazyColumn {
                items(settings_items) { item ->
                    item.Item(player.app_page_state.Settings.settings_interface, { _, _ -> }, {}, Modifier)
                }
            }
        }
    )
}
