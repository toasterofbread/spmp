package com.toasterofbread.spmp.ui.layout.apppage.settingspage.category

import isWindowTransparencySupported
import androidx.compose.ui.Modifier
import dev.toastbits.composekit.platform.Platform
import dev.toastbits.composekit.settings.ui.Theme
import dev.toastbits.composekit.settings.ui.ThemeData
import dev.toastbits.composekit.settings.ui.item.*
import com.toasterofbread.spmp.model.settings.category.AccentColourSource
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.doesPlatformSupportVideoPlayback
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.appTextField
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.AppSliderItem
import com.toasterofbread.spmp.ui.layout.nowplaying.ThemeMode

internal fun getThemeCategoryItems(context: AppContext): List<SettingsItem> {
    val theme: Theme = context.theme

    return listOfNotNull(
        ThemeSelectorSettingsItem(
            context.settings.theme.CURRENT_THEME,
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
            context.settings.theme.ACCENT_COLOUR_SOURCE
        ) { source ->
            when (source) {
                AccentColourSource.THEME -> getString("s_option_accent_theme")
                AccentColourSource.THUMBNAIL -> getString("s_option_accent_thumbnail")
            }
        },

        MultipleChoiceSettingsItem(
            context.settings.theme.NOWPLAYING_THEME_MODE
        ) { mode ->
            when (mode) {
                ThemeMode.BACKGROUND -> getString("s_option_np_accent_background")
                ThemeMode.ELEMENTS -> getString("s_option_np_accent_elements")
                ThemeMode.NONE -> getString("s_option_np_accent_none")
            }
        },

        AppSliderItem(
            context.settings.theme.NOWPLAYING_DEFAULT_GRADIENT_DEPTH
        ),

        AppSliderItem(
            context.settings.theme.NOWPLAYING_DEFAULT_BACKGROUND_IMAGE_OPACITY
        ),

        if (doesPlatformSupportVideoPlayback())
            MultipleChoiceSettingsItem(
                context.settings.theme.NOWPLAYING_DEFAULT_VIDEO_POSITION
            ) { position ->
                position.getReadable()
            }
        else null,

        AppSliderItem(
            context.settings.theme.NOWPLAYING_DEFAULT_LANDSCAPE_QUEUE_OPACITY
        ),

        AppSliderItem(
            context.settings.theme.NOWPLAYING_DEFAULT_SHADOW_RADIUS
        ),

        AppSliderItem(
            context.settings.theme.NOWPLAYING_DEFAULT_IMAGE_CORNER_ROUNDING
        ),

        ToggleSettingsItem(
            context.settings.theme.SHOW_EXPANDED_PLAYER_WAVE
        ),

        AppSliderItem(
            context.settings.theme.NOWPLAYING_DEFAULT_WAVE_SPEED
        ),

        AppSliderItem(
            context.settings.theme.NOWPLAYING_DEFAULT_WAVE_OPACITY
        )
    ) + when (Platform.current) {
        Platform.DESKTOP -> getDesktopGroupItems(context)
        else -> emptyList()
    }
}

private fun getDesktopGroupItems(context: AppContext): List<SettingsItem> =
    listOf(
        GroupSettingsItem(
            getString("s_group_theming_desktop")
        )
    ) + (
        if (isWindowTransparencySupported()) getWindowTransparencyItems(context)
        else emptyList()
    )

private fun getWindowTransparencyItems(context: AppContext): List<SettingsItem> = listOf(
    ToggleSettingsItem(
        context.settings.theme.ENABLE_WINDOW_TRANSPARENCY
    ),

    AppSliderItem(
        context.settings.theme.WINDOW_BACKGROUND_OPACITY,
        range = 0f..1f
    )
)
