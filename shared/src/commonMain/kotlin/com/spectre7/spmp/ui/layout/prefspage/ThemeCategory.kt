package com.spectre7.spmp.ui.layout.prefspage

import com.spectre7.settings.model.SettingsItem
import com.spectre7.settings.model.SettingsItemMultipleChoice
import com.spectre7.settings.model.SettingsValueState
import com.spectre7.settings.ui.SettingsItemThemeSelector
import com.spectre7.spmp.model.AccentColourSource
import com.spectre7.spmp.model.Settings
import com.spectre7.spmp.resources.getString
import com.spectre7.spmp.ui.theme.Theme
import com.spectre7.spmp.ui.theme.ThemeData
import com.spectre7.spmp.ui.theme.ThemeManager

internal fun getThemeCategory(theme_manager: ThemeManager): List<SettingsItem> {
    return listOf(
        SettingsItemThemeSelector(
            SettingsValueState(Settings.KEY_CURRENT_THEME.name),
            getString("s_key_current_theme"), null,
            getString("s_theme_editor_title"),
            {
                check(theme_manager.themes.isNotEmpty())
                theme_manager.themes.size
            },
            { theme_manager.themes[it] },
            { index: Int, edited_theme: ThemeData ->
                theme_manager.updateTheme(index, edited_theme)
            },
            { theme_manager.addTheme(Theme.current.theme_data.copy(name = getString("theme_title_new")), it) },
            { theme_manager.removeTheme(it) }
        ),

        SettingsItemMultipleChoice(
            SettingsValueState(Settings.KEY_ACCENT_COLOUR_SOURCE.name),
            getString("s_key_accent_source"), null,
            3, false
        ) { choice ->
            when (AccentColourSource.values()[choice]) {
                AccentColourSource.THEME -> getString("s_option_accent_theme")
                AccentColourSource.THUMBNAIL -> getString("s_option_accent_thumbnail")
                AccentColourSource.SYSTEM -> getString("s_option_accent_system")
            }
        },

        SettingsItemMultipleChoice(
            SettingsValueState(Settings.KEY_NOWPLAYING_THEME_MODE.name),
            getString("s_key_np_theme_mode"), null,
            3, false
        ) { choice ->
            when (choice) {
                0 -> getString("s_option_np_accent_background")
                1 -> getString("s_option_np_accent_elements")
                else -> getString("s_option_np_accent_none")
            }
        }
    )
}
