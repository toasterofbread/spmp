package com.toasterofbread.spmp.ui.layout.apppage.settingspage.category

import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.doesPlatformSupportVideoPlayback
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.AppSliderItem
import com.toasterofbread.spmp.ui.layout.nowplaying.ThemeMode
import dev.toastbits.composekit.settingsitem.presentation.ui.component.item.GroupSettingsItem
import dev.toastbits.composekit.settingsitem.presentation.ui.component.item.MultipleChoiceSettingsItem
import dev.toastbits.composekit.settingsitem.domain.SettingsItem
import dev.toastbits.composekit.settingsitem.presentation.ui.component.item.ToggleSettingsItem
import dev.toastbits.composekit.util.platform.Platform
import isWindowTransparencySupported
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.s_group_theming_desktop
import spmp.shared.generated.resources.s_option_np_accent_background
import spmp.shared.generated.resources.s_option_np_accent_elements
import spmp.shared.generated.resources.s_option_np_accent_none

internal fun getThemeCategoryItems(context: AppContext): List<SettingsItem> =
    listOfNotNull(
        MultipleChoiceSettingsItem(
            context.settings.Theme.ACCENT_COLOUR_SOURCE
        ) { source ->
            stringResource(source.getNameResource())
        },

        MultipleChoiceSettingsItem(
            context.settings.Theme.NOWPLAYING_THEME_MODE
        ) { mode ->
            when (mode) {
                ThemeMode.BACKGROUND -> stringResource(Res.string.s_option_np_accent_background)
                ThemeMode.ELEMENTS -> stringResource(Res.string.s_option_np_accent_elements)
                ThemeMode.NONE -> stringResource(Res.string.s_option_np_accent_none)
            }
        },

        AppSliderItem(
            context.settings.Theme.NOWPLAYING_DEFAULT_GRADIENT_DEPTH
        ),

        AppSliderItem(
            context.settings.Theme.NOWPLAYING_DEFAULT_BACKGROUND_IMAGE_OPACITY
        ),

        if (doesPlatformSupportVideoPlayback())
            MultipleChoiceSettingsItem(
                context.settings.Theme.NOWPLAYING_DEFAULT_VIDEO_POSITION
            ) { position ->
                position.getReadable()
            }
        else null,

        AppSliderItem(
            context.settings.Theme.NOWPLAYING_DEFAULT_LANDSCAPE_QUEUE_OPACITY
        ),

        AppSliderItem(
            context.settings.Theme.NOWPLAYING_DEFAULT_SHADOW_RADIUS
        ),

        AppSliderItem(
            context.settings.Theme.NOWPLAYING_DEFAULT_IMAGE_CORNER_ROUNDING
        ),

        ToggleSettingsItem(
            context.settings.Theme.SHOW_EXPANDED_PLAYER_WAVE
        ),

        AppSliderItem(
            context.settings.Theme.NOWPLAYING_DEFAULT_WAVE_SPEED
        ),

        AppSliderItem(
            context.settings.Theme.NOWPLAYING_DEFAULT_WAVE_OPACITY
        )
    ) + when (Platform.current) {
        Platform.DESKTOP -> getDesktopGroupItems(context)
        else -> emptyList()
    }

private fun getDesktopGroupItems(context: AppContext): List<SettingsItem> =
    listOf(
        GroupSettingsItem(Res.string.s_group_theming_desktop)
    ) + (
        if (isWindowTransparencySupported()) getWindowTransparencyItems(context)
        else emptyList()
    )

private fun getWindowTransparencyItems(context: AppContext): List<SettingsItem> = listOf(
    ToggleSettingsItem(
        context.settings.Theme.ENABLE_WINDOW_TRANSPARENCY
    ),

    AppSliderItem(
        context.settings.Theme.WINDOW_BACKGROUND_OPACITY,
        range = 0f..1f
    )
)
