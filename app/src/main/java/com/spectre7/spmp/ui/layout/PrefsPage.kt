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
import com.spectre7.spmp.MainActivity
import com.spectre7.utils.Theme
import androidx.compose.ui.zIndex

@Composable
fun PrefsPage(set_overlay_page: (page: OverlayPage) -> Unit) {

    val main_page = remember { SettingsPage("Preferences", listOf(
        SettingsGroup("Theming"),
        SettingsValueToggle(SettinsValueState(false), "Hello World", "Subtitle text text subtitle text")
    ))}

    val interface_state = object : SettingsInterfaceState() {
        override fun getTheme(): Theme {
            return MainActivity.getTheme()
        }
    }

    interface_state.current_page = main_page

    SettingsInterface(interface_state, Modifier.fillMaxSize().background(Color.Red).zIndex(100f))
    // SettingsInterface(interface_state, Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).zIndex(100f))

    BackHandler {
        set_overlay_page(OverlayPage.NONE)
    }

}
