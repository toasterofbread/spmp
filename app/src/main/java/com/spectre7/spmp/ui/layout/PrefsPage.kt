package com.spectre7.spmp.ui.layout

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.material3.Text
import com.spectre7.spmp.ui.layout.OverlayPage
import com.spectre7.spmp.R
import com.spectre7.utils.getString
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.lazy.LazyColumn
import com.spectre7.composesettings.ui.*
import com.spectre7.composesettings.model.*
import androidx.activity.compose.BackHandler
import androidx.compose.ui.graphics.Color

@Composable
fun PrefsPage(set_overlay_page: (page: OverlayPage) -> Unit) {

    val main_page = remember { SettingsPage("Preferences", listOf(
        SettingsGroup("Theming")
    )) }

    val interface_state = object : SettingsInterfaceState() {
        override fun getThemeColour(): Color {
            return Color.Red
        }
    }

    interface_state.current_page = main_page

    SettingsInterface(interface_state, Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))

    BackHandler {
        set_overlay_page(OverlayPage.NONE)
    }

}
