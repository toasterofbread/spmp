package com.toasterofbread.spmp.ui.layout.apppage.settingspage.category

import isWindowTransparencySupported
import androidx.compose.ui.Modifier
import com.toasterofbread.composekit.platform.Platform
import com.toasterofbread.composekit.settings.ui.Theme
import com.toasterofbread.composekit.settings.ui.ThemeData
import com.toasterofbread.composekit.settings.ui.item.*
import com.toasterofbread.spmp.model.settings.category.AccentColourSource
import com.toasterofbread.spmp.model.settings.category.ThemeSettings
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.isVideoPlaybackSupported
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.appTextField
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.AppSliderItem

internal fun getThemeCategoryItems(context: AppContext): List<SettingsItem> {
    val theme: Theme = context.theme

    return listOfNotNull(
        ThemeSelectorSettingsItem(
            SettingsValueState(ThemeSettings.Key.CURRENT_THEME.getName()),
            getString("s_key_current_theme"), null,
            str_editor_title = getString("s_theme_editor_title"),
            str_field_name = getString("s_theme_editor_field_name"),
            str_field_background = getString("s_theme_editor_field_background"),
            str_field_on_background = getString("s_theme_editor_field_on_background"),
            str_field_card = getString("s_theme_editor_field_card"),
            str_field_accent = getString("s_theme_editor_field_accent"),
            str_button_preview = getString("s_theme_editor_button_preview"),
            { theme.getThemeCount() },
            { theme.getThemes().getOrNull(it) },
            { index: Int, edited_theme: ThemeData ->
                theme.updateTheme(index, edited_theme)
            },
            { theme.addTheme(theme.getCurrentTheme().toStaticThemeData(getString("theme_title_new")), it) },
            { theme.removeTheme(it) },
            getFieldModifier = { Modifier.appTextField() }
        ),

        MultipleChoiceSettingsItem(
            SettingsValueState(ThemeSettings.Key.ACCENT_COLOUR_SOURCE.getName()),
            getString("s_key_accent_source"), null,
            AccentColourSource.entries.size
        ) { choice ->
            when (AccentColourSource.entries[choice]) {
                AccentColourSource.THEME -> getString("s_option_accent_theme")
                AccentColourSource.THUMBNAIL -> getString("s_option_accent_thumbnail")
            }
        },

        MultipleChoiceSettingsItem(
            SettingsValueState(ThemeSettings.Key.NOWPLAYING_THEME_MODE.getName()),
            getString("s_key_np_theme_mode"), null,
            3
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
        ),

        AppSliderItem(
            SettingsValueState(ThemeSettings.Key.NOWPLAYING_DEFAULT_BACKGROUND_IMAGE_OPACITY.getName()),
            getString("s_key_np_default_background_image_video_opacity"), null
        ),

        if (isVideoPlaybackSupported())
            MultipleChoiceSettingsItem(
                SettingsValueState(ThemeSettings.Key.NOWPLAYING_DEFAULT_VIDEO_POSITION.getName()),
                getString("s_key_np_default_video_position"), null,
                ThemeSettings.VideoPosition.entries.size
            ) {
                ThemeSettings.VideoPosition.entries[it].getReadable()
            }
        else null,

        AppSliderItem(
            SettingsValueState(ThemeSettings.Key.NOWPLAYING_DEFAULT_LANDSCAPE_QUEUE_OPACITY.getName()),
            getString("s_key_np_default_landscape_queue_opacity"), null
        ),

        AppSliderItem(
            SettingsValueState(ThemeSettings.Key.NOWPLAYING_DEFAULT_SHADOW_RADIUS.getName()),
            getString("s_key_np_default_shadow_radius"), null
        ),

        AppSliderItem(
            SettingsValueState(ThemeSettings.Key.NOWPLAYING_DEFAULT_IMAGE_CORNER_ROUNDING.getName()),
            getString("s_key_np_default_image_corner_rounding"), null
        ),

        ToggleSettingsItem(
            SettingsValueState(ThemeSettings.Key.SHOW_EXPANDED_PLAYER_WAVE.getName()),
            getString("s_key_show_expanded_player_wave"), null
        ),

        AppSliderItem(
            SettingsValueState(ThemeSettings.Key.NOWPLAYING_DEFAULT_WAVE_SPEED.getName()),
            getString("s_key_np_default_wave_speed"), null
        ),

        AppSliderItem(
            SettingsValueState(ThemeSettings.Key.NOWPLAYING_DEFAULT_WAVE_OPACITY.getName()),
            getString("s_key_np_default_wave_opacity"), null
        )
    ) + when (Platform.current) {
        Platform.DESKTOP -> getDesktopGroupItems()
        else -> emptyList()
    }
}

private fun getDesktopGroupItems(): List<SettingsItem> =
    listOf(
        GroupSettingsItem(
            getString("s_group_theming_desktop")
        )
    ) + (
        if (isWindowTransparencySupported()) getWindowTransparencyItems()
        else emptyList()
    )

private fun getWindowTransparencyItems(): List<SettingsItem> = listOf(
    ToggleSettingsItem(
        SettingsValueState(ThemeSettings.Key.ENABLE_WINDOW_TRANSPARENCY.getName()),
        getString("s_key_enable_window_transparency"),
        getString("s_sub_enable_window_transparency")
    ),

    AppSliderItem(
        SettingsValueState(ThemeSettings.Key.WINDOW_BACKGROUND_OPACITY.getName()),
        getString("s_key_window_background_opacity"),
        getString("s_sub_window_background_opacity"),
        range = 0f..1f
    )
)
