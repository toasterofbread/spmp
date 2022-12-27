package com.spectre7.spmp.ui.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.spectre7.composesettings.model.*
import com.spectre7.composesettings.ui.SettingsInterface
import com.spectre7.composesettings.ui.SettingsPage
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.R
import com.spectre7.spmp.model.Settings
import com.spectre7.utils.OnChangedEffect
import com.spectre7.utils.getString
import java.util.*

enum class Page { ROOT, OTHER }

@Composable
fun PrefsPage(setOverlayPage: (page: OverlayPage) -> Unit) {

    val interface_lang = remember {
        SettingsValueState<Int>(
            Settings.KEY_LANG_UI.name,
            Settings.prefs,
            Settings.getDefaultProvider()
        )
    }
    var language_data by remember { mutableStateOf(MainActivity.languages.values.elementAt(interface_lang.value)) }

    OnChangedEffect(interface_lang.value) {
        language_data = MainActivity.languages.values.elementAt(interface_lang.value)
    }

    val settings_interface: SettingsInterface = remember { SettingsInterface(MainActivity.theme, Page.ROOT.ordinal, {
        when (Page.values()[it]) {

            Page.ROOT -> SettingsPage(getString(R.string.s_page_preferences), listOf(

                SettingsGroup(getString(R.string.s_group_general)),

                SettingsItemDropdown(
                    interface_lang,
                    getString(R.string.s_key_interface_lang), getString(R.string.s_sub_interface_lang),
                    MainActivity.languages.values.first().size,
                    { i ->
                        language_data.entries.elementAt(i).key
                    }
                ) { i ->
                    val language = language_data.entries.elementAt(i)
                    "${language.key} / ${language.value}"
                },

                SettingsItemDropdown(
                    SettingsValueState(Settings.KEY_LANG_DATA.name, Settings.prefs, Settings.getDefaultProvider()),
                    getString(R.string.s_key_data_lang), getString(R.string.s_sub_data_lang),
                    MainActivity.languages.values.first().size,
                    { i ->
                        language_data.entries.elementAt(i).key
                    }
                ) { i ->
                    val language = language_data.entries.elementAt(i)
                    "${language.key} / ${language.value}"
                },

                SettingsGroup(getString(R.string.s_group_theming)),

                SettingsItemMultipleChoice(
                    SettingsValueState(Settings.KEY_ACCENT_COLOUR_SOURCE.name, Settings.prefs, Settings.getDefaultProvider()),
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
                    SettingsValueState(Settings.KEY_NOWPLAYING_THEME_MODE.name, Settings.prefs, Settings.getDefaultProvider()),
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
                    SettingsValueState(Settings.KEY_LYRICS_FOLLOW_ENABLED.name, Settings.prefs, Settings.getDefaultProvider()),
                    "Follow current line", "When displaying timed lyrics, scroll to the current line automatically"
                ),

                SettingsItemSlider(
                    SettingsValueState(Settings.KEY_LYRICS_FOLLOW_OFFSET.name, Settings.prefs, Settings.getDefaultProvider()),
                    "Followed line position", "When scrolling to the current line, position within the view to place the line",
                    "Top", "Bottom", steps = 5
                ),

                SettingsItemToggle(
                    SettingsValueState(Settings.KEY_LYRICS_DEFAULT_FURIGANA.name, Settings.prefs, Settings.getDefaultProvider()),
                    "Show furigana by default", null
                ),

                SettingsItemDropdown(
                    SettingsValueState(Settings.KEY_LYRICS_TEXT_ALIGNMENT.name, Settings.prefs, Settings.getDefaultProvider()),
                    "Text alignment", null, 3
                ) { i ->
                    when (i) {
                        0 -> "Left"
                        1 -> "Center"
                        else -> "Right"
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
            .pointerInput(Unit) {}) {
        settings_interface.Interface(Modifier.requiredHeight(LocalConfiguration.current.screenHeightDp.dp - MINIMISED_NOW_PLAYING_HEIGHT.dp))
    }
}
