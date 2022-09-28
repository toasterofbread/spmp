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
import androidx.compose.ui.input.pointer.pointerInput

enum class Page { ROOT }

@Composable
fun PrefsPage(set_overlay_page: (page: OverlayPage) -> Unit) {

    val slider_state = remember { SettingsValueState(0.5f, "slider", MainActivity.prefs) }

    Box(Modifier.pointerInput(Unit) {}) {
        SettingsInterface(MainActivity.getTheme(), Page.ROOT, {
            when (it) {
                Page.ROOT -> SettingsPage("Preferences", listOf(
                    SettingsGroup("Theming"),
                    SettingsValueToggle(SettingsValueState(false, "toggle", MainActivity.prefs), "Hello World", "Subtitle text text subtitle text"),
                    SettingsValueSlider(slider_state, "Hello World - ${slider_state.value}", "Subtitle text text subtitle text")
                ), Modifier.fillMaxSize().background(MainActivity.getTheme().getBackground(false)))
            }
        }).Interface()
    }

    BackHandler {
        set_overlay_page(OverlayPage.NONE)
    }
}
