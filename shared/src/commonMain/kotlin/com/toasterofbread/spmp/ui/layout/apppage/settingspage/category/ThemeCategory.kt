package com.toasterofbread.spmp.ui.layout.apppage.settingspage.category

import com.toasterofbread.composekit.settings.ui.Theme
import com.toasterofbread.composekit.settings.ui.ThemeData
import com.toasterofbread.composekit.settings.ui.item.SettingsItem
import com.toasterofbread.composekit.settings.ui.item.MultipleChoiceSettingsItem
import com.toasterofbread.composekit.settings.ui.item.SettingsValueState
import com.toasterofbread.composekit.settings.ui.item.ThemeSelectorSettingsItem
import com.toasterofbread.spmp.model.settings.category.AccentColourSource
import com.toasterofbread.spmp.model.settings.category.ThemeSettings
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.AppSliderItem

internal fun getThemeCategoryItems(context: AppContext): List<SettingsItem> {
    val theme: Theme = context.theme

    return listOf(
        ThemeSelectorSettingsItem(
            SettingsValueState(ThemeSettings.Key.CURRENT_THEME.getName()),
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

        MultipleChoiceSettingsItem(
            SettingsValueState(ThemeSettings.Key.ACCENT_COLOUR_SOURCE.getName()),
            getString("s_key_accent_source"), null,
            AccentColourSource.values().size, false
        ) { choice ->
            when (AccentColourSource.values()[choice]) {
                AccentColourSource.THEME -> getString("s_option_accent_theme")
                AccentColourSource.THUMBNAIL -> getString("s_option_accent_thumbnail")
            }
        },

        MultipleChoiceSettingsItem(
            SettingsValueState(ThemeSettings.Key.NOWPLAYING_THEME_MODE.getName()),
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
            SettingsValueState(ThemeSettings.Key.NOWPLAYING_DEFAULT_GRADIENT_DEPTH.getName()),
            getString("s_key_np_default_gradient_depth"), null
        )
    )
}
