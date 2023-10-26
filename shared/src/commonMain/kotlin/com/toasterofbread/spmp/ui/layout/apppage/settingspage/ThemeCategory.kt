package com.toasterofbread.spmp.ui.layout.apppage.settingspage

import com.toasterofbread.toastercomposetools.settings.ui.Theme
import com.toasterofbread.toastercomposetools.settings.ui.ThemeData
import com.toasterofbread.toastercomposetools.settings.ui.item.SettingsItem
import com.toasterofbread.toastercomposetools.settings.ui.item.SettingsMultipleChoiceItem
import com.toasterofbread.toastercomposetools.settings.ui.item.SettingsSliderItem
import com.toasterofbread.toastercomposetools.settings.ui.item.SettingsValueState
import com.toasterofbread.settings.ui.item.SettingsItemThemeSelector
import com.toasterofbread.spmp.model.AccentColourSource
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.resources.getString

internal fun getThemeCategory(theme: Theme): List<SettingsItem> {
    return listOf(
        SettingsItemThemeSelector(
            SettingsValueState(Settings.KEY_CURRENT_THEME.name),
            getString("s_key_current_theme"), null,
            getString("s_theme_editor_title"),
            { theme.getThemeCount() },
            { theme.getThemes()[it] },
            { index: Int, edited_theme: ThemeData ->
                theme.updateTheme(index, edited_theme)
            },
            { theme.addTheme(theme.getCurrentTheme().toStaticThemeData(getString("theme_title_new")), it) },
            { theme.removeTheme(it) }
        ),

        SettingsMultipleChoiceItem(
            SettingsValueState(Settings.KEY_ACCENT_COLOUR_SOURCE.name),
            getString("s_key_accent_source"), null,
            AccentColourSource.values().size, false
        ) { choice ->
            when (AccentColourSource.values()[choice]) {
                AccentColourSource.THEME -> getString("s_option_accent_theme")
                AccentColourSource.THUMBNAIL -> getString("s_option_accent_thumbnail")
            }
        },

        SettingsMultipleChoiceItem(
            SettingsValueState(Settings.KEY_NOWPLAYING_THEME_MODE.name),
            getString("s_key_np_theme_mode"), null,
            3, false
        ) { choice ->
            when (choice) {
                0 -> getString("s_option_np_accent_background")
                1 -> getString("s_option_np_accent_elements")
                else -> getString("s_option_np_accent_none")
            }
        },

        AppSliderItem(
            SettingsValueState(Settings.KEY_NOWPLAYING_DEFAULT_GRADIENT_DEPTH.name),
            getString("s_key_np_default_gradient_depth"), null
        )
    )
}
