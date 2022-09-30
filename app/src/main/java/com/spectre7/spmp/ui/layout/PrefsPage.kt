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
import androidx.compose.ui.unit.*
import androidx.compose.ui.graphics.Color
import com.spectre7.spmp.MainActivity
import com.spectre7.utils.Theme
import androidx.compose.ui.zIndex
import androidx.compose.ui.input.pointer.pointerInput
import com.spectre7.spmp.PlayerHost

enum class Page { ROOT, OTHER }

@Composable
fun PrefsPage(setOverlayPage: (page: OverlayPage) -> Unit) {

    val slider_state = remember { SettingsValueState(0.5f, "slider", MainActivity.prefs) }
    val settings_interface: SettingsInterface = remember { SettingsInterface(MainActivity.getTheme(), Page.ROOT.ordinal, {
        when (Page.values()[it]) {
            Page.ROOT -> SettingsPage("Preferences", listOf(
                SettingsGroup("Theming"),
                SettingsItemToggle(SettingsValueState(false, "toggle", MainActivity.prefs), "Hello World", "Subtitle text text subtitle text"),
                SettingsItemSubpage("Other page", "Subtitle!", Page.OTHER.ordinal),
                SettingsItemSlider(slider_state, "Hello World - ${slider_state.value}", "Subtitle text text subtitle text"),
            ), Modifier.fillMaxSize())
            Page.OTHER -> SettingsPage("Other Preferences", listOf(
                SettingsGroup("Theming"),
                SettingsItemToggle(SettingsValueState(false, "toggle", MainActivity.prefs), "Hello World", "Subtitle text text subtitle text"),
                SettingsItemSlider(slider_state, "Hello World - ${slider_state.value}", "Subtitle text text subtitle text"),
                SettingsItemSubpage("Root page", "Other subtitle!", Page.ROOT.ordinal)
            ), Modifier.fillMaxSize())
        }
    }, {
        setOverlayPage(OverlayPage.NONE)
    }) }

    Box(Modifier.background(MainActivity.getTheme().getBackground(false)).padding(20.dp).pointerInput(Unit) {}) {
        settings_interface.Interface()
    }

    PlayerHost.interact {
        it.setVolume(it.getVolume() * 0.95f)
    }
}
