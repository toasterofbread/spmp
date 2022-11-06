package com.spectre7.composesettings.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spectre7.composesettings.model.SettingsItem

class SettingsPage(val title: String, val items: List<SettingsItem>, val modifier: Modifier = Modifier) {
    @Composable
    fun Page(settings_interface: SettingsInterface, openPage: (Int) -> Unit, goBack: () -> Unit) {
        // Page items
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            for (item in items) {
                item.GetItem(settings_interface.theme, openPage)
            }
        }

        BackHandler {
            goBack()
        }
    }

    @Composable
    fun TitleBar(settings_interface: SettingsInterface, is_root: Boolean, goBack: () -> Unit) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, fontSize = 30.sp, fontWeight = FontWeight.Bold)
//            IconButton(goBack) {
//                Icon(if (is_root) Icons.Filled.Close else Icons.Filled.ArrowBack, "", Modifier.size(30.dp), tint = settings_interface.theme.getOnBackground(false))
//            }
        }
    }

}
