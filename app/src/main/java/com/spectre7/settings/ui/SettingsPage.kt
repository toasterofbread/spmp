package com.spectre7.composesettings.ui

import com.spectre7.composesettings.model.*
import com.spectre7.utils.Theme
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.unit.dp

class SettingsPage(val title: String, val items: List<SettingsItem>) {
    internal var interface_state: SettingsInterfaceState? = null

    @Composable
    fun GetPage(modifier: Modifier = Modifier) {
        Column(modifier) {
            Text(title, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.requiredHeight(50.dp))
            for (item in items) {
                item.GetItem(interface_state?.getTheme() ?: Theme.default())
            }
        }
    }
}
