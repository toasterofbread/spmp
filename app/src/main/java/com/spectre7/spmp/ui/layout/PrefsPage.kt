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
import androidx.compose.ui.platform.LocalConfiguration
import com.spectre7.spmp.PlayerHost

enum class Page { ROOT, OTHER }

@Composable
fun PrefsPage(setOverlayPage: (page: OverlayPage) -> Unit) {

    val settings_interface: SettingsInterface = remember { SettingsInterface(MainActivity.theme, Page.ROOT.ordinal, {
        when (Page.values()[it]) {

            Page.ROOT -> SettingsPage("Preferences", listOf(

                SettingsGroup("Theming"),

                SettingsItemMultipleChoice(
                    SettingsValueState(0, "accent_colour_source", MainActivity.prefs),
                    "Accent colour", null,
                    2, false
                ) { choice ->
                    when (choice) {
                        0 -> {
                            "From thumbnail"
                        }
                        else ->  {
                            "From system theme"
                        }
                    }
                },

                SettingsItemMultipleChoice(
                    SettingsValueState(0, "np_theme_mode", MainActivity.prefs),
                    "Now playing theme mode", null,
                    3, false
                ) { choice ->
                  when (choice) {
                      0 -> {
                          "Colour background with accent"
                      }
                      1 -> {
                          "Colour elements with accent"
                      }
                      else -> {
                          "Don't use accent"
                      }
                  }
                },

            ), Modifier.fillMaxSize())
            else -> {
                null
            }
        }
    }, {
        setOverlayPage(OverlayPage.NONE)
    }) }

    BoxWithConstraints(
        Modifier
            .background(
                MainActivity
                    .theme
                    .getBackground(false)
            )
            .padding(20.dp)
            .pointerInput(Unit) {}) {
        LazyColumn(Modifier.fillMaxHeight()) {
            item {
                settings_interface.Interface(Modifier.requiredHeight(LocalConfiguration.current.screenHeightDp.dp))
            }
        }
    }

    PlayerHost.interact {
        it.volume = it.volume * 0.95f
    }
}
