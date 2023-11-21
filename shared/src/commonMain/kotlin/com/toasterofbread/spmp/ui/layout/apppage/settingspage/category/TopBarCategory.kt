package com.toasterofbread.spmp.ui.layout.apppage.settingspage.category

import com.toasterofbread.composekit.settings.ui.item.SettingsItem
import com.toasterofbread.composekit.settings.ui.item.SettingsToggleItem
import com.toasterofbread.composekit.settings.ui.item.SettingsValueState
import com.toasterofbread.spmp.model.settings.category.TopBarSettings
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.AppSliderItem
import kotlin.math.roundToInt

internal fun getTopBarCategoryItems(): List<SettingsItem> {
    return listOf(
        SettingsToggleItem(
            SettingsValueState(TopBarSettings.Key.LYRICS_LINGER.getName()),
            getString("s_key_topbar_lyrics_linger"), getString("s_sub_topbar_lyrics_linger")
        ),

        AppSliderItem(
            SettingsValueState<Float>(TopBarSettings.Key.VISUALISER_WIDTH.getName()),
            getString("s_key_topbar_visualiser_width"), null,
            getValueText = { value ->
                "${(value.toFloat() * 100f).roundToInt()}%"
            }
        ),

        SettingsToggleItem(
            SettingsValueState(TopBarSettings.Key.SHOW_LYRICS_IN_QUEUE.getName()),
            getString("s_key_topbar_show_lyrics_in_queue"), null
        ),
        SettingsToggleItem(
            SettingsValueState(TopBarSettings.Key.SHOW_VISUALISER_IN_QUEUE.getName()),
            getString("s_key_topbar_show_visualiser_in_queue"), null
        ),

        SettingsToggleItem(
            SettingsValueState(TopBarSettings.Key.DISPLAY_OVER_ARTIST_IMAGE.getName()),
            getString("s_key_topbar_display_over_artist_image"), null
        ),

        SettingsToggleItem(
            SettingsValueState(TopBarSettings.Key.LYRICS_ENABLE.getName()),
            getString("s_key_lyrics_top_bar_enable"), null
        ),

        AppSliderItem(
            SettingsValueState<Int>(TopBarSettings.Key.LYRICS_MAX_LINES.getName()),
            getString("s_key_lyrics_top_bar_max_lines"), null,
            range = 1f .. 10f
        ),

        SettingsToggleItem(
            SettingsValueState(TopBarSettings.Key.LYRICS_PREAPPLY_MAX_LINES.getName()),
            getString("s_key_lyrics_top_bar_preapply_max_lines"), null
        ),

        SettingsToggleItem(
            SettingsValueState(TopBarSettings.Key.LYRICS_SHOW_FURIGANA.getName()),
            getString("s_key_top_bar_lyrics_show_furigana"), null
        ),

        SettingsToggleItem(
            SettingsValueState(TopBarSettings.Key.SHOW_IN_LIBRARY.getName()),
            getString("s_key_top_bar_lyrics_show_in_library"), null
        ),
        SettingsToggleItem(
            SettingsValueState(TopBarSettings.Key.SHOW_IN_RADIOBUILDER.getName()),
            getString("s_key_top_bar_lyrics_show_in_radiobuilder"), null
        ),
        SettingsToggleItem(
            SettingsValueState(TopBarSettings.Key.SHOW_IN_SETTINGS.getName()),
            getString("s_key_top_bar_lyrics_show_in_settings"), null
        ),
        SettingsToggleItem(
            SettingsValueState(TopBarSettings.Key.SHOW_IN_LOGIN.getName()),
            getString("s_key_top_bar_lyrics_show_in_login"), null
        ),
        SettingsToggleItem(
            SettingsValueState(TopBarSettings.Key.SHOW_IN_PLAYLIST.getName()),
            getString("s_key_top_bar_lyrics_show_in_playlist"), null
        ),
        SettingsToggleItem(
            SettingsValueState(TopBarSettings.Key.SHOW_IN_ARTIST.getName()),
            getString("s_key_top_bar_lyrics_show_in_artist"), null
        ),
        SettingsToggleItem(
            SettingsValueState(TopBarSettings.Key.SHOW_IN_VIEWMORE.getName()),
            getString("s_key_top_bar_lyrics_show_in_viewmore"), null
        ),
        SettingsToggleItem(
            SettingsValueState(TopBarSettings.Key.SHOW_IN_SEARCH.getName()),
            getString("s_key_top_bar_lyrics_show_in_search"), null
        )
    )
}
