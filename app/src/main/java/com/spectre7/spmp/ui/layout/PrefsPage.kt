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

            Page.ROOT -> SettingsPage(getString(R.string.s_page_preferences), listOf(

                SettingsGroup("General"),

                SettingsItemDropdown(
                    SettingsValueState("ja", "localisation_code", MainActivity.prefs),
                    "Data language", "Language used for song and artist titles, etc. (if available)",
                    listOf("ja", "en")
                ),

                SettingsGroup(getString(R.string.s_group_theming)),

                SettingsItemMultipleChoice(
                    SettingsValueState(0, "accent_colour_source", MainActivity.prefs),
                    getString(R.string.s_key_accent_source), null,
                    2, false
                ) { choice ->
                    when (choice) {
                        0 -> {
                            getString(R.string.s_option_accent_thumbnail)
                        }
                        else ->  {
                            getString(R.string.s_option_accent_system)
                        }
                    }
                },

                SettingsItemMultipleChoice(
                    SettingsValueState(0, "np_theme_mode", MainActivity.prefs),
                    getString(R.string.s_key_np_theme_mode), null,
                    3, false
                ) { choice ->
                  when (choice) {
                      0 -> {
                          getString(R.string.s_option_np_accent_background)
                      }
                      1 -> {
                          getString(R.string.s_option_np_accent_elements)
                      }
                      else -> {
                          getString(R.string.s_option_np_accent_none)
                      }
                  }
                },

                SettingsGroup(getString(R.string.s_group_lyrics)),

                SettingsItemToggle(
                    SettingsValueState(true, "lyrics_follow_enabled", MainActivity.prefs),
                    "Follow current line", "When displaying timed lyrics, scroll to the current line automatically"
                ),

                SettingsItemSlider(
                    SettingsValueState(0.5f, "lyrics_follow_offset", MainActivity.prefs),
                    "Followed line position", "When scrolling to the current line, position within the view to place the line",
                    "Top", "Bottom", steps = 5
                ),

                SettingsItemToggle(
                    SettingsValueState(true, "lyrics_default_furigana", MainActivity.prefs),
                    "Show furigana by default", null
                )


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
            .pointerInput(Unit) {}) {
        settings_interface.Interface(Modifier.requiredHeight(LocalConfiguration.current.screenHeightDp.dp - MINIMISED_NOW_PLAYING_HEIGHT.dp))
    }
}
